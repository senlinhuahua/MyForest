package com.forest.myforest.utils;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.forest.myforest.MainActivity;

import java.io.File;

import dalvik.system.BaseDexClassLoader;

public class DexLoader {
    private Context context;


    public void loadJar(){
        File optizmizedDexOutPath = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"loader_dex.jar");
        BaseDexClassLoader classLoader = new BaseDexClassLoader(Environment.getExternalStorageDirectory().getAbsolutePath().toString(),
                optizmizedDexOutPath,optizmizedDexOutPath.getAbsolutePath(),context.getClassLoader());
        Class libProviderClazz = null;
        try {
            // 载入JarLoader类， 并且通过反射构建JarLoader对象， 然后调用sayHi方法
            libProviderClazz = classLoader.loadClass("com.example.interf.DynamicsLoader");
            //IDynamicsLoader loader = (IDynamicsLoader) libProviderClazz.newInstance();
            //Toast.makeText(MainActivity.this, loader.sayHi(), Toast.LENGTH_SHORT).show();
        } catch (Exception exception) { // Handle exception gracefully here.
            exception.printStackTrace();
        }


    }
}
