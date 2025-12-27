package com.jewer.bodycam.backend.functions

import android.os.Build

fun getPhoneName(): String {
    return "${Build.MODEL}   ${Build.ID}"
}