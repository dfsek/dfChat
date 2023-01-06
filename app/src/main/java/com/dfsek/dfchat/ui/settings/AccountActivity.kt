package com.dfsek.dfchat.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.dfsek.dfchat.*
import com.dfsek.dfchat.state.UserState
import com.dfsek.dfchat.util.SSO_REDIRECT_PATH
import com.dfsek.dfchat.util.SSO_REDIRECT_URL
import com.dfsek.dfchat.util.SSO_REDIRECT_URL_PARAM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.session.Session


class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = intent.data?.getQueryParameter("loginToken")

        if (token != null) {
            val homeserver = intent.data!!.getQueryParameter("homeserver").toString()
            Log.d("SVR", homeserver)

            CoroutineScope(Dispatchers.Default).launch {
                Log.i("Accounts", "Signing in to matrix account")
                try {

                    runOnUiThread {
                        startActivity(Intent(applicationContext, MainActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        setContent {
            Column {
                val client = SessionHolder.currentSession
                if (client == null) {
                    HomeserverUrl()
                } else {
                    CurrentUser(client)
                }
            }
        }
    }

    @Composable
    fun CurrentUser(matrixClient: Session) {
        Column {
            val userState = remember { UserState(matrixClient) }


            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(userState.getAvatar())
                    .crossfade(true)
                    .decoderFactory(BitmapFactoryDecoder.Factory())
                    .build(),
                contentScale = ContentScale.Fit,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(CircleShape)
            )


            Text(userState.getUsername())

            Button(onClick = {
                userState.logout()
                finish()
            }) {
                Text("Log Out")
            }
        }
    }

    @Composable
    @Preview
    fun HomeserverUrl() {

        Column {
            val homeserverUrl = remember { mutableStateOf("") }
            var error by remember { mutableStateOf("") }
            val res = remember { mutableStateOf(emptySet<LoginType>()) }
            TextField(
                value = homeserverUrl.value,
                onValueChange = {
                    homeserverUrl.value = it
                }
            )
            Button(
                onClick = {
                    val connectionConfig = try {
                        HomeServerConnectionConfig
                            .Builder()
                            .withHomeServerUri(Uri.parse(homeserverUrl.value))
                            .build()
                    } catch (e: Exception) {
                        error = "Invalid homeserver: ${e.message}"
                        e.printStackTrace()
                        return@Button
                    }

                    lifecycleScope.launch {
                        DfChat.getMatrix(this@AccountActivity).authenticationService()
                            .getLoginFlow(connectionConfig)
                            .supportedLoginTypes
                            .forEach {
                                Log.d("Login Type", it)
                            }
                    }
                }
            ) {
                Text("Scan")
            }
            Text(text = error)
            /*
            if (res.value.any {
                    it is LoginType.Password
                }) {
                Button(
                    onClick = {
                        Toast.makeText(this@AccountActivity, "Not implemented", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Sign in with Username")
                }
            }
            val sso = res.value.stream().filter {
                it is LoginType.Unknown && it.raw["type"]?.jsonPrimitive?.content == "m.login.sso"
            }.map { it as LoginType.Unknown }.toList()
            sso.forEach { ssoProvider ->
                Log.d("AccountActivity", ssoProvider.raw.toString())
                ssoProvider.raw["identity_providers"]?.jsonArray?.forEach {
                    val providerName = it.jsonObject.get("name")?.jsonPrimitive!!.content
                    val providerId = it.jsonObject.get("id")?.jsonPrimitive!!.content
                    Button(
                        onClick = {
                            val url = createUrl(homeserverUrl.value, providerId)
                            Log.d("AccountActivity", url)
                            openUrlInChromeCustomTab(this@AccountActivity, null, url)
                        }
                    ) {
                        Text("Sign in with $providerName")
                    }
                }
            }

             */
        }
    }

    private fun createUrl(base: String, provider: String): String {
        val trimmed = if (base.endsWith("/")) {
            base.substringBeforeLast("/")
        } else base


        return "$trimmed$SSO_REDIRECT_PATH/$provider?$SSO_REDIRECT_URL_PARAM=${Uri.encode("$SSO_REDIRECT_URL?homeserver=$base")}"
    }

    companion object {
        fun redirectIntent(context: Context, data: Uri?): Intent {
            return Intent(context, AccountActivity::class.java).apply {
                setData(data)
            }
        }
    }
}