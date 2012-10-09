package javassist.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import javassist.NotFoundException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.ExceptionsAttribute;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;

public class DalvikClassClassPath implements DalvikClassPath {
	private final Class<?> clazz;
	private final Context context;
	private final DexClassLoader dexClassLoader;
	
	private DalvikClassClassPath(Class<?> c, Context ctx) {
		clazz = c;
		context = ctx;
		final ApplicationInfo ai = context.getApplicationInfo();
		File f;
		try {
			f = createClassesDexFile(ai);
		} catch (IOException e) {
			Log.e(e, "failed to load a DEX file.");
			f = null;
		}
		if (null != f) {
			dexClassLoader = new DexClassLoader(f.getAbsolutePath(), context.getFilesDir().getAbsolutePath(), ai.nativeLibraryDir, context.getClassLoader());
		} else {
			dexClassLoader = null;
		}
	}
	
	public DalvikClassClassPath(Context context) {
		this(java.lang.Object.class, context);
	}
	
	private File createClassesDexFile(ApplicationInfo ai) throws FileNotFoundException, IOException {
		final DexClassLoader apkLoader = new DexClassLoader(new File(":" + ai.sourceDir, ai.packageName).getAbsolutePath(), context.getFilesDir().getAbsolutePath(), ai.nativeLibraryDir, context.getClassLoader());
		final InputStream is = apkLoader.getResourceAsStream("classes.dex");
		try {
			final FileOutputStream fos = new FileOutputStream(new File(context.getCacheDir(), "classes.dex"));
			try {
				byte[] buffer = new byte[4096];
				int eof_count = 0;
				for (;;) {
					int n = is.read(buffer);
					if (-1 == n) {
						++eof_count;
						if (eof_count > 5) {
							break;
						}
						Thread.sleep(10);
					} else {
						eof_count = 0;
						fos.write(buffer, 0, n);
					}
				}
			} finally {
				fos.close();
			}
		} catch (InterruptedException e) {
			return null;
		} finally {
			is.close();
		}
		return new File(context.getCacheDir(), "classes.dex");
	}
	
	@Override
	public InputStream openClassfile(String classname) throws NotFoundException {
		throw new NotFoundException("class file is not found.");
	}

	@Override
	public URL find(String classname) {
		if (null == dexClassLoader) {
			return null;
		}
		
		try {
			final Class<?> cls = dexClassLoader.loadClass(classname);
			final URL url = null == cls ? null : new URL("file", "", new File(context.getFilesDir(), classname).getAbsolutePath());
			Log.d("%s.find(%s) = %s", getClass().getSimpleName(), classname, url);
			return url;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void close() {
		// nothing to do.
	}

	@Override
	public List<FieldInfo> getClassFields(String classname, ConstPool cp) {
		final Field[] fields = clazz.getDeclaredFields();
		if (null == fields || 0 == fields.length) {
			return null;
		}
		final ArrayList<FieldInfo> ret = new ArrayList<FieldInfo>();
		for (Field f : fields) {
			ret.add(new FieldInfo(cp, f.getName(), Descriptor.of(f.getClass())));
		}
		return ret;
	}

	@Override
	public List<MethodInfo> getClassMethods(String classname, ConstPool cp) {
		final Method[] methods = clazz.getDeclaredMethods();
		final Constructor<?>[] ctors = clazz.getConstructors();
		final ArrayList<MethodInfo> ret = new ArrayList<MethodInfo>();
		if (null != methods && 0 != methods.length) {
			for (Method m : methods) {
				final MethodInfo mi = new MethodInfo(cp, m.getName(), Descriptor.ofMethod(m.getReturnType(), m.getParameterTypes()));
				ret.add(mi);
				buildMethodInfo(cp, mi, m);
			}
		}
		if (null != ctors && 0 != ctors.length) {
			for (Constructor<?> c : ctors) {
				final MethodInfo mi = new MethodInfo(cp, "<init>", Descriptor.ofConstructor(c.getParameterTypes()));
				ret.add(mi);
				buildMethodInfo(cp, mi, c);
			}
		}
		return 0 == ret.size() ? null : ret;
	}
	
	private void buildMethodInfo(ConstPool cp, MethodInfo mi, Method m) {
		mi.setAccessFlags(m.getModifiers());
		final Class<?>[] excs = m.getExceptionTypes();
		if (null != excs && 0 != excs.length) {
			final ExceptionsAttribute ea = new ExceptionsAttribute(cp);
			buildExceptionsAttribute(ea, excs);
			mi.setExceptionsAttribute(ea);
		}
	}
	
	private void buildMethodInfo(ConstPool cp, MethodInfo mi, Constructor<?> ctor) {
		mi.setAccessFlags(ctor.getModifiers());
		final Class<?>[] excs = ctor.getExceptionTypes();
		if (null != excs && 0 != excs.length) {
			final ExceptionsAttribute ea = new ExceptionsAttribute(cp);
			buildExceptionsAttribute(ea, excs);
			mi.setExceptionsAttribute(ea);
		}
	}
	
	private void buildExceptionsAttribute(ExceptionsAttribute ea, Class<?>[] exceptions) {
		if (null == exceptions || 0 == exceptions.length) {
			return;
		}
		
		final String[] list = new String[exceptions.length];
		for (int i = 0; i < list.length; ++i) {
			list[i] = exceptions[i].getName();
		}
		ea.setExceptions(list);
	}

}
