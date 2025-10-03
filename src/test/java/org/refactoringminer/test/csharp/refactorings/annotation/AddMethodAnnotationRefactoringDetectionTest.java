package org.refactoringminer.test.csharp.refactorings.annotation;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.RefactoringType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
public class AddMethodAnnotationRefactoringDetectionTest {

    private static void assertAddMethodAnnotationDetected(Map<String, String> before, Map<String, String> after) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(before).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(after).getUMLModel();
        UMLModelDiff diff = beforeUML.diff(afterUML);
        boolean found = diff.getRefactorings().stream().anyMatch(r -> r.getRefactoringType() == RefactoringType.ADD_METHOD_ANNOTATION);
        assertTrue(found, "Expected ADD_METHOD_ANNOTATION to be detected. Detected: " + diff.getRefactorings());
    }

    @Test
    void detectsAddObsoleteWithMessage_onMethod() throws Exception {
        String before = """
                class A {
                    int F()
                    {
                        return 1;
                    }
                }
                """;
        String after = """
                class A {
                    [Obsolete("use G")]
                    int F()
                    {
                        return 1;
                    }
                }
                """;
        assertAddMethodAnnotationDetected(Map.of("A.cs", before), Map.of("A.cs", after));
    }

    @Test
    void detectsAddTwoAttributes_onMethod() throws Exception {
        String before = """
                class B {
                    void Log()
                    {
                        int x = 0;
                        x = x + 1;
                    }
                }
                """;
        String after = """
                class B {
                    [Obsolete]
                    void Log()
                    {
                        int x = 0;
                        x = x + 1;
                    }
                }
                """;
        assertAddMethodAnnotationDetected(Map.of("B.cs", before), Map.of("B.cs", after));
    }

    @Test
    void detectsAddAttributeOnMethodWithParams() throws Exception {
        String before = """
                class C {
                    int Add(int a, int b)
                    {
                        return a + b;
                    }
                }
                """;
        String after = """
                class C {
                    [Obsolete]
                    int Add(int a, int b)
                    {
                        return a + b;
                    }
                }
                """;
        assertAddMethodAnnotationDetected(Map.of("C.cs", before), Map.of("C.cs", after));
    }

    @Test
    void detectsAddAttributeOnMethodWithLocalVar() throws Exception {
        String before = """
                class D {
                    int Area(int w, int h)
                    {
                        int a = w * h;
                        return a;
                    }
                }
                """;
        String after = """
                class D {
                    [Obsolete]
                    int Area(int w, int h)
                    {
                        int a = w * h;
                        return a;
                    }
                }
                """;
        assertAddMethodAnnotationDetected(Map.of("D.cs", before), Map.of("D.cs", after));
    }

    @Test
    void detectsAddAttributeOnMethodWithIf() throws Exception {
        String before = """
                class E {
                    int Abs(int n)
                    {
                        if (n < 0)
                        {
                            n = 0 - n;
                        }
                        return n;
                    }
                }
                """;
        String after = """
                class E {
                    [Obsolete]
                    int Abs(int n)
                    {
                        if (n < 0)
                        {
                            n = 0 - n;
                        }
                        return n;
                    }
                }
                """;
        assertAddMethodAnnotationDetected(Map.of("E.cs", before), Map.of("E.cs", after));
    }

    @Test
    void detectsAddAttributeOnMethodWithWhile() throws Exception {
        String before = """
                class F {
                    int SumTo(int n)
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
        String after = """
                class F {
                    [Obsolete]
                    int SumTo(int n)
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
        assertAddMethodAnnotationDetected(Map.of("F.cs", before), Map.of("F.cs", after));
    }

    @Test
    void detectsAddAttributeOnVoidMethod() throws Exception {
        String before = """
                class G {
                    void Log()
                    {
                        int x = 1;
                        x = x + 1;
                    }
                }
                """;
        String after = """
                class G {
                    [Obsolete]
                    void Log()
                    {
                        int x = 1;
                        x = x + 1;
                    }
                }
                """;
        assertAddMethodAnnotationDetected(Map.of("G.cs", before), Map.of("G.cs", after));
    }

    @Test
    void detectsAddAttributeQualifiedName() throws Exception {
        String before = """
                class H {
                    int Len(string s)
                    {
                        return s.Length;
                    }
                }
                """;
        String after = """
                class H {
                    [System.Obsolete]
                    int Len(string s)
                    {
                        return s.Length;
                    }
                }
                """;
        assertAddMethodAnnotationDetected(Map.of("H.cs", before), Map.of("H.cs", after));
    }

    @Test
    void detectsAddAttributeQualifiedWithMessage() throws Exception {
        String before = """
                class I {
                    int Id(int x)
                    {
                        return x;
                    }
                }
                """;
        String after = """
                class I {
                    [System.Obsolete("x")]
                    int Id(int x)
                    {
                        return x;
                    }
                }
                """;
        assertAddMethodAnnotationDetected(Map.of("I.cs", before), Map.of("I.cs", after));
    }

    @Test
    void detectsAddAttributeOnStringReturnMethod() throws Exception {
        String before = """
                class J {
                    string Name()
                    {
                        return "n";
                    }
                }
                """;
        String after = """
                class J {
                    [Obsolete]
                    string Name()
                    {
                        return "n";
                    }
                }
                """;
        assertAddMethodAnnotationDetected(Map.of("J.cs", before), Map.of("J.cs", after));
    }
}
