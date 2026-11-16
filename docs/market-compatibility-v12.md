# Market 26.11.3_CN compatibility

Input: user-supplied `OPPO-Market1.apk`, package `com.heytap.market`, version
`26.11.3_CN` / `261103`, 94,168,031 bytes.
SHA-256: `7d132ea30c911c18c43308a7bac3e454138d708d1799d0887c29b5dd199f9f1f`.

## Home regression and fix

The original 26.10.2 mapping treated `HomeSubTabPresenter.\u08af` as the
integer home/back target and `\u08b3` as the startup index. In 26.11.3,
a new FragmentManager field at `\u08ac` shifts subsequent members, so
`\u08af` now holds `HomeChosenCardFragment`; the corresponding integer
fields moved to `\u08b0` and `\u08b4`. The prior typed safety check refuses
to filter, so the native Featured page returns.

Static DEX inspection confirms:
- Final paired binding remains `\u079a(List,List) -> void`.
- Layout `\u08a8`, pager `\u08aa`, and pager-item title/fragment/route fields
  remain unchanged.
- Full-CTA initialization assigns `getFirstPagePos()` to both new index fields
  before generating real pages. The chosen fragment is inserted at that index.
- The one-shot adapter observer reads the home index via `\u0795`; Back and
  `onPageSelected` both read `\u08b0`.
- The new `a.a.a.dl7` factory still preserves each route on the paired item;
  `/appmoment/main` still supports the native `HomeChildCardFragment` fallback.

`MarketHomeSchema` selects only one of the two verified typed field layouts.
It refuses missing, changed, or ambiguous schemas before any mutation. The hook
still filters completed title/page pairs, never the source DTO or firstPagePos.
It preserves the existing scope: remove the actual Featured page, default to the
real App Moment page, retain other native tabs, preserve a still-valid selected
native tab on rebind, and use App Moment as the Back target. It does not relabel
Featured as App Moment or remove view text globally.

Tests cover both typed layouts, the new fragment/index collision, missing fields,
wrong primitive types, wrong fragment identity and ambiguous profiles. Existing
pair/filter tests continue to cover page identity, reordered tabs, repeated binds,
selection restoration and safe fallback.

## Verification boundary

Static APK analysis and host tests are not a ColorOS/Vector runtime test. On-device
checks remain necessary: cold start, App Moment content, changing tabs, Back,
refresh/rebind, and switch-disabled behavior. Search-specific evidence is recorded
separately. Preserve browser behavior and the existing module identity/signature.
