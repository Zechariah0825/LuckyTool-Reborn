# Search landing: history only

## Evidence and scope

Inspected original OPPO Browser 40.10.21.9.6, 272305359 bytes, SHA256 `caeda453881561b60e2342004688b86c830468dcb8e42a06231286581d88b6e1`. Screenshot 997 shows the empty-input landing page with a history section, advertising banner and ranking tabs. No user history strings are included in these tests or this document.

`search.suggest.view.h.b()` selects mode 0 for an empty query and mode 1 otherwise. `r(String,String,String)` switches back to mode 0 when the query is cleared/blank. Its `d(int)` factory creates `SearchInputPart` for mode 0 and a separate `NativeSugPart` for mode 1. Only the former is changed. The input bar/back/search/voice controls are outside its content root. Search results, engine selection/default synchronization and typed suggestions are not hooked.

In classes13.dex, `SearchInputPart.f()` inflates `browser_search_main_layout`, then calls `m02.a.a(part, abilityFactory, container)`. In classes19.dex, this card manager registers cards in its `a: SparseArray` and calls `e(ViewGroup)` to create their native holders. Item `search.suggest.card.item.g`, type/key 4, owns `SearchHistoryCard` and subscribes to history updates. Item `h`, key 8, owns holder `v` for the native incognito header notice and must also retain its native lifecycle. Other entries are clipboard, related search, banners, AI/recommendation cards and rank lists. The manager's later refresh/focus/theme/destroy operations iterate this same registry.

The layout has a `SearchMainScrollView` root. Its direct `ll_header: SearchScrollHeadView` contains the direct `card_history` child and non-history placeholders. The other direct child `browser_search_main_layout` contains ranking tabs and the pager. Resource names and parent relationships are checked at runtime, rather than relying on Chinese strings or copied numeric resource IDs.

## Implementation

New independent preference `reborn_browser_search_history_only` defaults to true in both the module UI and hook. Turning it off and restarting the browser restores the native landing page. The legacy app-promotion suggestion option retains its previous scope.

Before native `m02.a.e(ViewGroup)` initializes holders/subscriptions, retain the original key-4 history object and exact key-8 incognito notice object. First verify the native history-holder field, exact part owner for every registry entry, original history key/class and exact layout structure. A missing or changed contract leaves the original registry/page untouched. No history is deleted or synthesized.

Hide only the verified layout's non-history direct siblings, except the exact native incognito notices at their verified placements. `SearchInputPart.I()` dynamically adds `search.suggest.view.IncognitoView` directly to the root at index 0; `H(boolean)` controls its native visibility. The header layout directly includes `search.suggest.view.IncognitoViewHead`; its key-8 card controls visibility and initialization. Neither notice is forced visible or hidden by this hook. The separately declared `IncognitoViewSlogan` has no verified insertion into this landing hierarchy, so no speculative exception is added. A weak-root layout observer prevents later layout/visibility updates from restoring those placeholders; detach removes its observer and reattach reinstalls it. Native history visibility, row binding, item clicks, expand/collapse and deletion listeners remain unchanged. Empty history and incognito do not force a history card visible. No whole-window text scans or global LiveData/network methods are hooked.

This removes non-history card rendering/subscriptions; it does not claim all background recommendation network requests are suppressed.

## Checks

Regression tests cover registry history identity, recommendations/banner/rank/clipboard exclusion, missing/changed mappings, unrelated or typed-suggestion owners, repeated initialization, history clear control preservation, input controls outside the subtree, later layout additions, native empty-history visibility, dynamic incognito insertion/visibility/placement, incognito card identity and hierarchy mismatch. Test search text is synthetic.

The integrated v0.8 test run passed all 424 tests in 61 suites, including 11 search-landing tests. DEX/layout inspection and unit tests do not constitute real-device verification. Other browser versions are not newly validated by this change; runtime contract checks fail open on mismatches. See `VALIDATION_REBORN_8.md` for the complete validation record.
