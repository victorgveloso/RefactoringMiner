package org.refactoringminer.test.csharp.refactorings.clazz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.utils.RefactoringAssertUtils;

import java.util.Map;

@Isolated
public class RenameClassRefactoringDetectionTest {

    @Test
    void detectsRenameWithMethodReturningValue() throws Exception {
        String before = """
                class Car { int Speed(){ return 60; } }
                """;
        String after = """
                class Vehicle { int Speed(){ return 60; } }
                """;
        RefactoringAssertUtils.assertRenameClassRefactoringDetected(
                Map.of("Car.cs", before), Map.of("Car.cs", after),
                "Car", "Vehicle");
    }

    @Test
    void detectsRenameWithParametersInMethod() throws Exception {
        String before = """
                class Math { int Add(int a, int b){ return a + b; } }
                """;
        String after = """
                class Arithmetic { int Add(int a, int b){ return a + b; } }
                """;
        RefactoringAssertUtils.assertRenameClassRefactoringDetected(
                Map.of("Math.cs", before), Map.of("Math.cs", after),
                "Math", "Arithmetic");
    }

    @Test
    void detectsRenameWithLocalVariable() throws Exception {
        String before = """
                class Box { int Area(int w, int h){ int a = w * h; return a; } }
                """;
        String after = """
                class Rectangle { int Area(int w, int h){ int a = w * h; return a; } }
                """;
        RefactoringAssertUtils.assertRenameClassRefactoringDetected(
                Map.of("Box.cs", before), Map.of("Box.cs", after),
                "Box", "Rectangle");
    }

    @Test
    void detectsRenameWithIfStatement() throws Exception {
        String before = """
                class Num { int Abs(int n){ if(n < 0){ n = 0 - n; } return n; } }
                """;
        String after = """
                class Numbers { int Abs(int n){ if(n < 0){ n = 0 - n; } return n; } }
                """;
        RefactoringAssertUtils.assertRenameClassRefactoringDetected(
                Map.of("Num.cs", before), Map.of("Num.cs", after),
                "Num", "Numbers");
    }

    @Test
    void detectsRenameWithWhileLoop() throws Exception {
        String before = """
                class Counter { int SumTo(int n){ int s = 0; while(n > 0){ s = s + n; n = n - 1; } return s; } }
                """;
        String after = """
                class Accumulator { int SumTo(int n){ int s = 0; while(n > 0){ s = s + n; n = n - 1; } return s; } }
                """;
        RefactoringAssertUtils.assertRenameClassRefactoringDetected(
                Map.of("Counter.cs", before), Map.of("Counter.cs", after),
                "Counter", "Accumulator");
    }

    @Test
    void detectsRenameWithForLoop() throws Exception {
        String before = """
                class Loops { int Sum3(){ int s = 0; for(int i=0;i<3;i=i+1){ s = s + i; } return s; } }
                """;
        String after = """
                class Iter { int Sum3(){ int s = 0; for(int i=0;i<3;i=i+1){ s = s + i; } return s; } }
                """;
        RefactoringAssertUtils.assertRenameClassRefactoringDetected(
                Map.of("Loops.cs", before), Map.of("Loops.cs", after),
                "Loops", "Iter");
    }

    @Test
    void detectsRenameWithVoidMethod() throws Exception {
        String before = """
                class Printer { void Print(){ int x = 1; x = x + 1; } }
                """;
        String after = """
                class Writer { void Print(){ int x = 1; x = x + 1; } }
                """;
        RefactoringAssertUtils.assertRenameClassRefactoringDetected(
                Map.of("Printer.cs", before), Map.of("Printer.cs", after),
                "Printer", "Writer");
    }

    @Test
    void detectsRenameWithMemberAccess() throws Exception {
        String before = """
                class Util { int Len(string s){ return s.Length; } }
                """;
        String after = """
                class StringUtil { int Len(string s){ return s.Length; } }
                """;
        RefactoringAssertUtils.assertRenameClassRefactoringDetected(
                Map.of("Util.cs", before), Map.of("Util.cs", after),
                "Util", "StringUtil");
    }

    @Test
    void detectsRenameWithTwoClassesOnlyOneRenamed() throws Exception {
        String before = """
                class A { int F(){ return 1; } }
                class B { }
                """;
        String after = """
                class A1 { int F(){ return 1; } }
                class B { }
                """;
        RefactoringAssertUtils.assertRenameClassRefactoringDetected(
                Map.of("AB.cs", before), Map.of("AB.cs", after),
                "A", "A1");
    }

    @Test
    void detectsRenameWithParametersRenamedToo() throws Exception {
        String before = """
                class Math2 { int Sum(int a, int b){ int s = a + b; return s; } }
                """;
        String after = """
                class MathPlus { int Sum(int x, int y){ int s = x + y; return s; } }
                """;
        RefactoringAssertUtils.assertRenameClassRefactoringDetected(
                Map.of("Math2.cs", before), Map.of("Math2.cs", after),
                "Math2", "MathPlus");
    }
}
