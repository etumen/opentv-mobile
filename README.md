# OpenTV Mobile

**A free, open-source IPTV player built for Android phones** — portrait browsing, touch
controls, offline downloads and fast search over huge catalogues.

No account. No subscription. No server of ours between you and your provider.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

> **Built on [OpenTV](https://github.com/opentvproject/opentv)** by the OpenTV contributors.
> OpenTV is a d-pad-first player for Android TV; OpenTV Mobile started as a fork of it and
> reworks the app around phones. All of OpenTV's history, copyright notices and licence are kept
> — see [Credits](#credits). If this app is useful to you, consider supporting the original
> authors: [GitHub Sponsors](https://github.com/sponsors/legionnaireneyland) ·
> [PayPal](https://www.paypal.com/donate/?business=leetobin1982@gmail.com&item_name=Support+OpenTV&no_recurring=0&currency_code=GBP).

## What's different from OpenTV

- **Phone shell** — top app bar and bottom tabs; browse in portrait, play in landscape
- **Touch player** — swipe to zap channels or skip, brightness/volume drags, true full screen
- **Live TV** — inline search scoped to the category, a category sheet with its own filter and
  channel counts, *My categories* (e.g. only `ES|`), *Recent* channels, sort A–Z / by number
- **Country-aware categories** — `ES| Sports` and `DE| Sports` stay apart
- **Films & series** — origin tags on titles (`ES`, `EN 4K`, `NF`…) with filter chips, cleaned
  titles, 3-column grids, Continue watching per tab
- **Search** — system keyboard, instant full-text search over 180k+ titles, recent searches
- **Downloads** — save films and episodes for offline viewing, queued one at a time for
  single-connection providers
- **Performance** — built and tested against a 55k-channel / 180k-film provider

The TV layout still works: on Android TV and Google TV the app keeps OpenTV's d-pad interface.

## Why this exists

There is a recurring pattern in this corner of the app world. A promising IPTV player appears.
It looks good. It sells a "lifetime" subscription. The lifetime turns out to be shorter than
the buyer's — the developer burns out, or moves on, or simply stops replying, and everyone who
paid is left with an app that no longer works and no way to fix it.

The people affected are not helpless. They are often technical. What they lack is not skill
but *access*: the code is closed, so nobody else can pick it up.

OpenTV is the same category of app built so that cannot happen. The source is public and
GPL-licensed. If the current maintainers vanish tomorrow, anyone can fork it, build it and
keep it alive. That is the entire point.

**OpenTV is not a fork or a decompilation of any existing app.** It is written from scratch.

## What it does

- **Live TV** from Xtream Codes logins and plain M3U/M3U8 playlists
- **A guide that stays put** — XMLTV EPG that survives restarts, refreshes incrementally, and
  never wipes itself
- **Recording (DVR)** — record to the box, a USB drive or your NAS (SMB); schedule from the
  guide; series-link a whole show; play back in-app with full seeking
- **Catch-up / archive** where your provider offers it, plus **programme reminders**
- **Movies and series** with resume and per-profile watch history
- **Favourites, categories, hide channels, search**, picture-in-picture, aspect control, and
  hand-off to an external player
- **Free sync between your own devices** — favourites, watch history and NAS recordings, over
  your wifi or Tailscale, with no server of ours
- **Multiple languages** (fully translated to Spanish), a parental PIN, profiles, and self-updates
- **D-pad first** — designed for a remote, works with a touchscreen
- **One APK** for Android TV, Fire TV, phones and tablets

## Screenshots

The live TV guide — programme grid, favourites, and a live preview:

![OpenTV live TV guide](docs/screenshots/01-guide.png)

| Recordings &amp; reminders | Free cloud sync — your own NAS, no server |
| :---: | :---: |
| ![Recordings and reminders](docs/screenshots/02-recordings.png) | ![NAS cloud sync](docs/screenshots/03-cloud-sync.png) |
| Movies &amp; series | Settings |
| ![Movies](docs/screenshots/04-movies.png) | ![Settings](docs/screenshots/05-settings.png) |

Open-source through and through — the About screen carries the licence, the links, and a donation QR:

![About](docs/screenshots/06-about.png)

## What it deliberately does not do

- **No servers of ours, no web dashboard, no account.** Your provider credentials never leave
  your device, and syncing happens directly between your devices — or through your own NAS —
  never through a machine we run. This is a feature, not a gap — see
  [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the reasoning, and note that "sync your
  channel list to *our* servers" is precisely the feature whose hosting bill makes a one-off
  payment unsustainable.
- **No content.** OpenTV is a player. You bring a service you already pay for. The project has
  no affiliation with any provider and does not help you find one.

## Install

Grab the APK from [Releases](../../releases) and open it on your phone (allow installs from that
source when Android asks). It installs as its own app — alongside OpenTV if you have it.

## Build it yourself

```bash
git clone https://github.com/pablocesar87/opentv-mobile.git
cd opentv-mobile
./gradlew assembleDebug
```

You need JDK 17+ and the Android SDK (API 35). Android Studio will fetch it for you. The debug
APK lands in `app/build/outputs/apk/debug/`.

Run the tests:

```bash
./gradlew test
```

## How it was built

OpenTV was written with **Claude**, Anthropic's AI assistant, working from a human's
direction — the design decisions, the priorities, the "no server, ever" rule, and every
review of what shipped were the maintainer's; Claude did the bulk of the drafting, wiring and
debugging against that direction.

That is relevant here for one specific reason: **it kept the project genuinely clean-room.**
Nothing was decompiled or copied from any existing IPTV app. The code was written from public
specifications (the Xtream Codes request format, the XMLTV and M3U formats, the Kodi catch-up
tags) and from describing how other apps *behave* as a user — never from their source. That is
the legal footing that lets anyone fork this and keep it alive, which is the whole point.

## Contributing

Please do. See [CONTRIBUTING.md](CONTRIBUTING.md).

The project is explicitly looking for people who own devices the maintainers do not. Most
"works for me" bugs in this category are device-specific — an Ugoos box, a first-gen ONN, a
Fire Stick Lite — and a bug report from someone who owns one is worth more than a week of
guessing. If a channel fails on your setup, [open an issue](../../issues/new/choose).

## Credits

OpenTV Mobile is a derivative of **[OpenTV](https://github.com/opentvproject/opentv)**, written
by the OpenTV contributors and licensed GPL-3.0-or-later. The full commit history of OpenTV is
preserved in this repository, and every file keeps its original copyright notice; changes made
here are © the OpenTV Mobile contributors under the same licence.

The original project's donation links are kept on purpose: this app would not exist without
their work. [GitHub Sponsors](https://github.com/sponsors/legionnaireneyland) ·
[PayPal](https://www.paypal.com/donate/?business=leetobin1982@gmail.com&item_name=Support+OpenTV&no_recurring=0&currency_code=GBP)

## Licence

[GPL-3.0-or-later](LICENSE).

Chosen deliberately. GPL means anyone can take this code, but if they ship it they must ship
their source too. Nobody gets to close it, rebrand it, and sell a lifetime subscription on top
of work the community did for free.

## Legal

OpenTV is a media player, comparable to VLC. It ships with no channels, no playlists, and no
links to any. What you point it at, and whether you are entitled to, is between you and your
provider.
