package com.dfsek.dfchat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import kotlin.streams.toList
import com.dfsek.dfchat.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.*
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType


class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = intent.data?.getQueryParameter("loginToken")

        if(token != null) {
            Log.d("TOKEN", token.toString())

            val homeserver = intent.data!!.getQueryParameter("homeserver").toString()
            Log.d("SVR", homeserver)
            val mediaStore = InMemoryMediaStore()

            val repositoriesModule = createInMemoryRepositoriesModule()

            val coroutineScope = CoroutineScope(Dispatchers.Default)

            coroutineScope.launch {
                Log.i("Accounts", "Signing in to matrix account")
                try {
                    matrixClient = MatrixClient.login(
                        baseUrl = Url(homeserver),
                        identifier = null,
                        passwordOrToken = token,
                        loginType = LoginType.Token,
                        mediaStore = mediaStore,
                        deviceId = "dfchat",
                        repositoriesModule = repositoriesModule,
                        scope = coroutineScope
                    ).getOrThrow().also {
                        it.startSync()
                        it.room.getAll()
                            .onEach {
                                it.forEach { roomId, _ ->
                                    Log.d("Room", roomId.full)
                                }
                            }.collect()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        setContent {
            Column {
                homeserverUrl()
            }
        }
    }

    @Composable
    @Preview
    fun homeserverUrl() {

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
        @JvmStatic
        lateinit var matrixClient: MatrixClient
            private set
        fun redirectIntent(context: Context, data: Uri?): Intent {
            return Intent(context, AccountActivity::class.java).apply {
                setData(data)
            }
        }
    }
}