package com.bernaferrari.quietguard.data

import android.content.Context
import org.koin.core.context.GlobalContext

fun Context.preferences(): PreferencesRepository = GlobalContext.get().get()