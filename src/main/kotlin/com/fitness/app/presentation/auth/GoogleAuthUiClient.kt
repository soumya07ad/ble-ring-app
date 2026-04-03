package com.fitness.app.presentation.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Helper class that handles Google Sign-In flow and Firebase authentication.
 *
 * The WEB_CLIENT_ID is configured below using the value from google-services.json.
 */
class GoogleAuthUiClient(
    private val context: Context
) {
    companion object {
        // Actual Firebase Web Client ID from google-services.json
        const val WEB_CLIENT_ID = "441426635717-rtk84ar1meo4hg898pgcn4paetuh1058.apps.googleusercontent.com"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    /**
     * Returns the Intent to launch the Google Sign-In chooser.
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Authenticates with Firebase using the Google ID token.
     * @param idToken The ID token obtained from GoogleSignInAccount.
     * @return Result<Unit> indicating success or failure.
     */
    suspend fun signInWithFirebase(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out from both Firebase and Google Sign-In.
     */
    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }
}
