package fpmbuild.superinject;

import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;

final class BridgeClassGenerator {
    private static final MethodTypeDesc VOID_INIT = MethodTypeDesc.of(ConstantDescs.CD_void);

    record Generated(String className, byte[] bytes) {}

    Generated neoForge(String modId) {
        return generateAnnotated(className(modId,"NeoForgeBridge"), "net.neoforged.fml.common.Mod", "value", modId);
    }

    Generated forge(String modId) {
        return generateAnnotated(className(modId,"ForgeBridge"), "net.minecraftforge.fml.common.Mod", "value", modId);
    }

    /** Plain empty class used only because sponge_plugins.json requires an entrypoint. */
    Generated spongeEntrypoint(String modId) {
        return generatePlain(className(modId,"SpongeBridge"));
    }

    private Generated generateAnnotated(String className, String annotationClass, String elementName, String elementValue) {
        ClassDesc thisClass = ClassDesc.of(className);
        Annotation annotation = Annotation.of(ClassDesc.of(annotationClass), AnnotationElement.ofString(elementName, elementValue));
        byte[] bytes = ClassFile.of().build(thisClass, cb -> cb
                .withVersion(ClassFile.JAVA_8_VERSION, 0)
                .withFlags(AccessFlag.PUBLIC, AccessFlag.FINAL, AccessFlag.SUPER)
                .withSuperclass(ConstantDescs.CD_Object)
                .with(RuntimeVisibleAnnotationsAttribute.of(annotation))
                .withMethodBody("<init>", VOID_INIT, ClassFile.ACC_PUBLIC, code -> code
                        .aload(0)
                        .invokespecial(ConstantDescs.CD_Object, "<init>", VOID_INIT)
                        .return_()));
        return new Generated(className, bytes);
    }

    private Generated generatePlain(String className) {
        ClassDesc thisClass = ClassDesc.of(className);
        byte[] bytes = ClassFile.of().build(thisClass, cb -> cb
                .withVersion(ClassFile.JAVA_8_VERSION, 0)
                .withFlags(AccessFlag.PUBLIC, AccessFlag.FINAL, AccessFlag.SUPER)
                .withSuperclass(ConstantDescs.CD_Object)
                .withMethodBody("<init>", VOID_INIT, ClassFile.ACC_PUBLIC, code -> code
                        .aload(0)
                        .invokespecial(ConstantDescs.CD_Object, "<init>", VOID_INIT)
                        .return_()));
        return new Generated(className, bytes);
    }

    private static String className(String modId, String suffix) {
        return "featurecreep.superinjection." + ModId.javaSegment(modId) + "." + suffix;
    }

    static String entryName(String className) {
        return className.replace('.', '/') + ".class";
    }
}
