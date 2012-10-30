package javassist.android;

import javassist.ClassPath;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;

import java.util.List;

public interface DalvikClassPath extends ClassPath {
	List<FieldInfo> getClassFields(String classname, ConstPool cp);
	List<MethodInfo> getClassMethods(String classname, ConstPool cp);
	ClassFile getClassFile(String classname) throws NotFoundException;
}
