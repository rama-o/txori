package com.rama.adiskide.utils

import android.content.Context
import kotlin.math.roundToInt

fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density).roundToInt()
