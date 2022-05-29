package com.ls.plugindemo

import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.ls.plugindemo.plugincore.PluginManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resources
        // 通过PathClassLoader加载插件中的类
        val pluginTestClass = classLoader?.loadClass("com.ls.pluginapp.PluginTest")?:return
        // 通过反射调用类静态方法
        ReflectUtils.reflectStaticMethod(pluginTestClass,"doSomeThing")

        findViewById<Button>(R.id.hello_world).setOnClickListener {
            val intent = Intent()
            intent.component = ComponentName("com.ls.pluginapp","com.ls.pluginapp.PluginActivity")
            startActivity(intent)
        }
    }
}