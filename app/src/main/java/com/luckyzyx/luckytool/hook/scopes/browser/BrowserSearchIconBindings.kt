package com.luckyzyx.luckytool.hook.scopes.browser

import java.util.WeakHashMap

/** Main-thread binding generations; values contain no reference back to their weak key. */
internal class BrowserSearchIconBindings<T : Any> {
    class Token(val origin: String?, val label: String, val key: String)
    private val entries = WeakHashMap<T, Token>()
    fun bind(target: T, origin: String?, label: String, key: String): Token =
        Token(origin, label, key).also { entries[target] = it }
    fun remove(target: T) { entries.remove(target) }
    fun current(target: T, token: Token) = entries[target] === token
    fun mayApply(target: T, token: Token, attached: Boolean, engineKey: String?) =
        attached && current(target, token) && token.key == engineKey
}
