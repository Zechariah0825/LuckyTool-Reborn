# Browser account reward entry: native-role filtering (v0.11)

## Defect and scope

The previous implementation found the two inherited SDK controls, but still hid
only a recognized caption. The SDK has two independent promotion sources: the
remote `AcInfo.button` and the browser-provided `AcBaseView.mThirdBtnText`. Removing
one visible title cannot prevent the other source from being rebound. Neither
caption, localized string lookup, numeric resource ID nor text geometry is used
by the new reward cleanup.

The boundary is the real `MyProfileView` subtree, a `CustomSignInVipCard` (including
subclasses), and that card's exact `AcBaseView.mAccountInfoView` instance. Known
account-info subclasses resolve fields on `HeyTapAccountInfoView`. Unrelated
SDK siblings, standalone account views and cards outside Profile are untouched.

## Fresh reference APK inspection

`OPPO-Browser.apk`, version 40.10.25.2, SHA-256
`e60a6712963c9601d4cb7b756efd3d66c16e5b5bc4576fac92a3ba557d8f3a2a`,
223247642 bytes. Relevant SDK definitions are in `classes17.dex`.

- `HeyTapAccountInfoView.setButton(Context, AcInfo.ButtonInfo, boolean, String)`
  first resets `mImageBtnSignIn.tag` to false. A nonempty remote payload calls
  `setSignBtnText(remoteText, true)` and sets the tag true. With no remote payload,
  `isLogin=false` binds the SDK's login text; `isLogin=true` uses the third-party
  text or hides the control. Null ButtonInfo and third-text inputs are supported
  explicitly and are also passed by the SDK itself. There is no non-null assertion.
- The same `mImageBtnSignIn` / `mBtnSignIn` controls can represent login, so hiding
  them unconditionally would regress authentication. `setShowSignButton` only
  changes the chosen alternative; `setSignBtnText` can directly restore it.
- `AcBaseView.setSignInBtn(int,int,int,String)` and `setSignInBtnText(String)`
  persist `mThirdBtnText`. `applyPendingThirdSignBtnConfig()` can apply it again
  after account refresh, when logged in and remote text is absent.
- `refreshVipAccountUI(AcAccount)` updates `mVipAccount` and `mIsLogin`, then calls
  `setLoginUI` / `setUnLoginUI` and reapplies pending third-party configuration.
  `HeyTapAccountInfoView.setAccountInfo` retains avatar/name/VIP binding and calls
  `setButton`; the custom subclass calls super and reapplies button styling.
- `setThirdSignBtn(int,int,int,String)` styles the button then invokes
  `setSignBtnText(text,false)`. It is not a separate account login operation.
- `AcBaseView.setSignInBtnClickListener` installs a native dispatcher whose
  `handleSignInClick` reads `mIsLogin` on each click. False calls
  `reqVipAccountTask`; true calls `userInfoButtonControl`. Restoring native login
  therefore does not need a replacement click listener. Avatar, name and account
  region separately call `jumpOrLoginTask`, which opens account settings or login.

## Current APK reinspection (2026-11-14)

Freshly read `Browser-Browser.apk`: version `40.10.21.9.6`, code `10210906`,
272305359 bytes, SHA-256
`caeda453881561b60e2342004688b86c830468dcb8e42a06231286581d88b6e1`.
The SDK classes are in `classes17.dex`; MyProfileView is in `classes13.dex`.
All reward-binding signatures, source fields and the branch behavior above were
checked in this current APK, not inferred from the reference version.

Precise current-method offsets (byte offsets in the instruction report):

- `refreshVipAccountUI`: writes `mIsLogin` at `0x22`; calls setLoginUI/setUnLoginUI
  at `0xe0`/`0xec`, then applies pending third configuration at `0xf2`.
- `setButton`: clears the remote tag at `0x44`; accepts null ButtonInfo at `0x4a`;
  branches on login at `0xd4`; the logged-out branch reaches native login caption
  binding at `0xf8`. The remote branch sets the tag true only after caption binding.
- `handleSignInClick`: checks native login state at `0x0`, and calls the real
  login task at `0x10` when logged out. No replacement listener is necessary.
- `AcBaseView.onFinishInflate` calls initView. initView assigns header_layout to
  `mAccountInfoView` at `0x12`, then wires avatar/name/account area listeners.
  The runtime identity check targets precisely this field, not every child view.

The actual XML chain is `home_frame_my_profile_layout` → CustomSignInVipCard →
`account_center_heytap_custom` → CustomSignInAccountInfoView →
`account_center_header_heytap_account_info_custom_sign`. The card's XML constructor
inflates `0x7f0e0023`; the custom account view inflates `0x7f0e001f`. Resource-table
inspection finds only the default configuration for each layout. The final layout
contains both `COUIIconButton` at `button_login_sign` (`0x7f0b0522`) and COUIButton
at `nearx_btn_sign_in` (`0x7f0b153e`), matching base initView's field assignment.
Consequently this verified profile layout is not Nearx-only: the image control
and its native tag used for pre-attachment recovery exist. The SDK's setButton
would early-return if that control were absent; no unsupported Nearx-only login
reconstruction is claimed. IDs here document evidence; production uses none.

## Implementation

Before the native private `setButton` binds a scoped component, remove only the
remote ButtonInfo and third-party text arguments. Do not mutate the shared AcInfo
model, account identity, login Boolean or login/refresh method. Let native code
bind a real login button when logged out and no promotional button when logged in.
Clear the scoped card's third-party text source and intercept its setters so an
async account refresh cannot restore the fallback reward. Native style and account
binding still execute.

Scoped `setSignBtnText` uses its remote-source Boolean and native card login state,
not title content; `setShowSignButton` cannot reveal an already-logged-in reward.
Attachment and the existing profile-local layout observer handle pre-attachment
binding. A tagged remote control bound while detached is rebound through the
verified native setButton with null promotion inputs, preserving the logged-out
login branch. Ordinary login is not rebound on every layout. No global visibility
hook, caption blacklist, removed account card or listener replacement is added.

## Validation boundary

Focused fixtures mirror the native role branches, inherited private fields,
card ownership and click-time login dispatch. Tests cover changing/arbitrary
captions, null promotion input, unchanged shared account data and login Boolean,
login rebinding/clicks, refresh and detach/reattach cleanup, inherited controls,
shadow fields, unrelated SDK instances and retained avatar/VIP/actions. These are
helper/binding regression tests, not execution of Xposed hooks on an OPPO phone.

Fresh integrated validation on 2026-11-14 passed compilation and all 367 unit
tests, including 16 reward/profile cases. The two new cases verify that a scoped
card's unrelated sibling is untouched and that pre-attachment cleanup uses the
actual account-view identity. Current-version APK inspection is complete as
documented above. Full lint remains failed on existing missing translations;
see [the release validation record](../VALIDATION_REBORN_11.md) for exact counts.
No phone/Vector runtime validation is claimed.
