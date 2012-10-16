package org.jamruby.javassistsample;

import java.io.File;
import java.lang.reflect.Constructor;

import dalvik.system.DexClassLoader;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.android.DexFile;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String DEX_FILE_NAME_MYCLASSES = "myclasses.dex";

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final TextView view = (TextView)findViewById(R.id.textview_title);
        final File dexFile = new File(getCacheDir(), DEX_FILE_NAME_MYCLASSES);
        
        if (!dexFile.exists()) {
        	// generate DEX and ODEX file.
        	try {
		        final ClassPool cp = ClassPool.getDefault(getApplicationContext());
		        final CtClass cls = cp.makeClass("hoge");
	        	final CtConstructor ctor = new CtConstructor(null, cls);
	        	ctor.setBody("{}");
	        	cls.addConstructor(ctor);
				final CtMethod m1 = CtMethod.make(
						"java.lang.String toString() { return \"hoge.toString() is called.\"; }",
						cls);
				cls.addMethod(m1);
				cls.writeFile(getFilesDir().getAbsolutePath());
				
		        final DexFile df = new DexFile();
		        final String dexFilePath = new File(getCacheDir(), DEX_FILE_NAME_MYCLASSES).getAbsolutePath();
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
		        		getFilesDir().getAbsolutePath(),
		        		getApplicationInfo().nativeLibraryDir,
		        		getClassLoader());
		        String title = null;
	        	final Class<?> class_hoge = dcl.loadClass("hoge");
				final Constructor<?> ctor = class_hoge.getConstructor(new Class<?>[0]);
				final Object obj = ctor.newInstance(new Object[0]);
				title = obj.toString();
				view.setText(title);
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
