package org.jamruby.javassistsample;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.android.DexFile;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String DEX_FILE_NAME_MYCLASSES = "myclasses.dex";
    private static final boolean FORCE_GENRATE_DEX = false;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final TextView view = (TextView)findViewById(R.id.textview_title);
        final File dexFile = new File(getFilesDir(), DEX_FILE_NAME_MYCLASSES);
        
        if (!dexFile.exists() || FORCE_GENRATE_DEX) {
        	// generate DEX and ODEX file.
        	try {
        		// generate "xxx.class" file via Javassist.
		        final ClassPool cp = ClassPool.getDefault(getApplicationContext());
		        final CtClass cls = cp.makeClass("hoge");
		        final CtConstructor ctor = new CtConstructor(null, cls);
	        	ctor.setBody("{}");
	        	cls.addConstructor(ctor);
				final CtMethod m1 = CtMethod.make(
						"public java.lang.String toString() { return \"hoge.toString() is called.\"; }",
						cls);
				cls.addMethod(m1);
				final CtMethod m2 = CtMethod.make(
						"public void setText(android.widget.TextView view) { view.setText((java.lang.CharSequence)\"hoge.setText() is called.\"); }",
						cls);
				cls.addMethod(m2);
				cls.writeFile(getFilesDir().getAbsolutePath());
				
				// convert from "xxx.class" to "xxx.dex"
		        final DexFile df = new DexFile();
		        final String dexFilePath = dexFile.getAbsolutePath();
		        df.addClass(new File(getFilesDir(), "hoge.class"));
		        df.writeFile(dexFilePath);
        	} catch (Exception e) {
        		e.printStackTrace();
        		Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        	}
        }
        
        if (dexFile.exists()) {
        	try {
		        final DexClassLoader dcl = new DexClassLoader(
		        		dexFile.getAbsolutePath(),
		        		getCacheDir().getAbsolutePath(),
		        		getApplicationInfo().nativeLibraryDir,
		        		getClassLoader());
		        String title = null;
	        	final Class<?> class_hoge = dcl.loadClass("hoge");
				final Constructor<?> ctor = class_hoge.getConstructor(new Class<?>[0]);
				final Object obj = ctor.newInstance(new Object[0]);
				title = obj.toString();
				view.setText(title);
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						try {
							final Method m = obj.getClass().getDeclaredMethod("setText", TextView.class);
							m.invoke(obj, view);
						} catch (Exception e) {
							e.printStackTrace();
							Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
						}
					}
				}, 2000);
        	} catch (Exception e) {
        		e.printStackTrace();
        		Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        	}
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
