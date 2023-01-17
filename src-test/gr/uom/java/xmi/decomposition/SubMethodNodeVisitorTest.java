package gr.uom.java.xmi.decomposition;

import org.eclipse.jdt.core.dom.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class SubMethodNodeVisitorTest {
    static private char[] SOURCE = ("import org.junit.Test;" +
            "import org.junit.rules.ExpectedException;" +
            "class ExampleTest {" +
                "@ExpectedException(RuntimeException.class)" +
                "@Test" +
                "void testSomething() {" +
                    "(new Example()).method();" +
                "}" +
            "}").toCharArray();

    SubMethodNodeVisitor visitor;
    private CompilationUnit ast;

    private SubMethodNodeVisitor.SubMethodNodeVisitorBuilder createBuilderFromVisitor(SubMethodNodeVisitor visitor) {
        return SubMethodNodeVisitor.builder().creationMap(visitor.getCreationMap()).methodInvocationMap(this.visitor.getMethodInvocationMap()).cu(ast).filePath("");
    }

    @Before
    public void setUp() {
        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
        parser.setSource(SOURCE);
        ast = (CompilationUnit) parser.createAST(null);
        visitor = new SubMethodNodeVisitor(ast, "");
        ast.accept(visitor);
    }

    @Test
    public void testVisit() {
        SubMethodNodeVisitor expectedVisitor = createBuilderFromVisitor(visitor).variable("Test").variable("ExpectedException").variable("ExampleTest").variable("ExpectedException").type("RuntimeException").type("Example").typeLiteral("RuntimeException.class").build();
        assertThat(visitor).usingRecursiveComparison().isEqualTo(expectedVisitor);
    }

    @Test
    public void testProcessMethodInvocation() {
        var invocations = new ArrayList<MethodInvocation>();
        ASTVisitor v = new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {
                invocations.add(node);
                return super.visit(node);
            }
        };
        ast.accept(v);
        assertEquals(invocations.size(),1);
        assertEquals(SubMethodNodeVisitor.processMethodInvocation(invocations.get(0)),"method()");
    }

    @Test
    public void testProcessClassInstanceCreation() {
        var creations = new ArrayList<ClassInstanceCreation>();
        ASTVisitor v = new ASTVisitor() {
            @Override
            public boolean visit(ClassInstanceCreation node) {
                creations.add(node);
                return super.visit(node);
            }
        };
        ast.accept(v);
        assertEquals(creations.size(),1);
        assertEquals(SubMethodNodeVisitor.processClassInstanceCreation(creations.get(0)),"new Example()");
    }
}