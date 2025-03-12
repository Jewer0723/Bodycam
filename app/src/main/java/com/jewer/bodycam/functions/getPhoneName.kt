package com.jewer.bodycam.functions

import android.os.Build

fun getPhoneName(): String {
    return "${Build.MODEL}   ${Build.ID}"
}