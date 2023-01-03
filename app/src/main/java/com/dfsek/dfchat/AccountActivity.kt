package com.dfsek.dfchat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.dfsek.dfchat.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClientImpl
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import net.folivo.trixnity.core.model.events.Event
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.subscribe
import kotlin.streams.toList


class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = intent.data?.getQueryParameter("loginToken")

        if(token != null) {
            val homeserver = intent.data!!.getQueryParameter("homeserver").toString()
            Log.d("SVR", homeserver)
            val mediaStore = InMemoryMediaStore()

            val repositoriesModule = createRealmRepositoriesModule()

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
                        runOnUiThread {
                            startActivity(Intent(applicationContext, MainActivity::class.java))
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        setContent {
            Column {
                HomeserverUrl()
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
        init {
            tryLogIn()
        }
        @JvmStatic
        var matrixClient: MatrixClient? = null
            private set(it) {
                field = it
                CoroutineScope(Dispatchers.Default).launch {
                    it?.api?.sync?.subscribe<RoomMessageEventContent.TextMessageEventContent> {
                        Log.d("Matrix Message Event", it.toString())
                    }
                    it?.api?.sync?.subscribeAllEvents {
                        Log.d("Matrix Event", it.toString())
                    }
                }
            }
        fun redirectIntent(context: Context, data: Uri?): Intent {
            return Intent(context, AccountActivity::class.java).apply {
                setData(data)
            }
        }

        private fun tryLogIn() {
            CoroutineScope(Dispatchers.Default).launch {
                matrixClient = MatrixClient.fromStore(
                    repositoriesModule = createRealmRepositoriesModule(),
                    mediaStore = InMemoryMediaStore(),
                    scope = CoroutineScope(Dispatchers.Default),
                ).getOrThrow()
            }
        }
    }
}