package org.refactoringminer;

import gr.uom.java.xmi.UMLModel;
import org.graalvm.polyglot.*;

import java.io.IOException;
import java.nio.file.Path;

class PyRMiner {
    public static void main(String[] args) throws Exception {

        try (var context = Context.newBuilder().allowAllAccess(true).build()) {
            var py = context.parse(Source.newBuilder("python", Path.of(System.getProperty("user.dir") + "/src/main/python/pyminer/astreader.py").toFile()).build());
            py.execute();
            // public LocationInfo(String sourceFolder, String filePath, int startOffset, int endOffset, int length, int startLine, int startColumn, int endLine, int endColumn, int compilationUnitLength, CodeElementType codeElementType)
            var linfo = context.eval("python", "populate_file_contents('/Users/victor/IdeaProjects/RefactoringMiner/src/main/resources/python/example/before')").as(UMLModel.class);
            assert linfo != null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

