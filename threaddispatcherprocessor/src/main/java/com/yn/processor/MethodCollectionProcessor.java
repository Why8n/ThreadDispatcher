package com.yn.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import com.yn.annotations.Switch;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;

import static jdk.nashorn.internal.runtime.JSType.isPrimitive;

/**
 * Created by Whyn on 2017/8/9.
 */
@AutoService(Processor.class)
public class MethodCollectionProcessor extends AbstractProcessor {
    public static final String OPTION_DISPATCHER_INDEX = "dispatcherIndex";
    private Messager mMessager;
    private Filer mFiler;
    private Set<ExecutableElement> mMethodCollections = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mMessager = processingEnvironment.getMessager();
        mFiler = processingEnvironment.getFiler();
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<>();
        options.add(OPTION_DISPATCHER_INDEX);
        return options;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Switch.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        mMethodCollections.clear();
        String indexFullClassName = processingEnv.getOptions().get(OPTION_DISPATCHER_INDEX);
        if (indexFullClassName == null) {
            error("No option " + OPTION_DISPATCHER_INDEX +
                    " passed to annotation processor");
            return false;
        }
        note("receive option: " + indexFullClassName);
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Switch.class)) {
            if (element.getKind() != ElementKind.METHOD) {
                error(String.format("annotatino @%s must be used in method.",
                        Switch.class.getSimpleName()));
                return false;
            }
            mMethodCollections.add((ExecutableElement) element);
        }
        return parseCollection(indexFullClassName);
    }

    private boolean parseCollection(String indexFullClassName) {
        if (mMethodCollections.isEmpty())
            return false;
        try {
            brewJava(indexFullClassName).writeTo(mFiler);
        } catch (IOException e) {
            error("Failed to write to filer");
        }
        return true;
    }

    private JavaFile brewJava(String indexFullClassName) {
        int lastDotPos = indexFullClassName.lastIndexOf(".");
        String pkName = indexFullClassName.substring(0, lastDotPos);
        String className = indexFullClassName.substring(lastDotPos + 1,
                indexFullClassName.length());
        note(String.format("pkName=%s,className=%s", pkName, className));
        return JavaFile.builder(pkName, brewClass(className))
                .addFileComment("Generated code from ThreadDispatcher. Do not modify!")
                .build();
    }

    private TypeSpec brewClass(String className) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(className)
                .addSuperinterface(ClassName.get("com.yn.processor", "MethodBase"))
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.FINAL)
                .addField(brewFiled())
                .addMethod(brewMethod_transArgs2ClassArray())
                .addMethod(brewMethod_invokePrivate())
                .addMethod(brewMethod_invokePublic())
                .addMethod(brewMethod_invoke())
                .addMethod(brewMethod_mode())
                .addMethod(brewMethod_isRetrunTypeFuture())
                .addMethod(brewMethod_checkSuperClassAndInterface())
                .addMethod(brewMethod_checkInterface())
                .addMethod(brewMethod_isSameType())
                .addMethod(brewMethod_isPrimitive())
                .addMethod(brewMethod_checkPrimitive())
                .addMethod(brewMethod_getRawType())
                .addMethod(brewMethod_obj2class())
                .addMethod(brewMethod_getPrimitiveType())
                .addMethod(brewMethod_isGenericType());
        CodeBlock.Builder staticCodeBlockBuilder = CodeBlock.builder();
        Switch annotation = null;
        for (ExecutableElement element : mMethodCollections) {
            annotation = element.getAnnotation(Switch.class);
            // methodDescName.put("desc", new MethodBasic("name", ThreadMode.POST, true, true));
            if ("".equals(annotation.alias()))
                continue;
            StringBuilder paramsTypeBuilder = new StringBuilder();
            List<? extends VariableElement> varElements = element.getParameters();
            int len = varElements.size();
            for (int i = 0; i < len; ++i) {
                paramsTypeBuilder.append("\"");
                paramsTypeBuilder.append(varElements.get(i).asType().toString());
                paramsTypeBuilder.append("\"");
                paramsTypeBuilder.append(",");
            }
            if (paramsTypeBuilder.length() > 0)
                paramsTypeBuilder.deleteCharAt(paramsTypeBuilder.length() - 1);

            staticCodeBlockBuilder.addStatement(" methodDescName.put($S, " +
                            "new $T($S, $T.$L, $L,$L, $L,$L,$L))",
                    annotation.alias(),
                    ClassName.get("com.yn.processor", "MethodBasic"),
                    element.getSimpleName(),
                    ClassName.get("com.yn.enums", "ThreadMode"),
                    annotation.threadMode(),
                    element.getModifiers().contains(Modifier.PUBLIC),
                    element.getReturnType().getKind() != TypeKind.VOID,
                    element.getReturnType().toString().contains("java.util.concurrent.Future"),
                    paramsTypeBuilder.toString().isEmpty() ? "null" : String.format("new String[]{%s}", paramsTypeBuilder.toString()),
                    element.getModifiers().contains(Modifier.STATIC)
            );
        }
        builder.addStaticBlock(staticCodeBlockBuilder.build());
        return builder.build();
    }

//    private boolean isGenericType(final String type) {
//        Pattern pattern = Pattern.compile("(\\S+)\\<\\S*\\>");
//        return pattern.matcher(type).matches();
//    }

    private MethodSpec brewMethod_isGenericType() {
        return MethodSpec.methodBuilder("isGenericType")
                .addModifiers(Modifier.PRIVATE)
                .returns(boolean.class)
                .addParameter(ClassName.get(String.class), "type", Modifier.FINAL)
                .addStatement("$T pattern = Pattern.compile(\"(\\\\S+)\\\\<\\\\S*\\\\>\")",
                        ClassName.get(Pattern.class))
                .addStatement("return pattern.matcher(type).matches()")
                .build();
    }

    private Class<?> getPrimitiveType(final String type) throws ClassNotFoundException {
        switch (type) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
        }
        return Class.forName(type);
    }

    private MethodSpec brewMethod_getPrimitiveType() {
        TypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
        ClassName cls = ClassName.get(Class.class);
        TypeName clsWildcard = ParameterizedTypeName.get(cls, wildcard);
        return MethodSpec.methodBuilder("getPrimitiveType")
                .addModifiers(Modifier.PRIVATE)
                .returns(clsWildcard)
                .addParameter(ClassName.get(String.class), "type", Modifier.FINAL)
                .addException(ClassName.get(ClassNotFoundException.class))
                .beginControlFlow("switch (type)")
                .beginControlFlow("case $S:", boolean.class.getSimpleName())
                .addStatement("return $L.class", boolean.class)
                .endControlFlow()
                .beginControlFlow("case $S:", byte.class.getSimpleName())
                .addStatement("return $L.class", byte.class)
                .endControlFlow()
                .beginControlFlow("case $S:", char.class.getSimpleName())
                .addStatement("return $L.class", char.class)
                .endControlFlow()
                .beginControlFlow("case $S:", short.class.getSimpleName())
                .addStatement("return $L.class", short.class)
                .endControlFlow()
                .beginControlFlow("case $S:", int.class.getSimpleName())
                .addStatement("return $L.class", int.class)
                .endControlFlow()
                .beginControlFlow("case $S:", long.class.getSimpleName())
                .addStatement("return $L.class", long.class)
                .endControlFlow()
                .beginControlFlow("case $S:", float.class.getSimpleName())
                .addStatement("return $L.class", float.class)
                .endControlFlow()
                .beginControlFlow("case $S:", double.class.getSimpleName())
                .addStatement("return $L.class", double.class)
                .endControlFlow()
                .endControlFlow()
                .addStatement("return Class.forName(type)")
                .build();
    }

//    private Class<?>[] obj2class(MethodBasic methodBasic, Object[] args) throws
//            ClassNotFoundException {
//        int argsLen = args != null ? args.length : 0;
//        Class<?>[] argsArray = new Class[argsLen];
//        for (int i = 0; i < argsLen; ++i) {
//            if (isPrimitive(methodBasic.paramsType[i])) {
//                argsArray[i] = getPrimitiveType(methodBasic.paramsType[i]);
//            } else if (isGenericType(methodBasic.paramsType[i])) {
//                argsArray[i] = Class.forName(getRawType(methodBasic.paramsType[i]));
//            } else {
//                argsArray[i] = args[i].getClass();
//            }
//        }
//        return argsArray;
//    }

    private MethodSpec brewMethod_obj2class() {
        TypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
        ClassName cls = ClassName.get(Class.class);
        TypeName clsWildcard = ParameterizedTypeName.get(cls, wildcard);
        return MethodSpec.methodBuilder("obj2class")
                .addModifiers(Modifier.PRIVATE)
                .returns(ArrayTypeName.of(clsWildcard))
                .addParameter(ClassName.get("com.yn.processor", "MethodBasic"), "methodBasic")
                .addParameter(ArrayTypeName.of(ClassName.get(Object.class)), "args")
                .addException(ClassName.get(ClassNotFoundException.class))
                .addStatement("int argsLen = args != null ? args.length : 0")
                .addStatement("Class<?>[] argsArray = new Class[argsLen]")
                .beginControlFlow("for (int i = 0; i < argsLen; ++i) ")
                .beginControlFlow("if (isPrimitive(methodBasic.paramsType[i]))")
                .addStatement("argsArray[i] = getPrimitiveType(methodBasic.paramsType[i])")
                .endControlFlow()
                .beginControlFlow("else if (isGenericType(methodBasic.paramsType[i]))")
                .addStatement("argsArray[i] = $T.forName(getRawType(methodBasic.paramsType[i]))",
                        ClassName.get(Class.class))
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("argsArray[i] = args[i].getClass()")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return argsArray")
                .build();
    }


    private String getRawType(String type) {
        Pattern pattern = Pattern.compile("(\\S+)\\<\\S*\\>");
        Matcher matcher = pattern.matcher(type);
        return matcher.find() ? matcher.group(1) : type;
    }

    private MethodSpec brewMethod_getRawType() {
        return MethodSpec.methodBuilder("getRawType")
                .addModifiers(Modifier.PRIVATE)
                .returns(String.class)
                .addParameter(ClassName.get(String.class), "type")
                .addStatement("$T pattern = Pattern.compile(\"(\\\\S+)\\\\<\\\\S*\\\\>\")",
                        ClassName.get(Pattern.class))
                .addStatement("$T matcher = pattern.matcher(type)", ClassName.get(Matcher.class))
                .addStatement("return matcher.find() ? matcher.group(1) : type")
                .build();
    }


    //    boolean, byte, char, short, int, long, float,  double
//    private boolean checkPrimitive(Object srcType, String destType) {
//        Class<?> srcTypeClass = srcType.getClass();
//        if("boolean".equals(destType))
//        {
//            return isSameType(srcType, "java.lang.Boolean");
//        }else if("byte".equals(destType)){
//            return isSameType(srcType, "java.lang.Byte");
//        }else if("char".equals(destType)){
//            return isSameType(srcType, "java.lang.Character");
//        }else if("short".equals(destType)){
//            return isSameType(srcType, "java.lang.Short");
//        }else if("int".equals(destType)){
//            return isSameType(srcType, "java.lang.Integer");
//        }else if("long".equals(destType)){
//            return isSameType(srcType, "java.lang.Long");
//        }else if("float".equals(destType)){
//            return isSameType(srcType, "java.lang.Float");
//        }else if("double".equals(destType)){
//            return isSameType(srcType, "java.lang.Double");
//        }
//        return false;
//    }
    private MethodSpec brewMethod_checkPrimitive() {
        return MethodSpec.methodBuilder("checkPrimitive")
                .addModifiers(Modifier.PRIVATE)
                .returns(boolean.class)
                .addParameter(ClassName.get(Object.class), "srcType")
                .addParameter(ClassName.get(String.class), "destType")
                .beginControlFlow("if($S.equals(destType))", boolean.class)
                .addStatement("return isSameType(srcType, $S)", Boolean.class.getCanonicalName())
                .endControlFlow()
                .beginControlFlow("else if($S.equals(destType))", byte.class)
                .addStatement("return isSameType(srcType, $S)", Byte.class.getCanonicalName())
                .endControlFlow()
                .beginControlFlow("else if($S.equals(destType))", char.class)
                .addStatement("return isSameType(srcType, $S)", Character.class.getCanonicalName())
                .endControlFlow()
                .beginControlFlow("else if($S.equals(destType))", short.class)
                .addStatement("return isSameType(srcType, $S)", Short.class.getCanonicalName())
                .endControlFlow()
                .beginControlFlow("else if($S.equals(destType))", int.class)
                .addStatement("return isSameType(srcType, $S)", Integer.class.getCanonicalName())
                .endControlFlow()
                .beginControlFlow("else if($S.equals(destType))", long.class)
                .addStatement("return isSameType(srcType, $S)", Long.class.getCanonicalName())
                .endControlFlow()
                .beginControlFlow("else if($S.equals(destType))", float.class)
                .addStatement("return isSameType(srcType, $S)", Float.class.getCanonicalName())
                .endControlFlow()
                .beginControlFlow("else if($S.equals(destType))", double.class)
                .addStatement("return isSameType(srcType, $S)", Double.class.getCanonicalName())
                .endControlFlow()
                .addStatement("return false")
                .build();
    }

//    private boolean isPrimitive(final String type) {
//        return "boolean".equals(type)
//                || "byte".equals(type)
//                || "char".equals(type)
//                || "short".equals(type)
//                || "int".equals(type)
//                || "long".equals(type)
//                || "float".equals(type)
//                || "double".equals(type);
//    }

    private MethodSpec brewMethod_isPrimitive() {
        return MethodSpec.methodBuilder("isPrimitive")
                .addModifiers(Modifier.PRIVATE)
                .returns(boolean.class)
                .addParameter(ClassName.get(String.class), "type", Modifier.FINAL)
                .addStatement("    return \"boolean\".equals(type)\n" +
                        "                || \"byte\".equals(type)\n" +
                        "                || \"char\".equals(type)\n" +
                        "                || \"short\".equals(type)\n" +
                        "                || \"int\".equals(type)\n" +
                        "                || \"long\".equals(type)\n" +
                        "                || \"float\".equals(type)\n" +
                        "                || \"double\".equals(type)")
                .build();
    }

    //        private boolean checkSuperClassAndInterface(Object srcType, String destType) {
//        Class<?> srcTypeClass = srcType.getClass();
//        String destRawType = getRawType(destType);
//        while (srcTypeClass != null) {
//            System.out.println("srcType = "+srcTypeClass.getCanonicalName());
//            if (srcTypeClass.getCanonicalName().equals(destRawType)) {
//                return true;
//            }
//            if(checkInterface(srcTypeClass, destRawType))
//                return true;
//            srcTypeClass = srcTypeClass.getSuperclass();
//        }
//        return false;
//    }
    private MethodSpec brewMethod_checkSuperClassAndInterface() {
        return MethodSpec.methodBuilder("checkSuperClassAndInterface")
                .addModifiers(Modifier.PRIVATE)
                .returns(boolean.class)
                .addParameter(ClassName.get(Object.class), "srcType")
                .addParameter(ClassName.get(String.class), "destType")
                .addStatement("Class<?> srcTypeClass = srcType.getClass()")
                .addStatement("String destRawType = getRawType(destType)")
                .beginControlFlow("while (srcTypeClass != null)")
                .beginControlFlow("if (srcTypeClass.getCanonicalName().equals(destRawType))")
                .addStatement("return true")
                .endControlFlow()
                .beginControlFlow("if(checkInterface(srcTypeClass, destRawType))")
                .addStatement("return true")
                .endControlFlow()
                .addStatement("srcTypeClass = srcTypeClass.getSuperclass()")
                .endControlFlow()
                .addStatement("return false")
                .build();
    }

    //    private boolean checkInterface(Class<?> srcTypeClass, String destType) {
//        String destRawType = getRawType(destType);
//        for (Class<?> clz : srcTypeClass.getInterfaces()) {
//            System.out.println("superInterfaceType:" + clz.getCanonicalName());
//            if (clz.getCanonicalName().equals(destRawType)) {
//                return true;
//            }
//        }
//        return false;
//    }
    private MethodSpec brewMethod_checkInterface() {
        TypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
        ClassName cls = ClassName.get(Class.class);
        TypeName clsWildcard = ParameterizedTypeName.get(cls, wildcard);
        return MethodSpec.methodBuilder("checkInterface")
                .addModifiers(Modifier.PRIVATE)
                .returns(boolean.class)
                .addParameter(clsWildcard, "srcTypeClass")
                .addParameter(ClassName.get(String.class), "destType")
                .addStatement("String destRawType = getRawType(destType)")
                .beginControlFlow(" for (Class<?> clz : srcTypeClass.getInterfaces())")
                .beginControlFlow("if (clz.getCanonicalName().equals(destRawType))")
                .addStatement("return true")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return false")
                .build();
    }

    //    private boolean isSameType(Object srcType, String destType) {
//        if (isPrimitive(destType)) {
//            if (checkPrimitive(srcType, destType)) {
//                return true;
//            }
//        }
//        return checkSuperClassAndInterface(srcType,destType);
//    }
    private MethodSpec brewMethod_isSameType() {
        return MethodSpec.methodBuilder("isSameType")
                .addModifiers(Modifier.PRIVATE)
                .returns(boolean.class)
                .addParameter(ClassName.get(Object.class), "srcType")
                .addParameter(ClassName.get(String.class), "destType")
                .beginControlFlow("if (isPrimitive(destType))")
                .beginControlFlow("if (checkPrimitive(srcType, destType))")
                .addStatement("return true")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return checkSuperClassAndInterface(srcType,destType)")
                .build();
    }


    //    boolean isRetrunTypeFuture(String desc){
//        MethodBasic methodBasic = methodDescName.get(desc);
//        return methodBasic == null ? false: methodBasic.isReturnTypeFuture;
//    }
    private MethodSpec brewMethod_isRetrunTypeFuture() {
        return MethodSpec.methodBuilder("isRetrunTypeFuture")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(ClassName.get(String.class), "desc")
                .addStatement("$T methodBasic = methodDescName.get(desc)",
                        ClassName.get("com.yn.processor", "MethodBasic"))
                .addStatement("return methodBasic == null ? false: methodBasic.isReturnTypeFuture")
                .build();
    }

    //    @Override
//    public ThreadMode mode(String desc) {
//        MethodBasic methodBasic = methodDescName.get(desc);
//        return methodBasic == null ? ThreadMode.POST : methodBasic.mode;
//    }
    private MethodSpec brewMethod_mode() {
        return MethodSpec.methodBuilder("mode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("com.yn.enums", "ThreadMode"))
                .addParameter(ClassName.get(String.class), "desc")
                .addStatement("$T methodBasic = methodDescName.get(desc)",
                        ClassName.get("com.yn.processor", "MethodBasic"))
                .addStatement("return methodBasic == null ? $N : methodBasic.mode", "ThreadMode.POST")
                .build();
    }

    //    public <T> Object invoke(String desc,T target, Object[] args) throws Exception {
//        MethodBasic methodBasic = methodDescName.get(desc);
//        if (methodBasic != null) {
//            if (methodBasic.isPublic)
//                return invokePublic(methodBasic,target, args);//getMethod
//            return invokePrivate(methodBasic,target, args);//getDeclaredmethod
//        }
//        return null;
//    }
    private MethodSpec brewMethod_invoke() {
        return MethodSpec.methodBuilder("invoke")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(Object.class))
                .addTypeVariable(TypeVariableName.get("T"))
                .addParameter(ClassName.get(String.class), "desc")
                .addParameter(TypeVariableName.get("T"), "target")
                .addParameter(ArrayTypeName.of(ClassName.get(Object.class)), "args")
                .addException(ClassName.get(Exception.class))
                .addStatement("$T methodBasic = methodDescName.get(desc)",
                        ClassName.get("com.yn.processor", "MethodBasic"))
                .beginControlFlow("if (methodBasic != null)")
                .beginControlFlow("if (methodBasic.isPublic)")
                .addStatement("return invokePublic(methodBasic, target, args)")
                .endControlFlow()
                .addStatement("return invokePrivate(methodBasic, target, args)")
                .endControlFlow()
                .addStatement("return null")
                .build();
    }


    //    private <T> Object invokePublic(MethodBasic methodBasic, T target, Object[] args) {
//        if (methodBasic.hasReturn) {
//            if(args != null)
//            {
//                if(target instanceof EnclosingElement.fullname){
//                    return ((EnclosingElement.fullname) target). (element.simpleName) (args);
//                }else if(target instanceof EnclosingElement.nextfullname)
//                {
//                    return ((EnclosingElement.nextfullname) target). (element.simpleName) (args);
//                }
//            }else{
//                if(target instanceof EnclosingElement.fullname){
//                    return ((EnclosingElement.fullname) target). (element.simpleName) ();
//                }
//            }
//        }
//        if(args != null)
//        {
//            if(target instanceof EnclosingElement.fullname){
//                ((EnclosingElement.fullname) target). (element.simpleName) (args);
//            }else if(target instanceof EnclosingElement.nextfullname)
//            {
//                ((EnclosingElement.nextfullname) target). (element.simpleName) (args);
//            }
//        }else{
//            if(target instanceof EnclosingElement.fullname){
//                ((EnclosingElement.fullname) target). (element.simpleName) ();
//            }
//        }
//        return null;
//    }
    private MethodSpec brewMethod_invokePublic() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("invokePublic")
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PRIVATE)
                .returns(ClassName.get(Object.class))
                .addParameter(ClassName.get("com.yn.processor", "MethodBasic"), "methodBasic")
                .addParameter(TypeVariableName.get("T"), "target")
                .addParameter(ArrayTypeName.of(ClassName.get(Object.class)), "args")
                .addCode(brewInstanceCodeBlock())
                .addStatement("throw new IllegalArgumentException(String.format(\"unable to" +
                        " execute method.argument desire type %s,receive type %s. " +
                        "or did you pass the wrong target object.\", " +
                        "$T.toString(methodBasic.paramsType), " +
                        "Arrays.toString(transArgs2ClassArray(args))))", ClassName.get(Arrays.class));
        return builder.build();
    }

    //    boolean isSame = false;
//        if (target instanceof ITestSecond) {
//        do {
//            if (!methodBasic.name.equals("test")) {
//                isSame = false;
//                break;
//            }
//            if (args == null && methodBasic.paramsType == null) {
//                isSame = true;
//                break;
//            }
//            if (args != null && methodBasic.paramsType != null && args.length == methodBasic.paramsType.length) {
//                for (int i = 0; i < args.length; ++i) {
//                    if (!args[i].getClass().getCanonicalName().contains(methodBasic.paramsType[i])) {
//                        isSame = false;
//                        break;
//                    }
//                }
//            }
//            isSame = true;
//        } while (false);
//        if (isSame) {
//            if (execuable.getReturnType().kind != void)
//            return ((ITestSecond) target).test((execable.parm1.fullname) args[0]);
//            ((ITestSecond) target).test(args);
//            return null;
//        }
//    }
    private CodeBlock brewInstanceCodeBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.addStatement("boolean isSame = false");
        for (ExecutableElement element : mMethodCollections) {
            if (!element.getModifiers().contains(Modifier.PUBLIC))
                continue;
            StringBuilder returnParmsBuilder = new StringBuilder();
            if (element.getReturnType().getKind() != TypeKind.VOID) {
                returnParmsBuilder.append("return ");
            }
//            ((com.yn.threaddispatcherdemo.MethodAliasUsageActivity) target).publicMethod((int) args[0]);
//            com.yn.threaddispatcherdemo.MethodAliasUsageActivity.publicMethod((int) args[0]);
            String castPart = element.getModifiers().contains(Modifier.STATIC) ? "%s" : "((%s) target)";
            returnParmsBuilder.append(String.format(castPart + ".%s(",
                    element.getEnclosingElement().asType().toString(), element.getSimpleName()));
            List<? extends VariableElement> varElements = element.getParameters();
            int len = varElements.size();
            for (int i = 0; i < len; ++i) {
                returnParmsBuilder.append(String.format("(%s)args[%d],",
                        getRawType(varElements.get(i).asType().toString()), i));
            }
            if (len > 0)
                returnParmsBuilder.deleteCharAt(returnParmsBuilder.length() - 1);
            returnParmsBuilder.append(")");
            builder.beginControlFlow("if(target instanceof $T)",
                    element.getModifiers().contains(Modifier.STATIC) ?
                            ClassName.get(Class.class) : element.getEnclosingElement())
                    .beginControlFlow("do")
                    .beginControlFlow("if(!methodBasic.name.equals($S))", element.getSimpleName())
                    .addStatement("isSame = false")
                    .addStatement("break")
                    .endControlFlow()
                    .beginControlFlow("if (methodBasic.paramsType == null && (args.length == 0 || args == null))")
                    .addStatement("isSame = true")
                    .addStatement("break")
                    .endControlFlow()
                    .beginControlFlow("if (args != null && methodBasic.paramsType != null && args.length == methodBasic.paramsType.length)")
                    .beginControlFlow("for (int i = 0; i < args.length; ++i)")
                    .beginControlFlow("if(!isSameType(args[i],methodBasic.paramsType[i]))")
                    .addStatement("isSame = false")
                    .addStatement("break")
                    .endControlFlow()
                    .addStatement("isSame = true")
                    .endControlFlow()
                    .endControlFlow()
                    .endControlFlow("while(false)")
                    .beginControlFlow(" if (isSame)")
                    .add(brewReturnCodeBlock(element, returnParmsBuilder.toString()))
                    .endControlFlow()
                    .endControlFlow();
        }
        return builder.build();
    }

    private CodeBlock brewReturnCodeBlock(ExecutableElement element, String s) {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.addStatement(s);
        if (element.getReturnType().getKind() == TypeKind.VOID)
            builder.addStatement("return null");
        return builder.build();
    }

    //    if(args != null)
//    {
//        if(target instanceof EnclosingElement.fullname){
//            ((EnclosingElement.fullname) target). (element.simpleName) (args);
//        }else if(target instanceof EnclosingElement.nextfullname)
//        {
//            ((EnclosingElement.nextfullname) target). (element.simpleName) (args);
//        }
//    }else{
//        if(target instanceof EnclosingElement.fullname){
//            ((EnclosingElement.fullname) target). (element.simpleName) ();
//        }
//    }
    private CodeBlock brewNoReturnCodeBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.beginControlFlow("if(args != null)");
        int pos = 0;
        for (ExecutableElement element : mMethodCollections) {
            if (pos == 0) {
                builder.beginControlFlow("if(target instanceof $T)",
                        ClassName.get(element.getEnclosingElement().asType()))
                        .addStatement("(($T) target).$L(args)",
                                ClassName.get(element.getEnclosingElement().asType()),
                                element.getSimpleName())
                        .endControlFlow();
            } else {
                builder.beginControlFlow("else if(target instanceof $T)",
                        ClassName.get(element.getEnclosingElement().asType()))
                        .addStatement("(($T) target).$L(args)",
                                ClassName.get(element.getEnclosingElement().asType()),
                                element.getSimpleName())
                        .endControlFlow();
            }
            ++pos;
        }
        builder.endControlFlow();
        builder.beginControlFlow("else");
        pos = 0;
        for (ExecutableElement element : mMethodCollections) {
            if (pos == 0) {
                builder.beginControlFlow("if(target instanceof $T)",
                        ClassName.get(element.getEnclosingElement().asType()))
                        .addStatement("(($T) target).$L()",
                                ClassName.get(element.getEnclosingElement().asType()),
                                element.getSimpleName())
                        .endControlFlow();
            } else {
                builder.beginControlFlow("else if(target instanceof $T)",
                        ClassName.get(element.getEnclosingElement().asType()))
                        .addStatement("(($T) target).$L()",
                                ClassName.get(element.getEnclosingElement().asType()),
                                element.getSimpleName())
                        .endControlFlow();
            }
            ++pos;
        }
        builder.endControlFlow();
        return builder.build();
    }

    // if(args != null)
//    {
//        if(target instanceof EnclosingElement.fullname){
//            return ((EnclosingElement.fullname) target). (element.simpleName) (args);
//        }else if(target instanceof EnclosingElement.nextfullname)
//        {
//            return ((EnclosingElement.nextfullname) target). (element.simpleName) (args);
//        }
//    }else{
//        if(target instanceof EnclosingElement.fullname){
//            return ((EnclosingElement.fullname) target). (element.simpleName) ();
//        }
//    }
    private CodeBlock brewReturnCodeBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.beginControlFlow("if(args != null)");
        int pos = 0;
        for (ExecutableElement element : mMethodCollections) {
            if (pos == 0) {
                builder.beginControlFlow("if(target instanceof $T)",
                        ClassName.get(element.getEnclosingElement().asType()))
                        .addStatement(" return (($T) target).$L(args)",
                                ClassName.get(element.getEnclosingElement().asType()),
                                element.getSimpleName())
                        .endControlFlow();
            } else {
                builder.beginControlFlow("else if(target instanceof $T)",
                        ClassName.get(element.getEnclosingElement().asType()))
                        .addStatement(" return (($T) target).$L(args)",
                                ClassName.get(element.getEnclosingElement().asType()),
                                element.getSimpleName())
                        .endControlFlow();
            }
            ++pos;
        }
        builder.endControlFlow();

        //else
        builder.beginControlFlow("else");
        pos = 0;
        for (ExecutableElement element : mMethodCollections) {
            if (pos == 0) {
                builder.beginControlFlow("if(target instanceof $T)",
                        ClassName.get(element.getEnclosingElement().asType()))
                        .addStatement(" return (($T) target).$L()",
                                ClassName.get(element.getEnclosingElement().asType()),
                                element.getSimpleName())
                        .endControlFlow();
            } else {
                builder.beginControlFlow("else if(target instanceof $T)",
                        ClassName.get(element.getEnclosingElement().asType()))
                        .addStatement(" return (($T) target).$L()",
                                ClassName.get(element.getEnclosingElement().asType()),
                                element.getSimpleName())
                        .endControlFlow();
            }
            ++pos;
        }
        builder.endControlFlow();
        return builder.build();
    }


//    private <T> Object invokePrivate(MethodBasic methodBasic, T target, Object[] args) throws
//            Exception {
//        Method method = null;
//        if (target instanceof Class)
//            method = ((Class<?>) target).getDeclaredMethod(methodBasic.name, obj2class(methodBasic, args));
//        else
//            method = target.getClass().getDeclaredMethod(methodBasic.name, obj2class(methodBasic, args));
//        method.setAccessible(true);
//        if (methodBasic.hasReturn) {
//            return method.invoke(target, args);
//        }
//        method.invoke(target, args);
//        return null;
//    }

    private MethodSpec brewMethod_invokePrivate() {
        return MethodSpec.methodBuilder("invokePrivate")
                .addTypeVariable(TypeVariableName.get("T"))
                .addModifiers(Modifier.PRIVATE)
                .returns(ClassName.get(Object.class))
                .addParameter(ClassName.get("com.yn.processor", "MethodBasic"), "methodBasic")
                .addParameter(TypeVariableName.get("T"), "target")
                .addParameter(ArrayTypeName.of(ClassName.get(Object.class)), "args")
                .addException(ClassName.get(Exception.class))
//                .addStatement("$T method = target.getClass().getDeclaredMethod(" +
//                        "methodBasic.name, obj2class(methodBasic, args))", ClassName.get(Method.class))
                .addStatement("$T method = null", ClassName.get(Method.class))
                .beginControlFlow("if (target instanceof Class)")
                .addStatement("method = ((Class<?>) target).getDeclaredMethod(methodBasic.name, obj2class(methodBasic, args))")
                .endControlFlow()
                .beginControlFlow("else")
                .addStatement("method = target.getClass().getDeclaredMethod(methodBasic.name, obj2class(methodBasic, args))")
                .endControlFlow()
                .addStatement("method.setAccessible(true)")
                .beginControlFlow("if (methodBasic.hasReturn)")
                .addStatement("return method.invoke(methodBasic.isStatic ? null : target, args)")
                .endControlFlow()
                .addStatement("method.invoke(methodBasic.isStatic ? null : target, args)")
                .addStatement("return null")
                .build();
    }


    //
//    private Class<?>[] transArgs2ClassArray(Object[] args) {
//        int argsLen = args != null ? args.length : 0;
//        Class<?>[] argsArray = new Class[argsLen];
//        for (int i = 0; i < argsLen; ++i) {
//            argsArray[i] = args[i].getClass();
//        }
//        return argsArray;
//    }
    private MethodSpec brewMethod_transArgs2ClassArray() {
        TypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
        ClassName cls = ClassName.get(Class.class);
        TypeName clsWildcard = ParameterizedTypeName.get(cls, wildcard);
        return MethodSpec.methodBuilder("transArgs2ClassArray")
                .addModifiers(Modifier.PRIVATE)
//                .returns(ArrayTypeName.of(ClassName.get(Class.class)))//Class[]
                .returns(ArrayTypeName.of(clsWildcard))
                .addParameter(ArrayTypeName.of(ClassName.get(Object.class)), "args")
                .addStatement("int argsLen = args != null ? args.length : 0")
                .addStatement("Class<?>[] argsArray = new Class[argsLen]")
                .beginControlFlow("for (int i = 0; i < argsLen; ++i)")
                .addStatement("argsArray[i] = args[i].getClass()")
                .endControlFlow()
                .addStatement(" return argsArray")
                .build();
    }

    private FieldSpec brewFiled() {
        //private Map<String,MethodBasic> methodDescName = new HashMap<>();
        ClassName string = ClassName.get(String.class);
        ClassName methodBasic = ClassName.get("com.yn.processor", "MethodBasic");
        ClassName map = ClassName.get("java.util", "Map");
        ClassName conhashmap = ClassName.get("java.util.concurrent", "ConcurrentHashMap");
        TypeName mapSS = ParameterizedTypeName.get(map, string, methodBasic);
        return FieldSpec.builder(mapSS, "methodDescName", Modifier.PRIVATE, Modifier.FINAL)
                .addModifiers(Modifier.STATIC)
                .initializer("new $T<>()", conhashmap)
                .build();
    }

    private void note(String msg) {
        mMessager.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    private void error(String msg) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, msg);
    }
}
