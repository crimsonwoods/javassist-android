package javassist.environment;

public class Environment {
	public static boolean isRunningOnDalvikVM() {
		final String vmName = System.getProperty("java.vm.name");
		return "Dalvik".equals(vmName);
	}
}
