package com.ls.pluginapp

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

/**
 * 插件的全部Activity都继承于这个类
 */
abstract class BasePluginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getResources(): Resources {
        // 因为插件的全部Activity都继承于这个类，所以当Activity需要加载资源的时候，会访问这个getResources方法
        // 如果获取application的resources不为空
        //    如果当前app以插件形式在宿主中运行，那得到的便是宿主Application中的Resources对象
        //    又因为宿主的Application返回的是插件的Resources对象，所以最终加载的仍然是插件的资源
        //    如果当前app独立运行，那么得到的便是是自身的Application，那么返回的将是自身的Resources对象
        // 否则返回自身的Resources对象
        val pluginResources = application?.resources
        if(pluginResources != null){
            return pluginResources
        }
        return super.getResources()
    }
}