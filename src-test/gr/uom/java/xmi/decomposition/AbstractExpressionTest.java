package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AbstractExpressionTest {
    AbstractExpression sut;
    CompilationUnit cu;
    @Before
    public void setUp() {
        var obj = CompilationUnitMother.create();
        cu = obj.cu;
        sut = new AbstractExpression(cu, "", obj.exception, LocationInfo.CodeElementType.TYPE);
    }

    @Test
    public void testGetString() {
        assertEquals("RuntimeException.class",sut.getString());
    }

    @Test
    public void testGetExpression() {
        assertEquals("RuntimeException.class", sut.getExpression());
    }

    @Test
    public void testGetVariables() {
        assertArrayEquals(new String[]{}, sut.getVariables().toArray());
    }

    @Test
    public void testGetTypes() {
        assertArrayEquals(new String[]{"RuntimeException"}, sut.getTypes().toArray());
    }

    @Test
    public void testGetTypeLiterals() {
        assertArrayEquals(new String[]{"RuntimeException.class"}, sut.getTypeLiterals().toArray());
    }

    @Test
    public void testGetInfixExpressions() {
        assertArrayEquals(new String[]{"RuntimeException.class"}, sut.getTypeLiterals().toArray());
    }

    @Test
    public void testGetPrefixExpressions() {
        assertArrayEquals(new String[]{"RuntimeException.class"}, sut.getTypeLiterals().toArray());
    }

    @Test
    public void testGetPostfixExpressions() {
        assertArrayEquals(new String[]{"RuntimeException.class"}, sut.getTypeLiterals().toArray());
    }

    @Test
    public void testGetArguments() {
        assertArrayEquals(new String[]{}, sut.getArguments().toArray());
    }

    @Test
    public void testGetLambdas() {
        assertArrayEquals(new String[]{}, sut.getLambdas().toArray());
    }
}