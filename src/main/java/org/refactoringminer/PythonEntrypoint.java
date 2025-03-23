package org.refactoringminer;

import org.apache.commons.io.FileUtils;
import org.graalvm.polyglot.*;

import java.io.IOException;

public class PythonEntrypoint {
    public static String useMe() {
        return "Hello, World! Used. Yes, thank you.";
    }
    public static void main(String[] args) {
        try (var context = Context.newBuilder().allowAllAccess(true).build()) {
            context.eval(Source.newBuilder("python", FileUtils.getFile("src/main/resources/python/transpiler.py")).build());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}







//            context.eval("python", """
//import java
//App = java.type("org.refactoringminer.App")
//print(App.useMe())
//            """);