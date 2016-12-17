/*
 * Copyright 2015 Nicola Fäßler
 *
 * This file is part of StrimBagZ.
 *
 * StrimBagZ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.lubot.strimbagzrewrite.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import com.orhanobut.logger.Logger
import net.lubot.strimbagzrewrite.Constants
import net.lubot.strimbagzrewrite.R
import net.lubot.strimbagzrewrite.util.Utils

class LoginActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("url")
        setContentView(R.layout.activity_login)
        WebView.setWebContentsDebuggingEnabled(true)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        if (supportActionBar != null) {
            supportActionBar!!.title = "Login"
            supportActionBar!!.setHomeButtonEnabled(true)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        val webView = findViewById(R.id.loginView) as WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        webView.setWebViewClient(object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.contains("http://localhost/")) {
                    val token = url.substring(url.indexOf("=") + 1, url.indexOf("&"))
                    Logger.i("Token %s", token)
                    val settings = getSharedPreferences(Constants.SETTINGS, MODE_PRIVATE)
                    val editor = settings.edit()
                    editor.putString("oauth_token", token)
                    editor.apply()
                    Utils.getTwitchUser(this@LoginActivity)
                    return true
                }
                return false
            }

        })
        webView.loadUrl(url)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
