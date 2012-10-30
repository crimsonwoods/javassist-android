package javassist.android;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.DuplicateMemberException;
import javassist.bytecode.ExceptionsAttribute;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;

public class DalvikClassClassPath implements DalvikClassPath {
	private final Class<?> clazz;
	private final JarFile apk;
	
	private DalvikClassClassPath(Class<?> c, Context ctx) {
		clazz = c;
		JarFile jarFile = null;
		try {
			final ApplicationInfo ai = ctx.getApplicationInfo();
			jarFile = new JarFile(ctx, ai.sourceDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		apk = jarFile;
	}
	
	public DalvikClassClassPath(Context context) {
		this(java.lang.Object.class, context);
	}
	
	@Override
	public InputStream openClassfile(String classname) throws NotFoundException {
		throw new NotFoundException("class file is not found.");
	}
	
	@Override
	public ClassFile getClassFile(String classname) throws NotFoundException {
		Class<?> clazz = null;
		try {
			clazz = this.apk.getClass(classname);
		} catch (ClassNotFoundException e) {
			throw new NotFoundException(e.getMessage(), e);
		}
		
		final Class<?> superClass = clazz.getSuperclass();
		final ClassFile cf = new ClassFile(clazz.isInterface(), classname, null == superClass ? null : superClass.getName());
		
		addFields(cf, clazz);
		addConstructors(cf, clazz);
		addMethods(cf, clazz);
		
		return cf;
	}

	@Override
	public URL find(String classname) {
		if (null == apk) {
			return null;
		}
		
		try {
			final Class<?> cls = apk.getClass(classname);
			final URL url = null == cls ? null : new URL("file", "", classname);
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
			final FieldInfo fi = new FieldInfo(cp, f.getName(), Descriptor.of(f.getClass()));
			ret.add(fi);
			fi.setAccessFlags(f.getModifiers());
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
	
	private void addFields(ClassFile cfile, Class<?> clazz) {
		final Field[] fields = clazz.getDeclaredFields();
		if (null != fields && 0 < fields.length) {
			for (Field f : fields) {
				try {
					final FieldInfo fi = new FieldInfo(cfile.getConstPool(), f.getName(), Descriptor.of(f.getType()));
					fi.setAccessFlags(f.getModifiers());
					cfile.addField(fi);
				} catch (DuplicateMemberException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void addConstructors(ClassFile cfile, Class<?> clazz) {
		final Constructor<?>[] ctors = clazz.getDeclaredConstructors();
		if (null != ctors && 0 < ctors.length) {
			for (Constructor<?> c : ctors) {
				final MethodInfo mi = new MethodInfo(cfile.getConstPool(), "<init>", Descriptor.ofConstructor(c.getParameterTypes()));
				buildMethodInfo(cfile.getConstPool(), mi, c);
				try {
					cfile.addMethod(mi);
				} catch (DuplicateMemberException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void addMethods(ClassFile cfile, Class<?> clazz) {
		final Method[] methods = clazz.getDeclaredMethods();
		if (null != methods && 0 < methods.length) {
			for (Method m : methods) {
				final MethodInfo mi = new MethodInfo(cfile.getConstPool(), m.getName(), Descriptor.ofMethod(m.getReturnType(), m.getParameterTypes()));
				buildMethodInfo(cfile.getConstPool(), mi, m);
				try {
					cfile.addMethod(mi);
				} catch (DuplicateMemberException e) {
					e.printStackTrace();
				}
			}
		}
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
