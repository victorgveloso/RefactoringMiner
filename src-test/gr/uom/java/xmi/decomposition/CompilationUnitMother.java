package gr.uom.java.xmi.decomposition;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Collection;

public class CompilationUnitMother {
    public static CompilationUnit createFromSource(char[] source) {
        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
        parser.setSource(source);
        return (CompilationUnit) parser.createAST(null);
    }

    public static Objects create() {
        var ast = AST.newAST(AST.JLS_Latest, false);
        var cu = ast.newCompilationUnit();
        var junitTestAnnotationImport = ast.newImportDeclaration();
        junitTestAnnotationImport.setName(ast.newQualifiedName(ast.newName(new String[]{"org", "junit"}), ast.newSimpleName("Test")));
        cu.imports().add(junitTestAnnotationImport);
        var junitExpectedExceptionAnnotationImport = ast.newImportDeclaration();
        junitExpectedExceptionAnnotationImport.setName(ast.newQualifiedName(ast.newName(new String[]{"org", "junit"}), ast.newSimpleName("ExpectedException")));
        var testClass = ast.newTypeDeclaration();
        testClass.setInterface(false);
        testClass.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        testClass.setName(ast.newSimpleName("ExampleTest"));
        var testMethod = ast.newMethodDeclaration();
        testMethod.setName(ast.newSimpleName("testExample"));
        testMethod.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        testMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
        var testMethodAnnotation = ast.newNormalAnnotation();
        testMethodAnnotation.setTypeName(ast.newSimpleName("Test"));
        var expectedValuePair = ast.newMemberValuePair();
        var exceptionReference = ast.newTypeLiteral();
        var exceptionType = ast.newSimpleType(ast.newSimpleName("RuntimeException"));
        exceptionReference.setType(exceptionType);
        expectedValuePair.setName(ast.newSimpleName("expected"));
        expectedValuePair.setValue(exceptionReference);
        testMethodAnnotation.values().add(expectedValuePair);
        testMethod.modifiers().add(testMethodAnnotation);
        var testMethodBody = ast.newBlock();
        var sutCreation = ast.newClassInstanceCreation();
        sutCreation.setType(ast.newSimpleType(ast.newSimpleName("Example")));
        var parenthesizedSutCreation = ast.newParenthesizedExpression();
        parenthesizedSutCreation.setExpression(sutCreation);
        var methodInvocation = ast.newMethodInvocation();
        methodInvocation.setExpression(parenthesizedSutCreation);
        methodInvocation.setName(ast.newSimpleName("method"));
        var stmt = ast.newExpressionStatement(methodInvocation);
        testMethodBody.statements().add(stmt);
        testMethod.setBody(testMethodBody);
        testClass.bodyDeclarations().add(testMethod);
        cu.types().add(testClass);



        new Builder().addImport(new String[]{"org", "junit"},"Test").addClass("ExampleTest")
                .addMethod("ExampleTest","testExample").addAnnotation("testExample","Test").addValuePair("Test","expected","RuntimeException")
                .addStatement("testExample", Stmt.CONSTRUCT_AND_INVOKE_METHOD).build();


        return new Objects(cu,testClass,testMethod,testMethodAnnotation,exceptionReference);
    }
    @NoArgsConstructor
    @AllArgsConstructor
    static class Objects {
        CompilationUnit cu;
        TypeDeclaration type;
        MethodDeclaration method;
        NormalAnnotation annotation;
        TypeLiteral exception;
    }
    public enum Stmt {
        CONSTRUCT_AND_INVOKE_METHOD,
        EMPTY_TRY_CATCH,
        SUPER_TO_STRING,
        METHOD_CALL,
        LIST_VARIABLE_CREATION,
        CONDITION_A_IS_0,
        INT_VARIABLE_CREATION
        }
    public static class Builder {
        final AST ast = AST.newAST(AST.JLS_Latest, false);
        CompilationUnit cu = ast.newCompilationUnit();
        Collection<ImportDeclaration> imports = new ArrayList<>();
        Collection<TypeDeclaration> type = new ArrayList<>();
        Collection<MethodDeclaration> method = new ArrayList<>();
        Collection<NormalAnnotation> annotation = new ArrayList<>();
        Collection<TypeLiteral> exception = new ArrayList<>();

        public Builder addImport(String[] qualifier, String name) {

            return this;
        }

        public Builder addClass(String name) {
            return this;
        }

        public Builder addMethod(String className, String name) {
            return this;
        }

        public Builder addAnnotation(String methodName, String name) {
            return this;
        }

        public Builder addValuePair(String annotationName, String key, String value) {
            return this;
        }

        public Builder addStatement(String methodName, Stmt s) {
            return this;
        }

        public Objects build() {
            return new Objects();
        }
    }
}
