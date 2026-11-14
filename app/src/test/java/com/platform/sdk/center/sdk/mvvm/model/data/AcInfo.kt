package com.platform.sdk.center.sdk.mvvm.model.data

/** Minimal verified SDK payload shape; unrelated account data must not be mutated. */
class AcInfo(val userName: String, val button: ButtonInfo?) {
    class ButtonInfo(val buttonText: String)
}
