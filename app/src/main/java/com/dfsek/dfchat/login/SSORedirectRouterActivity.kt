package com.dfsek.dfchat.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dfsek.dfchat.ui.settings.AccountActivity

class SSORedirectRouterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(AccountActivity.redirectIntent(this, intent.data))
        finish()
    }
}