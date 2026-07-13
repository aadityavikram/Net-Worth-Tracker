package com.networth.tracker.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object GoogleDriveAuth {
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    fun authorizationRequest(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            .build()

    fun authorize(context: Context): Task<AuthorizationResult> =
        Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest())

    fun resultFromIntent(context: Context, data: Intent?): AuthorizationResult =
        Identity.getAuthorizationClient(context)
            .getAuthorizationResultFromIntent(data)

    suspend fun awaitAuthorization(context: Context): AuthorizationResult =
        authorize(context).await()

    fun accountEmail(result: AuthorizationResult): String? =
        result.toGoogleSignInAccount()?.email

    fun accessTokenOrNull(result: AuthorizationResult): String? =
        result.accessToken?.takeIf { it.isNotBlank() }
}

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { error ->
        if (cont.isActive) cont.resumeWithException(error)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}
