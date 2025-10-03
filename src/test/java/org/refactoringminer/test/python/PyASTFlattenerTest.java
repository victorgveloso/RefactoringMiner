package org.refactoringminer.test.python;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.utils.LangASTUtil;

@Isolated
public class PyASTFlattenerTest {

    @Test
    public void testFlattenerWithSimpleFunction() {
        String code = "class Calculator:\n"
                + "    def add(x, y):\n"
                + "        return x + y\n";


        LangASTUtil.printAST(code);

        System.out.println(LangASTUtil.getPyStringify(code));
    }

    @Test
    public void testFlattenerWithFunctionAndIfElse() {
        String code = "class Calculator:\n"
                + "    def min(x, y):\n"
                + "        if x < y:\n"
                + "            return x\n"
                + "        else:\n"
                + "            return y\n";

        String flattened = LangASTUtil.getPyStringify(code);
        System.out.println(flattened);
    }

    @Test
    public void testFlattenerWithMultipleIfs() {
        String code = ""
                + "def describe_number(x):\n"
                + "    if x < 0:\n"
                + "        return \"negative\"\n"
                + "    elif x == 0:\n"
                + "        return \"zero\"\n"
                + "    elif x == 1:\n"
                + "        return \"one\"\n"
                + "    else:\n"
                + "        return \"positive\"\n";

        LangASTUtil.printAST(code);
        String flattened = LangASTUtil.getPyStringify(code);
        System.out.println(flattened);
    }

    @Test
    public void testFlattenerWithVariousFields() {
        String code = ""
                + "class Data:\n"
                + "    def __init__(self, a, b):\n"
                + "        self.a = a\n"
                + "        self.b = b\n"
                + "    def display(self):\n"
                + "        print(self.a, self.b)\n";

        LangASTUtil.printAST(code);
        String flattened = LangASTUtil.getPyStringify(code);
        System.out.println(flattened);
    }

    @Test
    public void testFlattenerWithImportsAndDictionary() {
        String code = ""
                + "import os\n"
                + "from math import sqrt, pi\n"
                + "\n"
                + "data = {\n"
                + "    \"name\": \"John\",\n"
                + "    \"age\": 30,\n"
                + "    \"is_active\": True,\n"
                + "    \"nested\": {\"key\": \"value\", \"number\": 10},\n"
                + "    \"skills\": [\"Python\", \"Java\"]\n"
                + "}\n"
                + "\n"
                + "print(sqrt(data[\"age\"]))\n";

        // Use the LangASTUtil to analyze and print the AST
        LangASTUtil.printAST(code);

        // Get the flattened representation
        String flattened = LangASTUtil.getPyStringify(code);

        // Print the flattened result
        System.out.println(flattened);
    }


}
