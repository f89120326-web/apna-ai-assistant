# NEON OS

A futuristic mobile-first React interface for a personal command dashboard.

## Features
- HUD-inspired dark interface with cyan/violet ambient lighting
- Live clock and responsive navigation
- Focus timer with selectable durations, pause and reset
- Local-first notes with search, create and delete
- Interface settings for ambient glow and reduced motion
- Best-effort battery display when supported by the browser

## Develop
```sh
npm install
npm run dev
``

## Build
```sh
npm run build
``

## Honest capability notes
This is a web app, not a native Android launcher. Browser support controls whether battery telemetry is available. Notes stay in local browser storage; there is no cloud sync or account system. It does not silently listen for voice commands or control Android system settings.
