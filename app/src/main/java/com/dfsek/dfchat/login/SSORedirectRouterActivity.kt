package com.dfsek.dfchat.login

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dfsek.dfchat.AccountActivity

class SSORedirectRouterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(AccountActivity.redirectIntent(this, intent.data))
        finish()
    }
}