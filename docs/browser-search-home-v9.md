# Search+ Home round trip and Google icon

## Failure and change

The user supplied two screenshots: the Search+ provider row has a blue letter
badge for Google; a Home click opens the configured Bing page but Back then
invokes the browser exit prompt instead of returning to Search+.

The previous URL Home route first invoked native return-Home, rewinding the
native page stack to index zero, and then loaded the configured URL. Its root
Back guard deliberately prevented returning to the old news Home. This was not
a suitable round trip from Search+.

For an explicit Home callback on the active `MultiSearchPage` only, load the
configured URL through the native `BaseTab.loadUrl(String)` without invoking
return-Home first. This keeps Search+ as the previous native page. The ordinary
Back handler and web-history logic remain native. No synthetic Back, exit,
history deletion, news Home restoration, or extra Activity is introduced.
Ordinary webpage Home and initial Home keep their existing routing.

The interception is inside the existing version-gated `g1`/`h1` hooks and UI
Home callback scope; Back and background tabs cannot activate it. A synchronous
load failure leaves Search+ in place and logs the error rather than falling
through to the destructive return-Home route. Native permission handling is
unchanged.

## Static evidence and limits

Inspected the supplied reference APK `40.10.25.2` (Library OPPO-Browser.apk):
- Both MultiSearch toolbar listeners `e()` call `tab.h1(true)`.
- `BaseTab.h1` calls `L()` then `Tb(0, animated)` for a non-Home page.
- `loadUrl` creates `LoadParams(url)` and enters native `s9`, then `Q`.
- `Q` passes its actual current page into `h3`, and shows the returned page via
  `A8`; it does not invoke `h1` or explicitly rewind to native Home.

Current-version `g1` / `loadUrl` mappings are already recorded from the original
40.10.21.9.6 APK in browser-home-route-v7.md. This change adds no guessed obfuscated
method names. The original APK could not be re-downloaded through Drive in this
session because its 272,305,359 bytes exceed that connector's 256 MiB limit.
Device behavior, including asynchronous native permission and vendor load
strategy handling, still requires the user's device test; static analysis and
unit tests are not a claim of full runtime verification.

## Google

Google's existing verified favicon fixture is embedded as a small PNG, with its
red/yellow/green/blue G preserved. Google.com and www.google.com origins use it
immediately and do not request a network icon. Other origins still use the
bounded origin-only favicon loader. Labels and lookalike domains do not trigger
Google branding. The asset identifies the provider, not the app itself.

Tests cover the explicit/active/Search+ routing predicate, excluded routes, and
the four-color Google icon without any executor/network request.
