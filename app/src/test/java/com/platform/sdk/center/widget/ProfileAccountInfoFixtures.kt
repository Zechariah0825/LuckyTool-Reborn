package com.platform.sdk.center.widget

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Mirrors the verified SDK field owner, including inherited non-public fields. */
open class HeyTapAccountInfoView(context: Context) : LinearLayout(context) {
    private val mImageBtnSignIn = TextView(context).apply { visibility = View.GONE }
    private val mBtnSignIn = TextView(context)

    val imageButton: TextView get() = mImageBtnSignIn
    val plainButton: TextView get() = mBtnSignIn

    init {
        addView(mImageBtnSignIn)
        addView(mBtnSignIn)
    }
}

/** This is the class inflated by CustomSignInVipCard in the browser APK. */
class CustomSignInAccountInfoView(context: Context) : HeyTapAccountInfoView(context)
