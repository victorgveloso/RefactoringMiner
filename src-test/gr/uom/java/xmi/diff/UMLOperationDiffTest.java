package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.CompilationUnitMother;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Map;

@RunWith(Enclosed.class)
public class UMLOperationDiffTest {
    static private final String SOURCE_BEFORE = "import org.junit.Test;" +
            "class ExampleTest {" +
            "@Test(expected = RuntimeException.class)" +
            "void example() {" +
            "(new Example()).method();" +
            "}" +
            "}";
    static private final String SOURCE_AFTER = "import org.junit.Test;" +
            "class ExampleTest {" +
            "void testExample() {" +
            "try {" +
            "(new Example()).method();" +
            "Assert.fail();" +
            "} catch (RuntimeException ignored) {}" +
            "}" +
            "}";

    public static class NullOpsTest {
        @Test(expected = NullPointerException.class)
        public void nullInit() {

            UMLOperationDiff sut = new UMLOperationDiff(null, null);
        }
    }

    public static class ExploringTest {
        CompilationUnit cu;
        @Before
        public void setUp() {
            cu = CompilationUnitMother.createFromSource(SOURCE_BEFORE.toCharArray());
        }
        @Test
        public void testUnitIsNotNull() {
            Assert.assertNotNull(cu);
        }
        @Test
        public void testTypeDeclarationsCount() {
            var types = cu.types();
            Assert.assertEquals(1, types.size());
        }
        @Test
        public void testMethodDeclarationsCount() {
            var types = cu.types();
            var type = (TypeDeclaration) types.get(0);
            Assert.assertEquals(1, type.getMethods().length);
        }
        @Test
        public void testMethodName() {
            var types = cu.types();
            var type = (TypeDeclaration) types.get(0);
            var method = type.getMethods()[0];
            Assert.assertEquals("example", method.getName().getIdentifier());
        }
        @Test
        public void testUMLOperationInit() {
            var types = cu.types();
            var type = (TypeDeclaration) types.get(0);
            var method = type.getMethods()[0];
            var loc = new LocationInfo(cu, "", method, LocationInfo.CodeElementType.METHOD_DECLARATION);
            var op = new UMLOperation("example", loc);
            Assert.assertNotNull(op);
        }
        @Test
        public void testAnnotationsCount_DirectInit() {
            var types = cu.types();
            var type = (TypeDeclaration) types.get(0);
            var method = type.getMethods()[0];
            var loc = new LocationInfo(cu, "", method, LocationInfo.CodeElementType.METHOD_DECLARATION);
            var op = new UMLOperation("example", loc);
            var annotations = op.getAnnotations();
            Assert.assertEquals("Annotation found without UMLModelASTReader processing", 0, annotations.size());
        }
        @Test
        public void testAnnotationsCount_WithReader() {
            var op = parseAndFindFirstMethod("SOURCE_BEFORE", SOURCE_BEFORE);
            var annotations = op.getAnnotations();
            Assert.assertEquals(1, annotations.size());
        }
        @Test
        public void testAnnotationIdentifier() {
            var op = parseAndFindFirstMethod("SOURCE_BEFORE", SOURCE_BEFORE);
            var annotations = op.getAnnotations();
            var annotation = annotations.get(0);
            Assert.assertEquals("Test", annotation.getTypeName());
        }
        @Test
        public void testIsNormalAnnotation() {
            var op = parseAndFindFirstMethod("SOURCE_BEFORE", SOURCE_BEFORE);
            var annotations = op.getAnnotations();
            var annotation = annotations.get(0);
            Assert.assertTrue(annotation.isNormalAnnotation());
        }
        @Test
        public void testMemberValuePairsCount() {
            var op = parseAndFindFirstMethod("SOURCE_BEFORE", SOURCE_BEFORE);
            var annotations = op.getAnnotations();
            var annotation = annotations.get(0);
            var pairs = annotation.getMemberValuePairs();
            Assert.assertEquals(1, pairs.size());
        }
        @Test
        public void testMemberValuePairKey() {
            var op = parseAndFindFirstMethod("SOURCE_BEFORE", SOURCE_BEFORE);
            var annotations = op.getAnnotations();
            var annotation = annotations.get(0);
            var key = annotation.getMemberValuePairs().keySet().iterator().next();
            Assert.assertEquals("expected", key);
        }
        @Test
        public void testMemberValuePairValue() {
            var op = parseAndFindFirstMethod("SOURCE_BEFORE", SOURCE_BEFORE);
            var annotations = op.getAnnotations();
            var annotation = annotations.get(0);
            var value = annotation.getMemberValuePairs().values().iterator().next();
            Assert.assertEquals("RuntimeException.class", value.getTypeLiterals().get(0));
        }
    }

    public static class ValidTest {
        UMLOperationDiff sut;
        @Before
        public void setUp() {
            var removedOp = parseAndFindFirstMethod("before.java", SOURCE_BEFORE);
            var addedOp = parseAndFindFirstMethod("after.java", SOURCE_AFTER);

            sut = new UMLOperationDiff(removedOp, addedOp);
        }

        @Test
        public void isEmpty() {
            Assert.assertFalse(sut.isEmpty());
        }

        @Test
        public void testToString() {
            Assert.assertEquals("\tpackage example() : void\n" +
                    "\trenamed from example to testExample\n" +
                    "\tannotation @Test(expected = RuntimeException.class) removed\n", sut.toString());
        }

        @Test
        public void getRefactorings() {
            Assert.assertFalse(sut.getRefactorings().isEmpty());
        }

        @Test
        public void isOperationRenamed() {
            Assert.assertTrue(sut.isOperationRenamed());
        }

        @Test
        public void getAddedParameters() {
            Assert.assertTrue(sut.getAddedParameters().isEmpty());
        }

        @Test
        public void getRemovedParameters() {
            Assert.assertTrue(sut.getRemovedParameters().isEmpty());
        }

        @Test
        public void getParameterDiffList() {
            Assert.assertTrue(sut.getParameterDiffList().isEmpty());
        }
    }

    public static class NullLocTest {
        UMLOperationDiff sut;
        @Before
        public void setUp() {
            var removedOp = new UMLOperation("example", null);
            removedOp.setVisibility("package");

            var addedOp = new UMLOperation("testExample", null);
            addedOp.setVisibility("package");

            sut = new UMLOperationDiff(removedOp, addedOp);
        }

        @Test
        public void isEmpty() {
            Assert.assertFalse(sut.isEmpty());
        }

        @Test
        public void testToString() {
            Assert.assertEquals("\tpackage example()\n" +
                    "\trenamed from example to testExample\n", sut.toString());
        }

        @Test
        public void getRefactorings() {
            Assert.assertTrue(sut.getRefactorings().isEmpty());
        }

        @Test
        public void isOperationRenamed() {
            Assert.assertTrue(sut.isOperationRenamed());
        }

        @Test
        public void getAddedParameters() {
            Assert.assertTrue(sut.getAddedParameters().isEmpty());
        }

        @Test
        public void getRemovedParameters() {
            Assert.assertTrue(sut.getRemovedParameters().isEmpty());
        }

        @Test
        public void getParameterDiffList() {
            Assert.assertTrue(sut.getParameterDiffList().isEmpty());
        }

    }

    private static UMLOperation parseAndFindFirstMethod(String filePath, String fileContent) {
        var reader = new UMLModelASTReader(Map.of(filePath, fileContent), Collections.emptySet());
        return reader.getUmlModel().getClassList().get(0).getOperations().get(0);
    }

}