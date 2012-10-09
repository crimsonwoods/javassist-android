package org.jamruby.javassistsample;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import dalvik.system.DexClassLoader;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.android.DexFile;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final ClassPool cp = ClassPool.getDefault(getApplicationContext());
        final CtClass cls = cp.makeClass("hoge");
        try {
        	final CtConstructor ctor = new CtConstructor(null, cls);
        	ctor.setBody("{}");
        	cls.addConstructor(ctor);
			final CtMethod m1 = CtMethod.make("java.lang.String toString() { return \"hoge.toString() is called.\"; }", cls);
			cls.addMethod(m1);
			cls.writeFile(getFilesDir().getAbsolutePath());
		} catch (CannotCompileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        final DexFile df = new DexFile();
        final String dexFilePath = new File(getCacheDir(), "myclasses.dex").getAbsolutePath();
        df.addClass(new File(getFilesDir(), "hoge.class"));
        try {
			df.writeFile(dexFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        final DexClassLoader dcl = new DexClassLoader(dexFilePath, getFilesDir().getAbsolutePath(), getApplicationInfo().nativeLibraryDir, getClassLoader());
        String title = null;
        try {
			final Class<?> class_hoge = dcl.loadClass("hoge");
			final Constructor<?> ctor = class_hoge.getConstructor(new Class<?>[0]);
			final Object obj = ctor.newInstance(new Object[0]);
			title = obj.toString();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
        
        if (null != title) {
        	TextView view = (TextView)findViewById(R.id.textview_title);
			view.setText(title);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
