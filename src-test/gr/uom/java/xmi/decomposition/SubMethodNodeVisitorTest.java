package gr.uom.java.xmi.decomposition;

import org.eclipse.jdt.core.dom.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class SubMethodNodeVisitorTest {
    static private final char[] SOURCE = ("import org.junit.Test;" +
            "class ExampleTest {" +
            "@Test(expected = RuntimeException.class)" +
            "void testExample() {" +
            "(new Example()).method();" +
            "}" +
            "}").toCharArray();

    SubMethodNodeVisitor visitor;
    private CompilationUnit unit;

    private SubMethodNodeVisitor.SubMethodNodeVisitorBuilder createBuilderFromVisitor(SubMethodNodeVisitor visitor) {
        return SubMethodNodeVisitor.builder().creationMap(visitor.getCreationMap()).methodInvocationMap(this.visitor.getMethodInvocationMap()).cu(unit).filePath("");
    }

    @Before
    public void setUp() {
        unit = CompilationUnitMother.createFromSource(SOURCE);
        visitor = new SubMethodNodeVisitor(unit, "");
        unit.accept(visitor);
    }

    @Test
    public void testVisit() {
        SubMethodNodeVisitor expectedVisitor = createBuilderFromVisitor(visitor).variable("Test").variable("ExampleTest").variable("Test").variable("expected").type("RuntimeException").type("void").type("Example").typeLiteral("RuntimeException.class").build();
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
        unit.accept(v);
        assertEquals(invocations.size(), 1);
        assertEquals(SubMethodNodeVisitor.processMethodInvocation(invocations.get(0)), "method()");
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
        unit.accept(v);
        assertEquals(creations.size(), 1);
        assertEquals(SubMethodNodeVisitor.processClassInstanceCreation(creations.get(0)), "new Example()");
    }

}