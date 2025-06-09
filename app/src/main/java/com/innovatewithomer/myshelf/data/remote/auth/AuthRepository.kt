package com.innovatewithomer.myshelf.data.remote.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


interface AuthRepository {
    val currentUser: FirebaseUser?
    suspend fun signInAnonymously(): Result<FirebaseUser>
    suspend fun signOut()
}

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
): AuthRepository {

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            Result.success(authResult.user!!)
        } catch (e:Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }
}