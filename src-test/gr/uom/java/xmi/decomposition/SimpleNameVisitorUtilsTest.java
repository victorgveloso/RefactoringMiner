package gr.uom.java.xmi.decomposition;

import org.eclipse.jdt.core.dom.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SimpleNameVisitorUtilsTest <T> {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return List.of(List.of("class clazz {void method(){try{}catch(Exception e){}}}", 4, 3, true).toArray(),
                List.of("class clazz {void method(int param){}}", 3, 2, true).toArray(),
                List.of("class clazz {void method(){}}",2,1,true).toArray(),
                List.of("class clazzTest {@Test void testMethod(){}}",3,1, true).toArray(),
                List.of("class clazz extends Serializable {}", 2, 1, true).toArray(),
                List.of("class clazz {void method(){super.toString();}}",3,2,true).toArray(),
                List.of("class clazz {void method(){a();}}",3,2,true).toArray(),
                List.of("class clazz {List<Object> i = new ArrayList<>();}",5,4, true).toArray(),
                List.of("class clazz {int i = 0;}",2,1, false).toArray(),
                List.of("class clazz{clazz(int a){if(a == 0){}}}",4,3, false).toArray(),
                List.of("class clazz{int i = 3 * 5}",2, 1, false).toArray()
        );
    }

    private final String sourceCode;
    private final int numberOfNames;
    private final int nameIndex;
    private final boolean shouldSkip;

    public SimpleNameVisitorUtilsTest(String sourceCode, Integer numberOfNames, Integer nameIndex, Boolean shouldSkip) {
        this.sourceCode = sourceCode;
        this.numberOfNames = numberOfNames;
        this.nameIndex = nameIndex;
        this.shouldSkip = shouldSkip;
    }

    @Test
    public void testShouldSkip() {
        var names = detectNamesIntoCode(sourceCode);
        assertEquals(numberOfNames, names.size());
        assertEquals(shouldSkip, SimpleNameVisitorUtils.shouldSkip(names.get(nameIndex)));
    }

    private static ArrayList<SimpleName> detectNamesIntoCode(String code) {
        var result = new ArrayList<SimpleName>();
        ASTVisitor v = new ASTVisitor() {
            @Override
            public boolean visit(SimpleName node) {
                result.add(node);
                return super.visit(node);
            }
        };
        var parser = ASTParser.newParser(AST.JLS_Latest);
        parser.setSource(code.toCharArray());
        var ast = parser.createAST(null);
        ast.accept(v);
        return result;
    }
}