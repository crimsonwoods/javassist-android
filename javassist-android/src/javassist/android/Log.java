package javassist.android;

public class Log {
	private static final String LOG_TAG = "javassist";
	
	public static void d(String format, Object...args) {
		android.util.Log.d(LOG_TAG, String.format(format, args));
	}
	
	public static void i(String format, Object...args) {
		android.util.Log.i(LOG_TAG, String.format(format, args));
	}
	
	public static void v(String format, Object...args) {
		android.util.Log.v(LOG_TAG, String.format(format, args));
	}
	
	public static void e(String format, Object...args) {
		android.util.Log.e(LOG_TAG, String.format(format, args));
	}
	
	public static void w(String format, Object...args) {
		android.util.Log.w(LOG_TAG, String.format(format, args));
	}
	
	public static void d(Throwable t, String format, Object...args) {
		android.util.Log.d(LOG_TAG, String.format(format, args), t);
	}
	
	public static void i(Throwable t, String format, Object...args) {
		android.util.Log.i(LOG_TAG, String.format(format, args), t);
	}
	
	public static void v(Throwable t, String format, Object...args) {
		android.util.Log.v(LOG_TAG, String.format(format, args), t);
	}
	
	public static void e(Throwable t, String format, Object...args) {
		android.util.Log.e(LOG_TAG, String.format(format, args), t);
	}
	
	public static void w(Throwable t, String format, Object...args) {
		android.util.Log.w(LOG_TAG, String.format(format, args), t);
	}
}
