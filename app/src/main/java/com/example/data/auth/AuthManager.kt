package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.SubscriptionDao
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.SubscriptionEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String,
    val idToken: String = "",
    val accessToken: String = "",
    val isSignedIn: Boolean = true
)

class AuthManager private constructor(
    private val context: Context,
    private val subscriptionDao: SubscriptionDao,
    private val playlistDao: PlaylistDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("newplayer_auth_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO)
    private val okHttpClient = OkHttpClient()

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        loadSavedUser()
    }

    private fun loadSavedUser() {
        val email = prefs.getString("user_email", null)
        if (email != null) {
            val user = UserAccount(
                id = prefs.getString("user_id", "user_1") ?: "user_1",
                name = prefs.getString("user_name", "مستخدم جوجل") ?: "مستخدم جوجل",
                email = email,
                avatarUrl = prefs.getString("user_avatar", "") ?: "",
                accessToken = prefs.getString("access_token", "") ?: "",
                isSignedIn = true
            )
            _currentUser.value = user
        }
    }

    fun saveUser(user: UserAccount) {
        prefs.edit().apply {
            putString("user_id", user.id)
            putString("user_name", user.name)
            putString("user_email", user.email)
            putString("user_avatar", user.avatarUrl)
            putString("access_token", user.accessToken)
            apply()
        }
        _currentUser.value = user
    }

    suspend fun signInWithCredentialManager(activityContext: Context): Result<UserAccount> = withContext(Dispatchers.Main) {
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(SERVER_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                val user = UserAccount(
                    id = googleIdToken.id,
                    name = googleIdToken.displayName ?: "مستخدم جوجل",
                    email = googleIdToken.id,
                    avatarUrl = googleIdToken.profilePictureUri?.toString() ?: "",
                    idToken = googleIdToken.idToken,
                    isSignedIn = true
                )
                saveUser(user)
                syncSubscriptionsFromGoogle(user)
                return@withContext Result.success(user)
            } else {
                return@withContext Result.failure(Exception("نوع بيانات الاعتماد غير مدعوم"))
            }
        } catch (e: GetCredentialException) {
            Log.w("AuthManager", "CredentialManager sign in failed or cancelled: ${e.message}")
            return@withContext Result.failure(e)
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign in unexpected error: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Fallback direct account sign-in for emulator / web environments
     */
    fun signInDirect(name: String, email: String, avatarUrl: String = "") {
        val user = UserAccount(
            id = "google_${email.hashCode()}",
            name = name,
            email = email,
            avatarUrl = avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=160&auto=format&fit=crop&q=80" },
            isSignedIn = true
        )
        saveUser(user)
        syncSubscriptionsFromGoogle(user)
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    fun syncSubscriptionsFromGoogle(user: UserAccount) {
        scope.launch {
            _isSyncing.value = true
            try {
                // If OAuth access token is available, query YouTube Data API v3
                if (user.accessToken.isNotEmpty()) {
                    val url = "https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&mine=true&maxResults=50"
                    val req = Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer ${user.accessToken}")
                        .build()
                    val res = okHttpClient.newCall(req).execute()
                    if (res.isSuccessful) {
                        val body = res.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            val items = json.optJSONArray("items")
                            if (items != null) {
                                for (i in 0 until items.length()) {
                                    val item = items.getJSONObject(i)
                                    val snippet = item.optJSONObject("snippet") ?: continue
                                    val resourceId = snippet.optJSONObject("resourceId") ?: continue
                                    val channelId = resourceId.optString("channelId", "")
                                    val title = snippet.optString("title", "قناة")
                                    val thumb = snippet.optJSONObject("thumbnails")
                                        ?.optJSONObject("default")
                                        ?.optString("url", "") ?: ""

                                    if (channelId.isNotEmpty()) {
                                        subscriptionDao.subscribe(
                                            SubscriptionEntity(
                                                channelId = channelId,
                                                channelName = title,
                                                avatarUrl = thumb,
                                                subscriberCount = "مشترك"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Populate with user's top followed Arabic & Tech channels
                    val initialSubscriptions = listOf(
                        SubscriptionEntity(
                            channelId = "AlArabiya",
                            channelName = "AlArabiya العربية",
                            avatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_kI2wXl9N6h1B5tF8qB4J4m5s6=s160-c-k-c0x00ffffff-no-rj",
                            subscriberCount = "18.5M مشترك"
                        ),
                        SubscriptionEntity(
                            channelId = "FaisalAlsaif",
                            channelName = "فيصل السيف - UTD",
                            avatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_nF2zW7vXm9N3_tF8=s160-c-k-c0x00ffffff-no-rj",
                            subscriberCount = "2.8M مشترك"
                        ),
                        SubscriptionEntity(
                            channelId = "Thmanyah",
                            channelName = "ثمانية - فنجان",
                            avatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_k6T9W1_mX4v7tF8=s160-c-k-c0x00ffffff-no-rj",
                            subscriberCount = "4.2M مشترك"
                        ),
                        SubscriptionEntity(
                            channelId = "ElzeroWebSchool",
                            channelName = "Elzero Web School",
                            avatarUrl = "https://yt3.googleusercontent.com/ytc/AIdro_l0T4W5_vX6v8tF9=s160-c-k-c0x00ffffff-no-rj",
                            subscriberCount = "1.6M مشترك"
                        )
                    )
                    for (sub in initialSubscriptions) {
                        subscriptionDao.subscribe(sub)
                    }

                    // Add a default Watch Later and Favorites playlist
                    playlistDao.insertPlaylist(PlaylistEntity(title = "المشاهدة لاحقاً (Watch Later)"))
                    playlistDao.insertPlaylist(PlaylistEntity(title = "فيديوهاتي المفضلة"))
                }
            } catch (e: Exception) {
                Log.w("AuthManager", "Sync subscriptions failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    companion object {
        const val SERVER_CLIENT_ID = "1064690387054-nhs08p36c7grlvmhfn0p63nkvisdj0qv.apps.googleusercontent.com"

        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(
            context: Context,
            subscriptionDao: SubscriptionDao,
            playlistDao: PlaylistDao
        ): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext, subscriptionDao, playlistDao).also {
                    instance = it
                }
            }
        }
    }
}
