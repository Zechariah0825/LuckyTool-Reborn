# Browser search-engine icons

## Cause and native binding

The module-created `api.search.engine.e` model used resource ID `-1` and empty image fields. This correctly avoided resolving a foreign resource ID, but provided neither a bitmap nor a tab-icon URL.

In the freshly inspected 40.10.25.2 reference APK:

- `search.impl.engine.c.getIcon()` reads `k.l()` (light bitmap) or `k.g()` (dark bitmap).
- `k.E(Bitmap)` supplies the light bitmap. Its native dark lookup already falls back to the light bitmap.
- `PagerTabItemView.d(engine)` binds the label, retaining the engine in field `e`.
- `PagerTabItemView.f(engine)` reads the light/dark tab URLs from `c.B()` / `c.z()`, and calls `BrowserDraweeView.setImageURI(Uri, Object)` on field `c`.
- The bundled Fresco `DataFetchProducer` accepts `data:` URIs and decodes their base64 payloads, so the hook supplies local PNG bytes rather than a remote image URL. This is static pipeline evidence, not a device-rendering test.
- Those URLs originate in the sixth/seventh String properties of the engine data model, not in `getIcon()`. Supplying only a bitmap therefore cannot fix the result tabs.
- `initialize()` creates the icon container through `a(RelativeLayout)` only when `setShouldDisplayTabIcon(true)` permits it.

The hook affects only module-created `reborn_` engines. It seeds their native bitmap and supplies a small local PNG data URI at the actual tab binding, including theme refresh. If a module tab lacks its icon container, the verified native creation method runs before label placement. Existing engine routing, default-engine precedence, list selection and native engine icons are unchanged. Icon initialization errors do not prevent search-engine construction.

The provided 913.jpg shows the settings page's text-only native default-engine dropdown, with version 40.10.21.9.6_dba1160. It is not a screenshot of the result tab bar. This patch does not replace that dropdown or infer that it should contain icons.

## Fetch and fallback

Only the configured search engine's HTTPS origin is used. The search template's path, query, fragment and placeholder are never sent to an icon service. Candidates are `/favicon.ico` and `/apple-touch-icon.png` on that origin, with at most two same-origin HTTPS redirects per candidate. There is no favicon aggregator, page scraping, HTTP downgrade, cookie jar, authentication handler or Referer. Nonstandard ports, literal IPs, local hostnames and private/reserved DNS targets use the fallback instead.

The private OkHttp client has two-second connect/read timeouts and a four-second per-call timeout. Work uses two background threads and a bounded queue of 32. Responses are limited to 256 KiB; image dimensions to 1024×1024; displayed output to 64×64. Network reads and image decoding stay off the UI thread. Initial fallback creation is only a small 64×64 letter badge.

PNG/WebP and other platform-decodable formats use bounded BitmapFactory decoding. ICO is explicitly parsed for embedded PNG and uncompressed 24/32-bit DIB payloads; it does not assume Android supports ICO. Unsupported or malformed formats retain the stable site-letter fallback. One process-local LRU holds at most 64 sites, with 24-hour success and five-minute failure caching. Requests for the same origin coalesce. No cache stores a query or custom search URL, and no Activity/View is retained by the loader.

Callbacks use weak targets and binding-generation identity, verify the current native engine key and skip detached views. Reattachment reads cached data. Rebinding to another engine, including a native engine, invalidates the previous callback. Native image-controller replacement handles earlier data-URI loads without changing tab indices.

## Verification scope

Direct HTTPS byte fetches on 2026-11-10 returned Bing (4286 bytes), Google (5430 bytes) and a custom-source example DuckDuckGo (2799 bytes). Bing/Google contain DIB ICO images; DuckDuckGo contains PNG-in-ICO images. Small offline regression fixtures and their exact hashes/source URLs are in `app/src/test/resources/search-icons/README.txt`.

Added tests cover actual site fixtures, synthetic PNG/WebP/ICO, corrupted/oversized data, origin and redirect privacy, private addresses, async coalescing, negative-cache expiry, stable fallback and binding reuse/detach safety. Aggregate build/test results are recorded by the coordinating validation task. APK/DEX inspection and unit tests are not real-phone verification.
