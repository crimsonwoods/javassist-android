package javassist.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import dalvik.system.DexClassLoader;

public class JarFile {
	private static final String RESOURCE_CLASSES_DEX = "classes.dex";
	
	private final DexClassLoader classLoader;
	
	public JarFile(Context context, String path) throws FileNotFoundException, IOException {
		this.classLoader = loadClasses(loadFile(context, path), context, new File(path).getName());
	}
	
	public Class<?> getClass(String classname) throws ClassNotFoundException {
		if (null == this.classLoader) {
			throw new ClassNotFoundException();
		}
		return this.classLoader.loadClass(classname);
	}
	
	private static DexClassLoader loadFile(Context context, String path) throws FileNotFoundException {
		final File f = new File(path);
		final String fileName = f.getName().toLowerCase();
		if (!fileName.endsWith(".jar") && !fileName.endsWith(".zip") && !fileName.endsWith(".apk")) {
			throw new UnsupportedOperationException("unsupported file type.");
		}
		if (!f.exists()) {
			throw new FileNotFoundException(String.format("'%s' is not exist.", f.getAbsolutePath()));
		}
		return new DexClassLoader(
				path,
				context.getCacheDir().getAbsolutePath(),
				context.getApplicationInfo().nativeLibraryDir,
				context.getClassLoader());
	}
	
	private static DexClassLoader loadClasses(DexClassLoader jarLoader, Context context, String jarFileName) throws IOException {
		final InputStream is = jarLoader.getResourceAsStream(RESOURCE_CLASSES_DEX);
		if (null == is) {
			return null;
		}
		
		final File dir = new File(context.getFilesDir(), jarFileName);
		final File dexFile = new File(dir, RESOURCE_CLASSES_DEX);
		
		try {			
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					throw new IOException(String.format("cannot make directory '%s'.", dir.getAbsolutePath()));
				}
			} else if (!dir.isDirectory()) {
				throw new IllegalStateException(String.format("cannot make directory '%s'.", dir.getAbsolutePath()));
			}
			
			final FileOutputStream fos = new FileOutputStream(dexFile);
			try {
				final byte[] buffer = new byte[4096];
				
				for (;;) {
					final int n = is.read(buffer);
					if (0 > n) {
						break;
					}
					fos.write(buffer, 0, n);
				}
				fos.flush();
			} finally {
				fos.close();
			}
			return new DexClassLoader(
					dexFile.getAbsolutePath(),
					context.getCacheDir().getAbsolutePath(),
					context.getApplicationInfo().nativeLibraryDir,
					context.getClassLoader());
		} finally {
			final String[] files = dir.list();
			if (null != files) {
				for (String file : files) {
					new File(dir, file).delete();
				}
			}
			dir.delete();
			is.close();
		}
	}
}
