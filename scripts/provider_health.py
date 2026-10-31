#!/usr/bin/env python3
"""Read-only OpenTV provider checks. Never modify providers, DNS, config, or public sites."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import ssl
import sys
import time
from datetime import datetime, timezone
from html.parser import HTMLParser
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import Request, HTTPRedirectHandler, build_opener, HTTPSHandler

# Keep in sync with verified built-in provider roots. A change here requires review.
DEFAULTS = {
    "fullhdfilmizlesene": ("https://www.fullhdfilmizlesene.now", "/"),
    "filmmakinesi": ("https://filmmakinesi.co", "/"),
    "hdfilmcehennemi": ("https://www.hdfilmcehennemi.nl", "/"),
    "sinemacx": ("https://sinemacc.com", "/"),
    "jetfilmizle": ("https://jetfilmizle.app", "/"),
    "dizipal": (None, None),  # Domain discovery in Android: no trustworthy fixed host.
    "ddizibox": ("https://ddizibox.com", "/diziler"),
    "dramadizilerim": ("https://dramadizilerim.com", "/"),
    "dizimom": ("https://ddizimom.net", "/diziler"),
}
MAX_BYTES = 256 * 1024


class LinkCount(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.links = 0

    def handle_starttag(self, tag, attrs):
        if tag == "a" and dict(attrs).get("href"):
            self.links += 1


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def validate_endpoint(endpoint: str) -> str:
    uri = urlparse(endpoint)
    if uri.scheme != "https" or not uri.hostname or uri.username or uri.password or uri.port:
        raise ValueError("Invalid HTTPS endpoint")
    if uri.path not in ("", "/") or uri.query or uri.fragment:
        raise ValueError("Domain override must have no path, query, or fragment")
    host = uri.hostname.lower()
    if host in {"localhost"} or host.endswith((".localhost", ".local", ".internal")):
        raise ValueError("Local hostname prohibited")
    if "." not in host or all(ch in "0123456789." for ch in host):
        raise ValueError("Public hostname required")
    return endpoint.rstrip("/")


def read_config(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    if set(data) != {"schema", "version", "providers"} or data["schema"] != 1:
        raise ValueError("Unsupported provider schema")
    if type(data["version"]) is not int or data["version"] < 1:
        raise ValueError("Invalid manifest version")
    if not isinstance(data["providers"], dict) or data["providers"].keys() - DEFAULTS.keys():
        raise ValueError("Unknown provider ID")
    return data


def fetch(url: str, timeout: int = 12) -> tuple[int, bytes, str | None]:
    opener = build_opener(NoRedirect(), HTTPSHandler(context=ssl.create_default_context()))
    request = Request(url, headers={
        "User-Agent": "OpenTV-ProviderHealth/1.0 (+read-only; GitHub Actions)",
        "Accept": "text/html,application/xhtml+xml",
    })
    try:
        with opener.open(request, timeout=timeout) as response:
            body = response.read(MAX_BYTES + 1)
            if len(body) > MAX_BYTES:
                body = body[:MAX_BYTES]
            return response.status, body, response.headers.get("Location")
    except HTTPError as error:
        return error.code, error.read(4096), error.headers.get("Location")


def check_one(provider_id: str, domain: str | None, path: str | None) -> dict:
    record = {
        "provider": provider_id, "url": None, "status": "not_checked",
        "http": None, "links": None, "elapsed_ms": None, "note": "",
    }
    if not domain:
        record["note"] = "Dynamic domain; cannot infer active provider URL from built-in default"
        return record
    try:
        base = validate_endpoint(domain)
        if not path or not path.startswith("/") or path.startswith("//"):
            raise ValueError("Invalid probe path")
        url = base + path
        record["url"] = url
        started = time.monotonic()
        code, content, redirect = fetch(url)
        record["elapsed_ms"] = int((time.monotonic() - started) * 1000)
        record["http"] = code
        if code in (401, 403, 429, 503):
            record["status"] = "blocked_or_unavailable"
            record["note"] = "WAF/rate limit/upstream outage possible; not proof of provider failure"
        elif code in (301, 302, 307, 308):
            record["status"] = "redirect"
            record["note"] = "Location: " + (redirect or "(missing)")[:180]
        elif code != 200:
            record["status"] = "http_error"
            record["note"] = "Non-200 response"
        else:
            parser = LinkCount()
            parser.feed(content.decode("utf-8", errors="replace"))
            record["links"] = parser.links
            if parser.links >= 3:
                record["status"] = "reachable"
                record["note"] = "HTML has links; playback/pagination not verified"
            else:
                record["status"] = "needs_review"
                record["note"] = "HTTP 200 but few links; possible challenge or layout change"
    except (OSError, URLError, ValueError, ssl.SSLError, Exception) as error:
        # A single provider failure must not abort checks of other providers.
        record["status"] = "connection_error"
        record["note"] = (type(error).__name__ + ": " + str(error))[:220]
    return record


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=Path("config/providers.json"))
    parser.add_argument("--output", type=Path, default=Path("provider-health.json"))
    parser.add_argument("--dry-run", action="store_true", help="Parse and validate without HTTP")
    args = parser.parse_args()
    config = read_config(args.config)
    records = []
    for provider_id, (fallback, path) in DEFAULTS.items():
        override = config["providers"].get(provider_id, {})
        domain = override.get("domain", fallback)
        if domain:
            validate_endpoint(domain)
        if args.dry_run:
            records.append({"provider": provider_id, "status": "dry_run", "url": domain})
        else:
            records.append(check_one(provider_id, domain, path or "/"))
    result = {
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "config_version": config["version"],
        "results": records,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    counts = {}
    for row in records:
        counts[row["status"]] = counts.get(row["status"], 0) + 1
    summary = [
        "## OpenTV provider health (read-only)",
        "",
        f"Manifest: v{config['version']}. Checks: {len(records)}. Status counts: {counts}.",
        "",
        "| Provider | Status | HTTP | Links | Time (ms) | Notes |",
        "|---|---|---|---|---|---|",
    ]
    for row in records:
        safe_note = str(row.get("note", "")).replace("|", "/").replace("\n", " ")[:180]
        summary.append(
            f"| {row['provider']} | {row['status']} | {row.get('http') or '-'} | "
            f"{row.get('links') if row.get('links') is not None else '-'} | "
            f"{row.get('elapsed_ms') if row.get('elapsed_ms') is not None else '-'} | {safe_note} |"
        )
    summary_text = "\n".join(summary) + "\n"
    print(summary_text)
    if os.getenv("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as file:
            file.write(summary_text)
    # Provider health is an observation, not a CI assertion: WAF blocks are common.
    return 0


if __name__ == "__main__":
    sys.exit(main())
