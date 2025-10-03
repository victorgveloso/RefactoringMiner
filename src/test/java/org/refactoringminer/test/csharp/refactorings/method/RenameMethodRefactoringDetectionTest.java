package org.refactoringminer.test.csharp.refactorings.method;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.RefactoringType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
public class RenameMethodRefactoringDetectionTest {

    private static void assertRenameMethodDetected(Map<String, String> before, Map<String, String> after, String beforeName, String afterName) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(before).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(after).getUMLModel();
        UMLModelDiff diff = beforeUML.diff(afterUML);
        boolean found = diff.getRefactorings().stream().anyMatch(r -> r.getRefactoringType() == RefactoringType.RENAME_METHOD);
        assertTrue(found, "Expected RENAME_METHOD to be detected. Detected: " + diff.getRefactorings());
        boolean matched = diff.getRefactorings().stream().anyMatch(ref -> {
            if (ref instanceof RenameOperationRefactoring rename) {
                return rename.getOriginalOperation().getName().equals(beforeName) &&
                        rename.getRenamedOperation().getName().equals(afterName);
            }
            return false;
        });
        assertTrue(matched, "Expected a RenameOperationRefactoring from '" + beforeName + "' to '" + afterName + "'. Detected: " + diff.getRefactorings());
    }

    @Test
    void detectsSimpleMethodRename_inSingleClass() throws Exception {
        String before = """
                class Account {
                    int balance()
                    {
                        return 100;
                    }
                }
                """;
        String after = """
                class Account {
                    int getBalance()
                    {
                        return 100;
                    }
                }
                """;
        assertRenameMethodDetected(Map.of("Account.cs", before), Map.of("Account.cs", after), "balance", "getBalance");
    }

    @Test
    void detectsMethodRename_withParametersAndBody() throws Exception {
        String before = """
                class MathOps {
                    int add(int a, int b)
                    {
                        int s = a + b;
                        return s;
                    }
                }
                """;
        String after = """
                class MathOps {
                    int sum(int x, int y)
                    {
                        int s = x + y;
                        return s;
                    }
                }
                """;
        assertRenameMethodDetected(Map.of("MathOps.cs", before), Map.of("MathOps.cs", after), "add", "sum");
    }

    @Test
    void detectsMethodRename_withDifferentReturnName() throws Exception {
        String before = """
                class Greeter {
                    string hi()
                    {
                        return "hi";
                    }
                }
                """;
        String after = """
                class Greeter {
                    string sayHi()
                    {
                        return "hi";
                    }
                }
                """;
        assertRenameMethodDetected(Map.of("Greeter.cs", before), Map.of("Greeter.cs", after), "hi", "sayHi");
    }

    @Test
    void detectsMethodRename_withSingleParameter() throws Exception {
        String before = """
                class Identity {
                    int id(int x)
                    {
                        return x;
                    }
                }
                """;
        String after = """
                class Identity {
                    int value(int x)
                    {
                        return x;
                    }
                }
                """;
        assertRenameMethodDetected(Map.of("Identity.cs", before), Map.of("Identity.cs", after), "id", "value");
    }

    @Test
    void detectsMethodRename_withLocalVariable() throws Exception {
        String before = """
                class Box {
                    int area(int w, int h)
                    {
                        int a = w * h;
                        return a;
                    }
                }
                """;
        String after = """
                class Box {
                    int computeArea(int w, int h)
                    {
                        int a = w * h;
                        return a;
                    }
                }
                """;
        assertRenameMethodDetected(Map.of("Box.cs", before), Map.of("Box.cs", after), "area", "computeArea");
    }

    @Test
    void detectsMethodRename_withIfStatement() throws Exception {
        String before = """
                class Num {
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
        String after = """
                class Num {
                    int absolute(int n)
                    {
                        if (n < 0)
                        {
                            n = 0 - n;
                        }
                        return n;
                    }
                }
                """;
        assertRenameMethodDetected(Map.of("Num.cs", before), Map.of("Num.cs", after), "abs", "absolute");
    }

    @Test
    void detectsMethodRename_withWhileLoop() throws Exception {
        String before = """
                class Counter {
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
        String after = """
                class Counter {
                    int accumulateTo(int n)
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
        assertRenameMethodDetected(Map.of("Counter.cs", before), Map.of("Counter.cs", after), "sumTo", "accumulateTo");
    }

    @Test
    void detectsMethodRename_withForLoop() throws Exception {
        String before = """
                class Loops {
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
        String after = """
                class Loops {
                    int sumFirst3()
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
        assertRenameMethodDetected(Map.of("Loops.cs", before), Map.of("Loops.cs", after), "sum3", "sumFirst3");
    }

    @Test
    void detectsMethodRename_onVoidMethod() throws Exception {
        String before = """
                class Printer {
                    void print()
                    {
                        int x = 1;
                        x = x + 1;
                    }
                }
                """;
        String after = """
                class Printer {
                    void write()
                    {
                        int x = 1;
                        x = x + 1;
                    }
                }
                """;
        assertRenameMethodDetected(Map.of("Printer.cs", before), Map.of("Printer.cs", after), "print", "write");
    }

    @Test
    void detectsMethodRename_inDifferentClass() throws Exception {
        String before = """
                class A {
                    int f()
                    {
                        return 1;
                    }
                }
                class B {
                }
                """;
        String after = """
                class A {
                    int g()
                    {
                        return 1;
                    }
                }
                class B {
                }
                """;
        assertRenameMethodDetected(Map.of("AB.cs", before), Map.of("AB.cs", after), "f", "g");
    }
}
