package com.dfsek.dfchat

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
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
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import kotlin.streams.toList
import androidx.compose.foundation.lazy.items
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


class AccountActivity : AppCompatActivity() {
    lateinit var matrixClient: MatrixClient
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val text = remember { mutableStateOf("") }
            val error = remember { mutableStateOf("") }
            val res = remember { mutableStateOf(emptySet<LoginType>()) }
            TextField(
                value = text.value,
                onValueChange = {
                    text.value = it
                }
            )
            Button(
                onClick = {
                    val matrixRestClient = MatrixClientServerApiClientImpl(
                        baseUrl = Url(text.value),
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
            if(res.value.any {
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
            sso.forEach {
                Log.d("AccountActivity", it.raw.toString())
                it.raw["identity_providers"]?.jsonArray?.forEach {
                    Button(
                        onClick = {

                        }
                    ) {
                        Text("Sign in with ${it.jsonObject.get("name")?.jsonPrimitive?.content}")
                    }
                }
            }
        }

    }
}