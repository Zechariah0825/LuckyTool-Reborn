package com.luckyzyx.luckytool.diagnostics

import java.util.UUID

/** Shared-UID processes can load several packages, each with its own channel namespace. */
internal class HookDiagnosticAttachments {
    private val namespaces = mutableSetOf<Pair<String, String>>()
    private val sessions = mutableMapOf<String, String>()

    @Synchronized fun attach(packageName: String, processName: String, register: (String) -> Unit) {
        val namespace = packageName to processName
        if (namespace in namespaces) return
        val session = sessions.getOrPut(processName) { UUID.randomUUID().toString().replace("-", "") }
        register(session)
        // Failed setup can be retried without changing the process counter identity.
        namespaces.add(namespace)
    }
}
