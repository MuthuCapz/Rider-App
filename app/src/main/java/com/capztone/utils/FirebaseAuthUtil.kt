package com.capztone.utils

import com.google.firebase.auth.FirebaseAuth

object FirebaseAuthUtil {
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
}
