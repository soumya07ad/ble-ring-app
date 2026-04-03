package com.fitness.app.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    val currentUser: Flow<FirebaseUser?>
    
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    fun signOut()
    fun getCurrentUser(): FirebaseUser?
}
