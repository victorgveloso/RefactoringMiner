package org.refactoringminer.test.csharp.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.Visibility;
import gr.uom.java.xmi.diff.ChangeOperationAccessModifierRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.RefactoringType;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
public class ChangeVisibilityRefactoringDetectionTest {

    private static void assertVisibilityChangeDetected(Map<String, String> before, Map<String, String> after,
                                                       Visibility from, Visibility to) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(before).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(after).getUMLModel();

        UMLOperation beforeOp = beforeUML.getClassList().get(0).getOperations().get(0);
        UMLOperation afterOp = afterUML.getClassList().get(0).getOperations().get(0);
        assertEquals(from, beforeOp.getVisibility());
        assertEquals(to, afterOp.getVisibility());

        UMLModelDiff diff = beforeUML.diff(afterUML);
        Optional<ChangeOperationAccessModifierRefactoring> ch = diff.getRefactorings().stream()
                .filter(r -> r.getRefactoringType() == RefactoringType.CHANGE_OPERATION_ACCESS_MODIFIER)
                .map(r -> (ChangeOperationAccessModifierRefactoring) r)
                .findFirst();
        assertTrue(ch.isPresent(), "Expected CHANGE_OPERATION_ACCESS_MODIFIER to be detected. Detected: " + diff.getRefactorings());
        assertEquals(from, ((UMLOperation) ch.get().getOperationBefore()).getVisibility());
        assertEquals(to, ((UMLOperation) ch.get().getOperationAfter()).getVisibility());
    }

    @Test
    void detectsPrivateToPublicVisibilityChange() throws Exception {
        String before = """
                class A {
                    private void Work() { }
                }
                """;
        String after = """
                class A {
                    public void Work() { }
                }
                """;
        assertVisibilityChangeDetected(Map.of("A.cs", before), Map.of("A.cs", after), Visibility.PRIVATE, Visibility.PUBLIC);
    }

    @Test
    void detectsPublicToPrivate() throws Exception {
        String before = """
                class B {
                    public int X()
                    {
                        return 1;
                    }
                }
                """;
        String after = """
                class B {
                    private int X()
                    {
                        return 1;
                    }
                }
                """;
        assertVisibilityChangeDetected(Map.of("B.cs", before), Map.of("B.cs", after), Visibility.PUBLIC, Visibility.PRIVATE);
    }

    @Test
    void detectsPublicToProtected() throws Exception {
        String before = """
                class C {
                    public int F(int n)
                    {
                        return n;
                    }
                }
                """;
        String after = """
                class C {
                    protected int F(int n)
                    {
                        return n;
                    }
                }
                """;
        assertVisibilityChangeDetected(Map.of("C.cs", before), Map.of("C.cs", after), Visibility.PUBLIC, Visibility.PROTECTED);
    }

    @Test
    void detectsProtectedToPublic() throws Exception {
        String before = """
                class D { protected void Do(){ int x = 0; x = x + 1; } }
                """;
        String after = """
                class D { public void Do(){ int x = 0; x = x + 1; } }
                """;
        assertVisibilityChangeDetected(Map.of("D.cs", before), Map.of("D.cs", after), Visibility.PROTECTED, Visibility.PUBLIC);
    }

    @Test
    void detectsInternalToPublic() throws Exception {
        String before = """
                class E { internal int Get(){ return 2; } }
                """;
        String after = """
                class E { public int Get(){ return 2; } }
                """;
        assertVisibilityChangeDetected(Map.of("E.cs", before), Map.of("E.cs", after), Visibility.PACKAGE, Visibility.PUBLIC);
    }

    @Test
    void detectsPublicToInternal() throws Exception {
        String before = """
                class F { public int Value(){ return 3; } }
                """;
        String after = """
                class F { internal int Value(){ return 3; } }
                """;
        assertVisibilityChangeDetected(Map.of("F.cs", before), Map.of("F.cs", after), Visibility.PUBLIC, Visibility.PACKAGE);
    }

    @Test
    void detectsProtectedToPrivate() throws Exception {
        String before = """
                class G { protected int Inc(int x){ return x + 1; } }
                """;
        String after = """
                class G { private int Inc(int x){ return x + 1; } }
                """;
        assertVisibilityChangeDetected(Map.of("G.cs", before), Map.of("G.cs", after), Visibility.PROTECTED, Visibility.PRIVATE);
    }

    @Test
    void detectsPrivateToProtected() throws Exception {
        String before = """
                class H { private int Dec(int x){ return x - 1; } }
                """;
        String after = """
                class H { protected int Dec(int x){ return x - 1; } }
                """;
        assertVisibilityChangeDetected(Map.of("H.cs", before), Map.of("H.cs", after), Visibility.PRIVATE, Visibility.PROTECTED);
    }

    @Test
    void detectsPrivateToInternal() throws Exception {
        String before = """
                class I { private void Log(){ int a = 1; a = a + 1; } }
                """;
        String after = """
                class I { internal void Log(){ int a = 1; a = a + 1; } }
                """;
        assertVisibilityChangeDetected(Map.of("I.cs", before), Map.of("I.cs", after), Visibility.PRIVATE, Visibility.PACKAGE);
    }

    @Test
    void detectsInternalToProtected() throws Exception {
        String before = """
                class J { internal int Len(string s){ return s.Length; } }
                """;
        String after = """
                class J { protected int Len(string s){ return s.Length; } }
                """;
        assertVisibilityChangeDetected(Map.of("J.cs", before), Map.of("J.cs", after), Visibility.PACKAGE, Visibility.PROTECTED);
    }
}
