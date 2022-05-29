package com.ls.plugindemo.plugincore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class RegisteredActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onPause() {
        Log.d("RegisteredActivity", "onPause: ")
        super.onPause()
    }

    override fun onResume() {
        Log.d("RegisteredActivity", "onResume: ")
        super.onResume()
    }

    override fun onDestroy() {
        Log.d("RegisteredActivity", "onDestroy: ")
        super.onDestroy()
    }
}