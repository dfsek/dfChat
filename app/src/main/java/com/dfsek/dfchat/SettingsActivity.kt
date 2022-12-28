package com.dfsek.dfchat

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            settings()
        }
        /*
                val mediaStore = InMemoryMediaStore()

                val repositoriesModule = createInMemoryRepositoriesModule()

                val coroutineScope = CoroutineScope(Dispatchers.Default)


                setContentView(binding.root)
                binding.save.setOnClickListener {
                    coroutineScope.launch {
                        try {
                            val matrixClient = MatrixClient.login(
                                baseUrl = Url(binding.homeserver.text.toString()),
                                identifier = IdentifierType.User(binding.username.text.toString()),
                                passwordOrToken = binding.password.text.toString(),
                                repositoriesModule = repositoriesModule,
                                mediaStore = mediaStore,
                                scope = coroutineScope
                            ).getOrThrow()

                            matrixClient.startSync()
                        } catch (e: MatrixServerException) {
                            runOnUiThread {
                                binding.error.text = e.message
                            }
                            return@launch
                        }
                        finish()
                    }
                }

                 */
    }

    @Composable
    @Preview
    fun settings() {
        LazyColumn {
            item {
                account()
            }
        }
    }

    @Composable
    @Preview
    fun account() {
        Row(modifier = Modifier.clickable {
            startActivity(Intent(applicationContext, AccountActivity::class.java))
        }) {
            Text("Account", fontSize = 30.sp)
        }
    }
}