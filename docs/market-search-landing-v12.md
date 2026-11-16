# Market search compatibility, v0.12

## Inspected input

User-supplied `OPPO-Market1.apk`, package `com.heytap.market`, version
`26.11.3_CN` / `261103`, 94,168,031 bytes. SHA-256:
`7d132ea30c911c18c43308a7bac3e454138d708d1799d0887c29b5dd199f9f1f`.
Inspected DEX directly, not assumptions based on displayed titles. The previous
verified layout remains 26.10.2_CN (see `market-search-landing-v8.md`).

## Confirmed drift and fix

The old hook returned immediately when `a.a.a.jde` or `a.a.a.nhe` was absent.
That disabled even the stable `setSearchHint` hooks. In 26.11.3_CN the dedicated
recommendation helper is `a.a.a.tie`, and its recommendation DTO is `a.a.a.fne`:

| Operation | 26.10.2_CN | 26.11.3_CN |
| --- | --- | --- |
| Read recommendation tag | jde.037f(ActionBar) -> nhe | tie.0528(ActionBar) -> fne |
| Bind/fetch fallback | jde.052b(ActionBar,nhe,String) -> void | tie.052d(ActionBar,fne,String) -> void |
| Private initialization | ActionBar.0792() | ActionBar.0798() |

The getter reads `R$id.search_core_action_bar_recommend_data`. The binder obtains
`getNextRecommendInfo(0)` where needed, sets the same tag, and calls a rich hint
setter. The blank-input button-state function in the new bar calls tie.0528;
returning null prevents an invisible suggested word from becoming its fallback.
Both helpers are public static methods. Their complete parameter and return types
are checked before installing helper hooks. Short obfuscated class names alone
are never enough; ambiguous/missing layouts are logged and not selected.

Stable simple/rich `setSearchHint` hooks now install independently of helper
resolution. All three overloads are unchanged in the new DEX. Rich overloads are
short-circuited before delayed listeners are created, then the simple native setter
updates hint/button state. Initialization cleanup now runs after bar constructors,
not a private obfuscated initializer. The innermost constructor inflates the bar,
calls 0798 (which assigns its EditText), and finishes layout before returning.
Repeated constructor-chain cleanup only calls the hint API. It never sets text.

Unknown helper layouts receive a visible warning and display cleanup only; this
is not a claim that unknown versions have full recommendation fallback suppression.
No global TextView/View hooks, arbitrary DEX scanning, or keyword title matching
were introduced. Actual query callbacks, input text, cursor/selection, history,
and results remain outside these hint hooks.

## Search landing boundary remains valid

Directly checked in the new DEX:

- `SearchHomeCardAdapterPresenter.0792(paging.d,List)` remains the dedicated
  insertion boundary. On page zero it clears the adapter and invokes the history
  Runnable, then calls base `CardAdapterPresenter.0792`, which inserts the list.
- `CardAdapterPresenter.0797` obtains `ViewLayerWrapDto.getCards()` and virtually
  dispatches to 0792. New `SearchHomeLayoutController` layout-state handling in
  the derived callback still delegates to this base path.
- `SearchAllLookingForCard.0e98()` still returns **100203**; its binding still
  requires `TermListGlobalCardDto`, reads `getTerms()`, and renders keyword chips.
- `SearchRecordCardManager.0588(int)` still recognizes **100204**, **40117**,
  **154** as the three history types.

Therefore the existing scoped landing filter and allowlist are unchanged, rather
than broadening/removing unrelated behavior to compensate for the version bump.
Original batch/cache lists remain untouched and first-page history initialization
still runs, including when a filtered batch is empty.

## Verification boundary

`MarketHintLayoutTest` adds old/new helper shape acceptance, swapped-layout
rejection, and wrong getter/binder return, non-static, parameter and missing-method
rejection. Existing `MarketSearchRulesTest` covers retained code identity/order,
unknown cards, empty batches, immutable/repeated batches, schema fallback, hint-only
updates, input/selection preservation and native button-state updates.

These are JVM/schema fixtures, not on-phone hook execution. Final integrated build,
unit test and lint results belong in the release validation record. Device checks
remain required: cold/repeated search entry, cached/refresh/paginated landing,
history clearing, keyword click, typed query/keyboard submission, return from
results, waiting for hint rotation, and toggling the two controls independently.
