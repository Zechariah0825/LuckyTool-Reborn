package com.platform.sdk.center.widget

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.platform.sdk.center.sdk.mvvm.model.data.AcInfo

/** Mirrors native inherited fields and setButton role branching, not caption matching. */
open class HeyTapAccountInfoView(context: Context) : LinearLayout(context) {
    private val mImageBtnSignIn = TextView(context).apply { visibility = View.GONE }
    private val mBtnSignIn = TextView(context).apply { visibility = View.GONE }
    val imageButton: TextView get() = mImageBtnSignIn
    val plainButton: TextView get() = mBtnSignIn
    var bindingCalls = 0
        private set
    init { addView(mImageBtnSignIn); addView(mBtnSignIn) }

    private fun setButton(context: Context, button: AcInfo.ButtonInfo?, loggedIn: Boolean, third: String?) {
        bindingCalls++
        mImageBtnSignIn.tag = false
        when {
            !button?.buttonText.isNullOrEmpty() -> {
                show(button!!.buttonText)
                mImageBtnSignIn.tag = true
            }
            !loggedIn -> show("Native login")
            !third.isNullOrEmpty() -> show(third)
            else -> mImageBtnSignIn.visibility = View.GONE
        }
    }
    private fun show(text: String) { mImageBtnSignIn.text = text; mImageBtnSignIn.visibility = View.VISIBLE }
    fun nativeBind(button: AcInfo.ButtonInfo?, loggedIn: Boolean, third: String?) =
        setButton(context, button, loggedIn, third)
}

open class CustomSignInAccountInfoView(context: Context) : HeyTapAccountInfoView(context)

/** The real card dispatches the same listener by mIsLogin at click time. */
open class AcBaseView(context: Context) : FrameLayout(context) {
    private var mAccountInfoView: HeyTapAccountInfoView? = null
    private var mIsLogin = false
    private var mThirdBtnText: String? = null
    var loggedIn: Boolean
        get() = mIsLogin
        set(value) { mIsLogin = value }
    var thirdText: String?
        get() = mThirdBtnText
        set(value) { mThirdBtnText = value }
    var loginClicks = 0
    var rewardClicks = 0
    fun wireNativeButton(info: HeyTapAccountInfoView) {
        mAccountInfoView = info
        info.imageButton.setOnClickListener {
            if (mIsLogin) rewardClicks++ else loginClicks++
        }
    }
}
