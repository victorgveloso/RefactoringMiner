package org.refactoringminer.test.csharp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.utils.LangASTUtil;

@Isolated
public class CSharpASTPrinterTest {

    @Test
    public void testCSharp_EmptyClass() {
        String code = "class EmptyClass { }";
        LangASTUtil.printASTCSharp(code);
    }

    @Test
    public void testCSharp_SimpleMethod_ReturnLiteral() {
        String code = "class Calculator { int Answer() { return 42; } }";
        LangASTUtil.printASTCSharp(code);
    }

    @Test
    public void testCSharp_SimpleMethod_ReturnParam() {
        String code = "class Identity { int Id(int x) { return x; } }";
        LangASTUtil.printASTCSharp(code);
    }

    @Test
    public void testCSharp_MultipleMethods() {
        String code = "class Ops { int One() { return 1; } int Two() { return 2; } }";
        LangASTUtil.printASTCSharp(code);
    }

    @Test
    public void testCSharp_BlockOnly() {
        String code = "class Blocks { int Foo() { { return 7; } } }";
        LangASTUtil.printASTCSharp(code);
    }

    @Test
    public void testCSharp_Assignment_And_BinaryOps() {
        String code = "class A { int F(int a, int b){ int c = a + b * 2; c = c - 1; return c; } }";
        LangASTUtil.printASTCSharp(code);
    }

    @Test
    public void testCSharp_MethodInvocation_And_MemberAccess() {
        String code = "class B { int Len(string s){ return s.Length; } int Call(){ return Len(\"hi\"); } }";
        LangASTUtil.printASTCSharp(code);
    }

    @Test
    public void testCSharp_If_While_For() {
        String code = "class C { int Sum(int n){ int s = 0; if(n > 0){ while(n > 0){ s = s + n; n = n - 1; } } for(int i=0;i<3;i=i+1){ s = s + i; } return s; } }";
        LangASTUtil.printASTCSharp(code);
    }
}