package org.refactoringminer.test.python;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.utils.LangASTUtil;

@Isolated
public class PyASTPrinterTest {

    @Test
    public void testASTVisitorSimple() {
        String code = "x = 10\n";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor() {
        // Example Python code
        String code = "class Calculator:\n" +
                "    def add(self, x, y):\n" +
                "        x = x + y\n" +
                "        return x";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_AugmentOperator() {
        // Example Python code
        String code = "class Calculator:\n" +
                "    def add(self, x, y):\n" +
                "        x += y\n" +
                "        return x";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_IdentityOperators() {
        // Example Python code with 'is' and 'is not' operators
        String code = "class Checker:\n" +
                "    def check_identity(self, x, y):\n" +
                "        if x is None:\n" +
                "            return True\n" +
                "        if x is not None:\n" +
                "            return False\n" +
                "        return x is y";
        LangASTUtil.printAST(code);
    }


    @Test
    public void testASTVisitor_IfElifElse() {
        // Example Python code with if-elif-else chain
        String code =
                "def categorize_number(x):\n" +
                        "    if x < 0:\n" +
                        "        return 'Negative'\n" +
                        "    elif x == 0:\n" +
                        "        return 'Zero'\n" +
                        "    elif x > 0 and x <= 10:\n" +
                        "        return 'Small Positive'\n" +
                        "    else:\n" +
                        "        return 'Large Positive'\n";

        // Print the AST
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_WithComments() {
        // Example Python code
        String code = "class Calculator:\n" +
                "    # Comment one line\n" +
                "    def add(self, x, y):\n" +
                "        x = x + y\n" +
                "        return x";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_IfStatement() {
        // Example Python code with if statement
        String code = "class Calculator:\n" +
                "    def add(self, x, y):\n" +
                "        if x > y:\n" +
                "            return x\n" +
                "        else:\n" +
                "            return y";

        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_ForLoop() {
        // Example Python code with for loop
        String code = "class ListProcessor:\n" +
                "    def process_numbers(self, numbers):\n" +
                "        sum = 0\n" +
                "        for num in numbers:\n" +
                "            sum = sum + num\n" +
                "        return sum";

        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_WhileLoop() {
        // Example Python code with for while
        String code = "class Counter:\n" +
                "    def count_up(self, limit):\n" +
                "        i = 0\n" +
                "        while i < limit:\n" +
                "            i = i + 1\n" +
                "            print(i)\n" +
                "        return i";

        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_MultipleMethods() {
        // Example Python code with for while
        String code = "class Counter:\n" +
                "    def count_up(self, limit):\n" +
                "        i = 0\n" +
                "        while i < limit:\n" +
                "            i = i + 1\n" +
                "            print(i)\n" +
                "        return i\n" +
                "       \n" +
                "    def process_numbers(self, numbers):\n" +
                "        sum = 0\n" +
                "        for num in numbers:\n" +
                "            sum = sum + num\n" +
                "        return sum";

        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_Literals() {
        String code =
                "class Example:\n" +
                        "    def demonstrate_literals(self):\n" +
                        "        text = \"hello world\"\n" +
                        "        numbers = [1, 2, 3]\n" +
                        "        flag = True\n" +
                        "        return flag\n";


        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_MethodInvocation() {
        String code =
                "class Example:\n" +
                        "    def demonstrate_literals(self):\n" +
                        "        text = \"hello world\"\n" +
                        "        print(text)\n" +
                        "        return flag\n";


        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_MethodInvocationWithMultipleArguments() {
        String code =
                "class Example:\n" +
                        "    def demonstrate_literals(self):\n" +
                        "        text1 = \"hello world1\"\n" +
                        "        text2 = \"hello world2\"\n" +
                        "        method(text1, text2)\n" +
                        "        flag = false\n" +
                        "        return flag\n";


        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_MethodInvocationWithChaining() {
        String code =
                "class Example:\n" +
                        "    def demonstrate_literals(self):\n" +
                        "        text1 = \"hello world1\"\n" +
                        "        text2 = \"hello world2\"\n" +
                        "        method1(text1).method2(text2)\n" +
                        "        flag = false\n" +
                        "        return flag\n";


        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_PythonTuple() {
        String code =
                "def test_tuple_operations():\n" +
                        "    coordinates = (10, 20, 30)\n" +
                        "    \n" +
                        "    # Access tuple elements\n" +
                        "    x = coordinates[0]\n" +
                        "    y = coordinates[1]\n" +
                        "    z = coordinates[2]\n" +
                        "    \n" +
                        "    return (x, y + 5, z * 2)\n";

        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_PythonDictionary() {
        String code =
                "def test_dictionary_operations():\n" +
                        "    user = {\"name\": \"John\", \"age\": 30, \"is_active\": True}\n" +
                        "    \n" +
                        "    username = user[\"name\"]\n" +
                        "    \n" +
                        "    user[\"last_login\"] = \"2023-07-15\"\n" +
                        "    user[\"age\"] = user[\"age\"] + 1\n" +
                        "    \n" +
                        "    return user\n";

        LangASTUtil.printAST(code);
    }


    @Test
    public void testASTVisitor_PythonMinimalVarargs() {
        String code =
                "def _foo__(*args):\n" +
                        "    pass\n";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_OnlyImports() {
        String code =
                "import os\n" +
                        "import sys, re\n" +
                        "import numpy as np\n" +
                        "from math import sqrt, pi\n" +
                        "from collections import defaultdict as dd\n" +
                        "from random import *\n" +
                        "from . import sibling\n" +
                        "from ..parent import utility\n";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_BreakAndContinue() {
        String code =
                "for i in range(10):\n" +
                        "    if i == 3:\n" +
                        "        continue\n" +
                        "    if i == 7:\n" +
                        "        break\n" +
                        "    print(i)\n";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_GlobalPassDelYieldAssert() {
        String code =
                "global x, y\n" +       // global statement
                        "pass\n" +              // pass statement
                        "del x\n" +             // del statement
                        "def gen():\n" +        // yield statement inside a function
                        "    yield 42\n" +
                        "assert x > 0, 'x must be positive'\n"; // assert statement
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_TryExceptRaiseWith() {
        String code =
                "def operations():\n" +
                        "    with open('file.txt') as f:\n" +
                        "        data = f.read()\n" +
                        "    try:\n" +
                        "        risky_operation()\n" +
                        "    except ValueError as ex:\n" +
                        "        raise RuntimeError('Wrapping error') from ex\n" +
                        "    finally:\n" +
                        "        cleanup()\n";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_TryExceptRaiseWith_CustomContext() {
        String code =
                "def operations():\n" +
                        "    with DatabaseConnection() as conn:\n" +
                        "        conn.query()\n" +
                        "    try:\n" +
                        "        risky_operation()\n" +
                        "    except ValueError as ex:\n" +
                        "        raise RuntimeError('Wrapping error') from ex\n" +
                        "    finally:\n" +
                        "        cleanup()\n";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_NonlocalAndAsync() {
        String code =
                "def outer():\n" +
                        "    var = 10\n" +
                        "    def inner():\n" +
                        "        nonlocal var\n" +
                        "        var = var + 1\n" +
                        "    async def do_async():\n" +
                        "        await something()\n";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_LambdaFunction() {
        String code =
                "def apply(f, x):\n" +
                        "    return f(x)\n" +
                        "double = lambda y: y * 2\n" +
                        "result = apply(double, 4)\n";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_MatchCase() {
        String code =
                "def http_status(status):\n" +
                        "    match status:\n" +
                        "        case 400:\n" +
                        "            return 'Bad request'\n" +
                        "        case 404:\n" +
                        "            return 'Not found'\n" +
                        "        case 418:\n" +
                        "            return \"I'm a teapot\"\n" +
                        "        case _:\n" +
                        "            return 'Something else'\n";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_MethodReturnType() {
        String code =
                "def add(x: int, y: int) -> int:\n" +
                        "    return x + y\n" +
                        "\n" +
                        "def greet(name: str) -> str:\n" +
                        "    return 'Hello, ' + name\n";

        LangASTUtil.printAST(code);
    }


    @Test
    public void testASTVisitor_ComplexPython() {
        String code =
                "import math\n" +
                        "\n" +
                        "def safe_divide(a, b):\n" +
                        "    try:\n" +
                        "        return a / b\n" +
                        "    except ZeroDivisionError:\n" +
                        "        return None\n" +
                        "\n" +
                        "def map_and_filter(func, lst):\n" +
                        "    return [func(x) for x in lst if func(x) is not None]\n" +
                        "\n" +
                        "numbers = [10, 0, 5, 2]\n" +
                        "div_func = lambda y: safe_divide(100, y)\n" +
                        "\n" +
                        "results = map_and_filter(div_func, numbers)\n" +
                        "total = 0\n" +
                        "\n" +
                        "for val in results:\n" +
                        "    if val is None:\n" +
                        "        total = total + val\n" +
                        "    else:\n" +
                        "        pass\n" +
                        "\n" +
                        "settings = {'threshold': 1, 'verbose': True}\n" +
                        "\n" +
                        "with open('log.txt', 'w') as log:\n" +
                        "    log.write(str(results))\n" +
                        "\n" +
                        "print('Sum:', total)\n";
        LangASTUtil.printAST(code);
    }

    @Test
    public void testASTVisitor_PythonDocstringsAndExpressions() {
        String code =
                "'''\n" +
                        "This is a module-level docstring.\n" +
                        "It documents what this module does.\n" +
                        "'''\n" +
                        "\n" +
                        "import os\n" +
                        "import sys\n" +
                        "\n" +
                        "# Regular assignments\n" +
                        "x = 5\n" +
                        "y = \"hello world\"\n" +
                        "z = [1, 2, 3]\n" +
                        "\n" +
                        "# Tuple assignments\n" +
                        "a, b, c = 1, 2, 3\n" +
                        "first, *middle, last = [1, 2, 3, 4, 5]\n" +
                        "\n" +
                        "# Augmented assignments\n" +
                        "x += 10\n" +
                        "y *= 2\n" +
                        "z.append(4)\n" +
                        "\n" +
                        "def my_function(param1, param2):\n" +
                        "    '''This is a function docstring'''\n" +
                        "    result = param1 + param2\n" +
                        "    return result\n" +
                        "\n" +
                        "class MyClass:\n" +
                        "    '''This is a class docstring'''\n" +
                        "    \n" +
                        "    def __init__(self):\n" +
                        "        self.value = 42\n" +
                        "        \n" +
                        "    def method(self, *args):\n" +
                        "        '''Method docstring'''\n" +
                        "        return sum(args)\n" +
                        "\n" +
                        "# Expression statements\n" +
                        "print(\"Hello\")\n" +
                        "my_function(1, 2)\n" +
                        "obj.method_call()\n" +
                        "\n" +
                        "# Star expressions in function calls\n" +
                        "numbers = [1, 2, 3]\n" +
                        "print(*numbers)\n" +
                        "\n" +
                        "# Complex assignments with unpacking\n" +
                        "data = {'a': 1, 'b': 2}\n" +
                        "key, value = data.items()\n" +
                        "\n" +
                        "# This should NOT be a docstring (inside function)\n" +
                        "def another_func():\n" +
                        "    x = \"This is just a string assignment\"\n" +
                        "    return x\n" +
                        "\n" +
                        "# This should NOT be a docstring (in assignment)\n" +
                        "message = \"This is also not a docstring\"\n";

        LangASTUtil.printAST(code);
    }


}
