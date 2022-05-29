package com.ls.pluginapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class PluginActivity : BasePluginActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("hahaha", "PluginActivity onCreate ")
        setContentView(R.layout.activity_plugin)
    }

    override fun onResume() {
        Log.d("hahaha", "PluginActivity onResume ")
        super.onResume()
    }

    override fun onPause() {
        Log.d("hahaha", "PluginActivity onPause ")
        super.onPause()
    }

    override fun onDestroy() {
        Log.d("hahaha", "PluginActivity onDestroy ")
        super.onDestroy()
    }
}