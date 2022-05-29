package com.ls.plugindemo;

import android.app.Application;
import android.content.res.Resources;

import com.ls.plugindemo.plugincore.PluginManager;

/**
 * Description：
 * Created by liushuo on 2022/5/18.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PluginManager.get(getApplicationContext()).loadPlugin();
    }

    @Override
    public Resources getResources() {
        // 重写宿主Application的getResources方法
        // 如果存在插件的Resources对象，便返回插件的Resources
        // 否则返回宿主自己的Resources
        Resources pluginResources = PluginManager.get(getApplicationContext()).getPluginResources();
        if(pluginResources != null){
            return pluginResources;
        }
        return super.getResources();
    }
}
