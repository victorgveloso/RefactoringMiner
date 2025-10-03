package org.refactoringminer.util;

public class PathFileUtils {
    public static boolean isSupportedFile(String path){
        return path.endsWith(".java") || path.endsWith(".py");
    }

    public static boolean isJavaFile(String path){
        return path.endsWith(".java");
    }

    public static boolean isPythonFile(String path){
        return path.endsWith(".py");
    }

    public static boolean isLangSupportedFile(String path){
        // Add new languages in the future
        return isPythonFile(path);
    }
}
