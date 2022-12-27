package com.dfsek.dfchat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dfsek.dfchat.databinding.ActivitySettingsBinding
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.MatrixServerException
import org.koin.dsl.module

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mediaStore = InMemoryMediaStore()

        val repositoriesModule = createInMemoryRepositoriesModule()

        val coroutineScope = CoroutineScope(Dispatchers.Default)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
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
    }
}