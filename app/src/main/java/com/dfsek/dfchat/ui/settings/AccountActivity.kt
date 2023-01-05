package com.dfsek.dfchat.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.dfsek.dfchat.*
import com.dfsek.dfchat.state.LoginState
import com.dfsek.dfchat.state.UserState
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.*
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import kotlin.streams.toList


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
                    LoginState.logInToken(Url(homeserver), token)

                    runOnUiThread {
                        startActivity(Intent(applicationContext, MainActivity::class.java))
                        finish()
                    }

                    LoginState.matrixClient?.startSync()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        setContent {
            Column {
                val client = LoginState.matrixClient
                if (client == null) {
                    HomeserverUrl()
                } else {
                    CurrentUser(client)
                }
            }
        }
    }

    @Composable
    fun CurrentUser(matrixClient: MatrixClient) {
        Column {
            val userState = remember { UserState(matrixClient) }
            var avatar: ByteArray? by remember { mutableStateOf(null) }

            LaunchedEffect(matrixClient) {
                userState.getAvatar {
                    avatar = it
                }
            }

            avatar?.let {
                Log.d("Channel Image", "Drawing image...")
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(it)
                        .crossfade(true)
                        .decoderFactory(BitmapFactoryDecoder.Factory())
                        .build(),
                    contentScale = ContentScale.Fit,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                )
            } ?: Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.Cyan)
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
            val error = remember { mutableStateOf("") }
            val res = remember { mutableStateOf(emptySet<LoginType>()) }
            TextField(
                value = homeserverUrl.value,
                onValueChange = {
                    homeserverUrl.value = it
                }
            )
            Button(
                onClick = {
                    val matrixRestClient = MatrixClientServerApiClientImpl(
                        baseUrl = Url(homeserverUrl.value),
                    )
                    val coroutineScope = CoroutineScope(Dispatchers.Default)

                    coroutineScope.launch {
                        matrixRestClient.authentication.getLoginTypes()
                            .onFailure {
                                it.printStackTrace()
                                error.value = it.message.toString()
                            }
                            .onSuccess {
                                res.value = it
                            }
                    }
                }
            ) {
                Text("Scan")
            }
            Text(text = error.value)
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