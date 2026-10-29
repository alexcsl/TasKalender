package com.example.tectest

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LocalDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getInstance().options.projectId == "demo-taskalender-portfolio") {
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8085)
        }
    }
}
