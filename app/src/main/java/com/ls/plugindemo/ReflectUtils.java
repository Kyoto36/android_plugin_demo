package com.ls.plugindemo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReflectUtils {

    /**
     * 反射实例化对象
     * @param clazz
     * @param args
     * @param <T>
     * @return
     */
    public static <T> T reflectConstructor(Class<T> clazz, Object... args) {
        try {
            if (args == null || args.length <= 0) {
                return clazz.newInstance();
            }
            Class[] argClazzs = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                argClazzs[i] = args[i].getClass();
            }
            Constructor<T> constructor = clazz.getDeclaredConstructor(argClazzs);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean reflectMethod(Object obj, String methodName, Object... args) {

        Class[] argClazzs = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argClazzs[i] = args[i].getClass();
        }
        Method method = null;
        for (Class<?> clazz = obj.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                method = clazz.getDeclaredMethod(methodName, argClazzs);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (method != null) {
                method.setAccessible(true);
                break;
            }
        }
        if (method != null) {
            try {
                method.invoke(obj, args);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void reflectStaticMethod(Class clazz, String methodName, Object... args) {
        try {
            Class[] paramClazz = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramClazz[i] = args[i].getClass();
            }
            Method method = clazz.getDeclaredMethod(methodName, paramClazz);
            method.setAccessible(true);
            method.invoke(null, args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static Object reflectStaticMethodResult(Class clazz, String methodName, Object... args) {
        try {
            Class[] paramClazz = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramClazz[i] = args[i].getClass();
            }
            Method method = clazz.getDeclaredMethod(methodName, paramClazz);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object reflectMethodResult(Object obj, String methodName, Object... args) {
        try {
            Class[] paramClazz = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramClazz[i] = args[i].getClass();
            }
            Method method = obj.getClass().getDeclaredMethod(methodName, paramClazz);
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Class getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Class getClass(ClassLoader classLoader,String className) {
        try {
            if(classLoader != null){
                return classLoader.loadClass(className);
            }
            else{
                return Class.forName(className);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取该对象所有属性的值
     *
     * @return 属性名 -> 属性值，键值对
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static Map<String, Object> getAllFieldValue(Object obj) {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            //获取属性值
            try {
                //开启反射获取私有属性值
                fields[i].setAccessible(true);
                map.put(fields[i].getName(), fields[i].get(obj));
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * 获取单个属性值
     *
     * @param obj
     * @param fieldName
     * @return
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = getField(obj.getClass(),fieldName);
            return field.get(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getStaticFieldValue(Class<?> clazz, String fieldName) {
        try {
            Field field = getField(clazz,fieldName);
            return field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean setField(Object obj,String fieldName,Object fieldValue){
        try {
            Field field = getField(obj.getClass(),fieldName);
            field.set(obj,fieldValue);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取类的属性
     * @param clazz
     * @param fieldName
     * @return
     */
    public static Field getField(Class clazz, String fieldName){
        if(clazz == null){
            return null;
        }
        Field field = null;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if(field != null){
            field.setAccessible(true);
            return field;
        }
        if(clazz == Object.class){
            return null;
        }
        return getField(clazz.getSuperclass(),fieldName);
    }
}
