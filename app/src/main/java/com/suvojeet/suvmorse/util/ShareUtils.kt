package com.suvojeet.suvmorse.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/** Helpers for sharing text out of the app, with a WhatsApp fast-path. */
object ShareUtils {

    /** Opens the system share sheet for [text]. */
    fun share(context: Context, text: String) {
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Share via")) }
    }

    /**
     * Sends [text] straight to WhatsApp (or WhatsApp Business as a fallback).
     * @return true if a WhatsApp app handled it, false if none is installed.
     */
    fun shareToWhatsApp(context: Context, text: String): Boolean {
        if (text.isBlank()) return false
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage(pkg)
            }
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // try the next package
            }
        }
        return false
    }
}
