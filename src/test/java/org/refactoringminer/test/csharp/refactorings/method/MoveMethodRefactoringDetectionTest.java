package org.refactoringminer.test.csharp.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.RefactoringType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
public class MoveMethodRefactoringDetectionTest {

    private static void assertMoveMethodDetected(Map<String, String> before, Map<String, String> after, String methodName, String fromClass, String toClass) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(before).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(after).getUMLModel();
        UMLModelDiff diff = beforeUML.diff(afterUML);
        boolean found = diff.getRefactorings().stream().anyMatch(r -> r.getRefactoringType() == RefactoringType.MOVE_OPERATION || r.getRefactoringType() == RefactoringType.MOVE_AND_RENAME_OPERATION);
        assertTrue(found, "Expected MOVE_OPERATION to be detected. Detected: " + diff.getRefactorings());
        boolean matched = diff.getRefactorings().stream().anyMatch(ref -> {
            if (ref instanceof MoveOperationRefactoring move) {
                boolean nameMatches = move.getMovedOperation().getName().equals(methodName);
                boolean classMatches = move.getOriginalOperation().getClassName().endsWith(fromClass) && move.getMovedOperation().getClassName().endsWith(toClass);
                return nameMatches && classMatches;
            }
            return false;
        });
        assertTrue(matched, "Expected a MoveOperationRefactoring of method '" + methodName + "' from class '" + fromClass + "' to class '" + toClass + "'. Detected: " + diff.getRefactorings());
    }

    @Test
    void detectsMove_withParameters() throws Exception {
        String before = """
                class A {
                    int add(int a, int b)
                    {
                        return a + b;
                    }
                }
                class B {
                }
                """;
        String after = """
                class A {
                }
                class B {
                    int add(int x, int y)
                    {
                        return x + y;
                    }
                }
                """;
        assertMoveMethodDetected(Map.of("AB1.cs", before), Map.of("AB1.cs", after), "add", "A", "B");
    }

    @Test
    void detectsMove_withLocalVariable() throws Exception {
        String before = """
                class A {
                    int area(int w, int h)
                    {
                        int a = w * h;
                        return a;
                    }
                }
                class B {
                }
                """;
        String after = """
                class A {
                }
                class B {
                    int area(int w, int h)
                    {
                        int a = w * h;
                        return a;
                    }
                }
                """;
        assertMoveMethodDetected(Map.of("AB2.cs", before), Map.of("AB2.cs", after), "area", "A", "B");
    }

    @Test
    void detectsMove_voidMethod() throws Exception {
        String before = """
                class A {
                    void log()
                    {
                        int x = 1;
                        x = x + 1;
                    }
                }
                class B {
                }
                """;
        String after = """
                class A {
                }
                class B {
                    void log()
                    {
                        int x = 1;
                        x = x + 1;
                    }
                }
                """;
        assertMoveMethodDetected(Map.of("AB3.cs", before), Map.of("AB3.cs", after), "log", "A", "B");
    }

    @Test
    void detectsMove_methodCallInside() throws Exception {
        String before = """
                class A {
                    int len(string s)
                    {
                        return s.Length;
                    }
                }
                class B {
                }
                """;
        String after = """
                class A {
                }
                class B {
                    int len(string s)
                    {
                        return s.Length;
                    }
                }
                """;
        assertMoveMethodDetected(Map.of("AB4.cs", before), Map.of("AB4.cs", after), "len", "A", "B");
    }

    @Test
    void detectsMove_withIf() throws Exception {
        String before = """
                class A {
                    int abs(int n)
                    {
                        if (n < 0)
                        {
                            n = 0 - n;
                        }
                        return n;
                    }
                }
                class B {
                }
                """;
        String after = """
                class A {
                }
                class B {
                    int abs(int n)
                    {
                        if (n < 0)
                        {
                            n = 0 - n;
                        }
                        return n;
                    }
                }
                """;
        assertMoveMethodDetected(Map.of("AB5.cs", before), Map.of("AB5.cs", after), "abs", "A", "B");
    }

    @Test
    void detectsMove_withWhile() throws Exception {
        String before = """
                class A {
                    int sumTo(int n)
                    {
                        int s = 0;
                        while (n > 0)
                        {
                            s = s + n;
                            n = n - 1;
                        }
                        return s;
                    }
                }
                class B {
                }
                """;
        String after = """
                class A {
                }
                class B {
                    int sumTo(int n)
                    {
                        int s = 0;
                        while (n > 0)
                        {
                            s = s + n;
                            n = n - 1;
                        }
                        return s;
                    }
                }
                """;
        assertMoveMethodDetected(Map.of("AB6.cs", before), Map.of("AB6.cs", after), "sumTo", "A", "B");
    }

    @Test
    void detectsMove_withFor() throws Exception {
        String before = """
                class A {
                    int sum3()
                    {
                        int s = 0;
                        for (int i = 0; i < 3; i = i + 1)
                        {
                            s = s + i;
                        }
                        return s;
                    }
                }
                class B {
                }
                """;
        String after = """
                class A {
                }
                class B {
                    int sum3()
                    {
                        int s = 0;
                        for (int i = 0; i < 3; i = i + 1)
                        {
                            s = s + i;
                        }
                        return s;
                    }
                }
                """;
        assertMoveMethodDetected(Map.of("AB7.cs", before), Map.of("AB7.cs", after), "sum3", "A", "B");
    }

    @Test
    void detectsMove_betweenThreeClasses_sourceToThird() throws Exception {
        String before = """
                class A {
                    int f()
                    {
                        return 1;
                    }
                }
                class B {
                }
                class C {
                }
                """;
        String after = """
                class A {
                }
                class B {
                }
                class C {
                    int f()
                    {
                        return 1;
                    }
                }
                """;
        assertMoveMethodDetected(Map.of("ABC1.cs", before), Map.of("ABC1.cs", after), "f", "A", "C");
    }

    @Test
    void detectsMove_betweenThreeClasses_sourceToSecond() throws Exception {
        String before = """
                class A {
                    int g(int x)
                    {
                        return x;
                    }
                }
                class B {
                }
                class C {
                }
                """;
        String after = """
                class A {
                }
                class B {
                    int g(int x)
                    {
                        return x;
                    }
                }
                class C {
                }
                """;
        assertMoveMethodDetected(Map.of("ABC2.cs", before), Map.of("ABC2.cs", after), "g", "A", "B");
    }

    @Test
    void detectsMove_methodWithDifferentParameterNames() throws Exception {
        String before = """
                class A {
                    int add(int a, int b)
                    {
                        int s = a + b;
                        return s;
                    }
                }
                class B {
                }
                """;
        String after = """
                class A {
                }
                class B {
                    int add(int x, int y)
                    {
                        int s = x + y;
                        return s;
                    }
                }
                """;
        assertMoveMethodDetected(Map.of("AB8.cs", before), Map.of("AB8.cs", after), "add", "A", "B");
    }
}
