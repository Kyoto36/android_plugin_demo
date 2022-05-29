package com.ls.plugindemo.plugincore;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ls.plugindemo.BuildConfig;
import com.ls.plugindemo.FileUtil;
import com.ls.plugindemo.ReflectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;

/**
 * Description：
 * Created by liushuo on 2022/5/18.
 */
public class PluginManager {
    private static PluginManager instance;

    public static PluginManager get(Context context){
        if(instance == null){
            instance = new PluginManager(context);
        }
        return instance;
    }

    private static final String TAG = PluginManager.class.getSimpleName();
    private static final String PLUGIN_CACHE_DIR_NAME = "plugin_cache";
    private static final String PLUGIN_UNCOMPRESS_DIR = "plugin_odex";

    private Context mContext;
    private String mPluginPath;
    private DexClassLoader mPluginClassloader;
    private Resources mPluginResources;

    private PluginManager(Context context){
        mContext = context;
    }

    public DexClassLoader getPluginClassloader() {
        return mPluginClassloader;
    }

    public Resources getPluginResources() {
        return mPluginResources;
    }

    public void loadPlugin(){
        // 拷贝插件到私有目录
        boolean success = copyPlugin();
        if(!success){
            return;
        }
        // 合并插件中的dex到宿主中
        success = dexMerge();
        if(!success){
            return;
        }
        // 拦截AMS检查，替换为宿主Intent
        success = hookAMS();
        if(!success){
            return;
        }

        // 拦截AMS回调，替换回插件Intent
        success = hookHandler();
        if(!success){
            return;
        }

        // 创建插件Resources
        success = createPluginResource();

    }

    private boolean copyPlugin() {
        String pluginName = "plugin.apk";
        // 插件所在目录
        mPluginPath = mContext.getExternalFilesDir(null).getAbsolutePath() + File.separator + pluginName;
        // 最终拷贝到的私有目录
        String targetPath = mContext.getDir(PLUGIN_CACHE_DIR_NAME,Context.MODE_PRIVATE).getAbsolutePath();
        if(!new File(mPluginPath).exists()){
            // 如果插件不存在
            Log.e(TAG,"plugin not exists !!!");
            return false;
        }
        File targetFile = new File(targetPath);
        if(targetFile.exists()){
            // 如果目标目录已有插件，则删除
            targetFile.delete();
        }
        if(!targetFile.getParentFile().exists()){
            // 如果目标目录不存在，则创建
            targetFile.getParentFile().mkdirs();
        }
        try {
            // 拷贝插件到私有缓存目录
            FileInputStream in = new FileInputStream(mPluginPath);
            FileOutputStream out = new FileOutputStream(targetPath);
            FileUtil.copy(in, out);
        }catch (Exception e){
            Log.e(TAG,"plugin copy failed !!!");
            e.printStackTrace();
            return false;
        }
        // 创建解析插件的ClassLoader
        // PLUGIN_UNCOMPRESS_DIR 插件解压地址，但是在Android8之后就无用了，插件解压地址又系统特定
        mPluginClassloader = new DexClassLoader(targetPath,PLUGIN_UNCOMPRESS_DIR,null,mContext.getClassLoader());
        return true;
    }

    private boolean dexMerge(){
        if(mPluginClassloader == null){
            return false;
        }
        String pathList = "pathList";
        // 获取到宿主ClassLoader的pathList属性
        Object systemPathList = ReflectUtils.getFieldValue(mContext.getClassLoader(),pathList);
        // 获取到插件ClassLoader的pathList属性
        Object pluginPathList = ReflectUtils.getFieldValue(mPluginClassloader,pathList);
        String dexElements = "dexElements";
        // 获取到宿主pathList的dexElements属性
        Object systemDexElements = ReflectUtils.getFieldValue(systemPathList,dexElements);
        // 获取到插件pathList的dexElements属性
        Object pluginDexElements = ReflectUtils.getFieldValue(pluginPathList,dexElements);
        if(systemDexElements == null || pluginDexElements == null){
            // dexElements获取失败
            Log.e(TAG,"dexElements not found, systemDexElements = " + systemDexElements + ", pluginDexElements = " + pluginDexElements);
            return false;
        }
        // 合并宿主的dexElements和插件的dexElements为新的dexElements数组
        Object newElements = combineArray(pluginDexElements,systemDexElements);
        if(newElements == null){
            // dexElements合并失败
            Log.e(TAG,"dexMerge failed !!!");
            return false;
        }
        // 新的dexElements数组设置给宿主的pathList中的dexElements属性
        if(!ReflectUtils.setField(systemPathList,dexElements,newElements)){
            // 新的dexElements设置失败
            Log.e(TAG,"new dexElements set failed !!!");
            return false;
        }
        return true;
    }

    /**
     * 合并数组
     *
     * @param arrayLhs 前数组（插队数组）
     * @param arrayRhs 后数组（已有数组）
     * @return 处理后的新数组
     */
    private Object combineArray(Object arrayLhs, Object arrayRhs) {
        // 获得一个数组的Class对象，通过Array.newInstance()可以反射生成数组对象
        Class<?> localClass = arrayLhs.getClass().getComponentType();
        // 前数组长度
        int i = Array.getLength(arrayLhs);
        // 新数组总长度 = 前数组长度 + 后数组长度
        int j = i + Array.getLength(arrayRhs);
        if (localClass != null) {
            // 生成数组对象
            Object result = Array.newInstance(localClass, j);
            for (int k = 0; k < j; ++k) {
                if (k < i) {
                    // 从0开始遍历，如果前数组有值，添加到新数组的第一个位置
                    Array.set(result, k, Array.get(arrayLhs, k));
                } else {
                    // 添加完前数组，再添加后数组，合并完成
                    Array.set(result, k, Array.get(arrayRhs, k - i));
                }
            }
            return result;
        }
        return null;
    }

    private boolean hookAMS(){
        // 系统是9.0及以下，获取IActivityManager单例对象
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
            Log.e(TAG,"api <= 25，start hook AMS !!!");
            Object singleton = null;
            // 系统是7.1及以下，获取IActivityManager单例的类名是ActivityManagerNative，属性名是gDefault
            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1){
                Class<?> clazz = ReflectUtils.getClass("android.app.ActivityManagerNative");
                Object gDefault = ReflectUtils.getStaticFieldValue(clazz,"gDefault");
                singleton = gDefault;
                if(singleton == null){
                    // api <= 25，get gDefault failed
                    Log.e(TAG,"api <= 25，get gDefault failed !!!");
                    return false;
                }
            }
            // 系统是9.0及以下，获取IActivityManager单例的类名是ActivityManager，属性名是IActivityManagerSingleton
            else{
                Log.e(TAG,"api <= 28，start hook AMS !!!");
                Object IActivityManagerSingleton = ReflectUtils.getStaticFieldValue(ActivityManager.class,"IActivityManagerSingleton");
                singleton = IActivityManagerSingleton;
                if(singleton == null){
                    // api <= 25，get IActivityManagerSingleton failed
                    Log.e(TAG,"api <= 28，get IActivityManagerSingleton failed !!!");
                    return false;
                }
            }
            // 获取单例对象的实例，也就是AMS
            Object AMSObject = ReflectUtils.getFieldValue(singleton,"mInstance");
            Class<?> IActivityManagerClazz = ReflectUtils.getClass("android.app.IActivityManager");
            // 对AMS对象进行动态代理，拦截startActivity方法，将Intent参数替换成宿主的Activity
            Object AMSProxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{IActivityManagerClazz}, (proxy, method, args) -> {
                if("startActivity".equals(method.getName())) {
                    Log.d(TAG,"Proxy IActivityManager startActivity invoke...");
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof Intent) {
                            Intent intent = new Intent();
                            intent.setClass(mContext, RegisteredActivity.class);
                            intent.putExtra("actionIntent", (Intent) args[i]);
                            args[i] = intent;
                            Log.d(TAG,"replaced startActivity intent");
                        }
                    }
                }
                return method.invoke(AMSObject,args);
            });
            // 将动态代理实例设置给单例
            boolean success = ReflectUtils.setField(singleton,"mInstance",AMSProxy);
            if(!success){
                Log.e(TAG,"api <= 28，AMS hook failed !!!");
            }
            return success;
        }
        // 系统是12及以下，获取IActivityTaskManager单例对象，API29开始将单独提取出了ActivityTaskManager来管理Activity
        else{
            Log.e(TAG,"api <= 32，start hook A(T)MS !!!");
            // 获取ActivityTaskManager类的静态属性IActivityTaskManagerSingleton，是一个单例
            Class<?> clazz = ReflectUtils.getClass("android.app.ActivityTaskManager");
            Object IActivityTaskManagerSingleton = ReflectUtils.getStaticFieldValue(clazz,"IActivityTaskManagerSingleton");
            // 获取单例的实例，即A(T)MS对象
            Object AMSObject = ReflectUtils.getFieldValue(IActivityTaskManagerSingleton,"mInstance");
            Class<?> IActivityTaskManagerClazz = ReflectUtils.getClass("android.app.IActivityTaskManager");
            // 对A(T)MS对象进行动态代理，拦截startActivity方法，将Intent参数替换成宿主的Activity
            Object AMSProxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{IActivityTaskManagerClazz}, (proxy, method, args) -> {
                if("startActivity".equals(method.getName())) {
                    Log.d(TAG,"Proxy IActivityTaskManager startActivity invoke...");
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof Intent) {
                            Intent intent = new Intent();
                            intent.setClass(mContext, RegisteredActivity.class);
                            intent.putExtra("actionIntent", (Intent) args[i]);
                            args[i] = intent;
                            Log.d(TAG,"replaced startActivity intent");
                        }
                    }
                }
                return method.invoke(AMSObject,args);
            });
            // 将动态代理实例设置给单例
            boolean success = ReflectUtils.setField(IActivityTaskManagerSingleton,"mInstance",AMSProxy);
            if(!success){
                Log.e(TAG,"api > 28，AMS hook failed !!!");
            }
            return success;
        }
    }

    private boolean hookHandler(){
        // 拦截AMS检查完成之后的消息，将启动Activity的消息拦截，将消息中的Intent替换会插件Activity
        // 获取ActivityThread类
        Class<?> activityThreadClazz = ReflectUtils.getClass("android.app.ActivityThread");
        // 获取ActivityThread的静态属性sCurrentActivityThread，也是它自己的实例
        Object activityThreadObject = ReflectUtils.getStaticFieldValue(activityThreadClazz,"sCurrentActivityThread");
        // 获取ActivityThread实例的mH属性，是一个Handler消息处理器
        Object mHObject = ReflectUtils.getFieldValue(activityThreadObject,"mH");
        // 设置Handler的实例属性mCallback为HookHmCallback，在其中进行消息拦截
        boolean success = ReflectUtils.setField(mHObject,"mCallback",new HookHmCallback());
        if(!success){
            Log.e(TAG,"api > 28，Handler hook failed !!!");
        }
        return success;
    }

    private class HookHmCallback implements Handler.Callback {
        private static final int LAUNCH_ACTIVITY         = 100;
        private static final int EXECUTE_TRANSACTION = 159;


        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                // API 21 ~ 27 启动Activity的消息是LAUNCH_ACTIVITY
                case LAUNCH_ACTIVITY:
                    Log.d(TAG,"HookHmCallback handleMessage LAUNCH_ACTIVITY enter !!!");
                    // 消息对象是ActivityClientRecord对象，其中包含Intent
                    // 获取intent对象
                    Object intentObject = ReflectUtils.getFieldValue(msg.obj,"intent");
                    if(intentObject instanceof  Intent){
                        Intent intent = (Intent) intentObject;
                        // 将之前替换缓存下来的插件Intent替换回去
                        Parcelable actionIntent = intent.getParcelableExtra("actionIntent");
                        if(actionIntent != null){
                            boolean success = ReflectUtils.setField(msg.obj,"intent",actionIntent);
                            if(success){
                                Log.d(TAG,"HookHmCallback handleMessage LAUNCH_ACTIVITY replaced !!!");
                            }
                        }
                    }
                    break;
                // API 28 ~ 32，添加了事务管理，启动Activity的消息是EXECUTE_TRANSACTION
                case EXECUTE_TRANSACTION:
                    Log.d(TAG,"HookHmCallback handleMessage EXECUTE_TRANSACTION enter !!!");
                    // 启动Activity之中EXECUTE_TRANSACTION其中一条消息，需要找到属于启动Activity的那条消息
                    // 消息对象是ClientTransaction对象，其中有ClientTransactionItem列表
                    // 启动Activity的Item是LaunchActivityItem，其中包含Intent
                    // 获取mActivityCallbacks，Item列表对象
                    Object mActivityCallbacksObject = ReflectUtils.getFieldValue(msg.obj,"mActivityCallbacks");
                    if(mActivityCallbacksObject instanceof List){
                        List mActivityCallbacks = (List) mActivityCallbacksObject;
                        // 循环列表
                        for (Object callbackItem : mActivityCallbacks) {
                            // 找到LaunchActivityItem对象
                            if(TextUtils.equals(callbackItem.getClass().getName(),"android.app.servertransaction.LaunchActivityItem")){
                                // 获取LaunchActivityItem的Intent对象
                                Object mIntentObject = ReflectUtils.getFieldValue(callbackItem,"mIntent");
                                if(mIntentObject instanceof Intent){
                                    Intent mIntent = (Intent) mIntentObject;
                                    // 将之前替换缓存下来的插件Intent替换回去
                                    Parcelable actionIntent = mIntent.getParcelableExtra("actionIntent");
                                    if(actionIntent != null){
                                        boolean success = ReflectUtils.setField(callbackItem,"mIntent",actionIntent);
                                        if(success){
                                            Log.d(TAG,"HookHmCallback handleMessage EXECUTE_TRANSACTION replaced !!!");
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
            return false;
        }
    }

    private boolean createPluginResource() {
        // 创建属于插件的Resources对象
        Log.e(TAG, "createPluginResource: enter !!!");
        try {
            // 创建Resources所需的AssetManager对象
            AssetManager pluginAssetManager = AssetManager.class.newInstance();
            // 设置AssetManager的资源路径为插件路径
            boolean success = ReflectUtils.reflectMethod(pluginAssetManager,"addAssetPath",mPluginPath);
            if(!success){
                Log.e(TAG, "createPluginResource: addAssetPath failed !!!");
            }
            // 创建Resources对象
            mPluginResources = new Resources(pluginAssetManager,mContext.getResources().getDisplayMetrics(),mContext.getResources().getConfiguration());
            return success;
        } catch (Exception e) {
            Log.e(TAG, "createPluginResource: AssetManager instantiation failed !!!");
            e.printStackTrace();
            return false;
        }
    }



}
