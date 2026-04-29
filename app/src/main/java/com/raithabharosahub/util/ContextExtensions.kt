package com.raithabharosahub.util

import androidx.activity.ComponentActivity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): ComponentActivity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity found in context chain")
}