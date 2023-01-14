package com.dfsek.dfchat.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.dfsek.dfchat.DfChat
import com.dfsek.dfchat.AppState
import com.dfsek.dfchat.state.UserState
import com.dfsek.dfchat.ui.rooms.DirectMessagesActivity
import com.dfsek.dfchat.util.SSO_REDIRECT_URL
import com.dfsek.dfchat.util.openUrlInChromeCustomTab
import im.vector.lib.multipicker.MultiPicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.session.Session


class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = intent.data?.getQueryParameter("loginToken")

        if (token != null) {
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    Log.i("Session", "Signing in to matrix account")
                    AppState.startSession( DfChat.getMatrix(this@AccountActivity)
                        .authenticationService()
                        .getLoginWizard()
                        .loginWithToken(token))

                    runOnUiThread {
                        startActivity(Intent(applicationContext, DirectMessagesActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        setContent {
            MaterialTheme(colors = AppState.themeColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column {
                        SettingsTopBar("Account")
                        val client = AppState.session
                        if (client == null) {
                            LoginForm()
                        } else {
                            CurrentUser(client)
                        }
                    }
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

            val scope = rememberCoroutineScope()

            Button(onClick = {
                scope.launch {
                    userState.logout()
                    runOnUiThread {
                        finish()
                    }
                }
            }) {
                Text("Log Out")
            }
        }
    }

    @Composable
    @Preview
    fun LoginForm() {
        val showUsernamePasswordLogin = remember { mutableStateOf(false) }
        Column {
            val homeserverUrl = remember { mutableStateOf("") }
            var error by remember { mutableStateOf("") }
            var loginTypes by remember { mutableStateOf<LoginFlowResult?>(null) }


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
                        loginTypes = DfChat.getMatrix(this@AccountActivity).authenticationService()
                            .getLoginFlow(connectionConfig)
                    }
                }
            ) {
                Text("Scan")
            }
            Text(text = error)
            if (loginTypes?.supportedLoginTypes?.any {
                    it == "m.login.password"
                } == true) {
                Button(
                    onClick = {
                        showUsernamePasswordLogin.value = true
                    }
                ) {
                    Text("Sign in with Username")
                }
            }
            loginTypes?.ssoIdentityProviders?.forEach {
                DfChat.getMatrix(this@AccountActivity).authenticationService().getSsoUrl(SSO_REDIRECT_URL, null, it.id)
                    ?.let {  url ->
                        Button(
                            onClick = {
                                Log.d("Homeserver Discovery", url)
                                openUrlInChromeCustomTab(this@AccountActivity, null, url)
                            }
                        ) {
                            Text("Sign in with ${it.name}")
                        }
                    }
            }
        }
        UsernamePasswordLogin(showUsernamePasswordLogin)
    }

    @Composable
    fun UsernamePasswordLogin(visible: MutableState<Boolean>) {
        if(visible.value) {
            var username by remember { mutableStateOf("") }

            var password by remember { mutableStateOf("") }
            var passwordVisible by remember { mutableStateOf(false) }

            var error by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()
            Dialog(
                onDismissRequest = {
                    visible.value = false
                },
                content = {
                    Surface(shape = MaterialTheme.shapes.medium) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Sign In With Username", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
                            TextField(value = username, onValueChange = {
                                username = it
                            }, placeholder = { Text("Username") })
                            Divider()
                            TextField(value = password, onValueChange = {
                                password = it
                            }, placeholder = { Text("Password") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    val image = if (passwordVisible)
                                        Icons.Filled.Visibility
                                    else Icons.Filled.VisibilityOff
                                    val description = if (passwordVisible) "Hide password" else "Show password"
                                    IconButton(onClick = {passwordVisible = !passwordVisible}){
                                        Icon(imageVector  = image, description)
                                    }
                                }
                            )
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        AppState.startSession(DfChat.getMatrix(this@AccountActivity)
                                            .authenticationService()
                                            .getLoginWizard()
                                            .login(username, password, "dfchat"))
                                        AppState.session?.open()
                                        AppState.session?.syncService()?.startSync(true)
                                        visible.value = false
                                        runOnUiThread {
                                            startActivity(Intent(applicationContext, DirectMessagesActivity::class.java))
                                            finish()
                                        }
                                    } catch (failure: Throwable) {
                                        failure.message?.let { error = it }
                                    }
                                }
                            }) {
                                Text("Sign In")
                            }

                            Text(error, color = MaterialTheme.colors.error)
                        }
                    }
                }
            )
        }
    }

    companion object {
        fun redirectIntent(context: Context, data: Uri?): Intent {
            return Intent(context, AccountActivity::class.java).apply {
                setData(data)
            }
        }
    }
}