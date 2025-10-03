package extension.umladapter.processor;

import extension.ast.node.LangASTNode;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.LangAssignment;
import extension.ast.node.expression.LangFieldAccess;
import extension.ast.node.expression.LangMethodInvocation;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.statement.*;
import extension.ast.visitor.LangVisitor;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.*;

import java.util.List;


public class UMLAdapterStatementProcessor {

    public static void processStatement(LangASTNode statement, CompositeStatementObject composite,
                                        String sourceFolder, String filePath, UMLOperation container) {

        //System.out.println("Processing statement type: " + statement.getNodeType());
        if (statement == null) {
            System.err.println("Warning: Null statement encountered, skipping processing");
            return;
        }

        switch (statement.getNodeType()) {

            case TYPE_DECLARATION:
                processTypeDeclaration((LangTypeDeclaration) statement, composite, sourceFolder, filePath, container);
                break;

            case METHOD_DECLARATION:
                processMethodDeclaration((LangMethodDeclaration) statement, composite, sourceFolder, filePath, container);
                break;

            case IMPORT_STATEMENT:
                processImportStatement((LangImportStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case ANNOTATION:
                processAnnotation((LangAnnotation) statement, composite, sourceFolder, filePath, container);
                break;

            case RETURN_STATEMENT:
                processReturnStatement((LangReturnStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case ASSIGNMENT:
                processAssignment((LangAssignment) statement, composite, sourceFolder, filePath, container);
                break;

            case METHOD_INVOCATION:
                processMethodInvocation((LangMethodInvocation) statement, composite, sourceFolder, filePath, container);
                break;

            case FIELD_ACCESS:
                processFieldAccess((LangFieldAccess) statement, composite, sourceFolder, filePath, container);
                break;

            case BLOCK:
                processBlock((LangBlock) statement, composite, sourceFolder, filePath, container);
                break;

            case EXPRESSION_STATEMENT:
                processExpressionStatement((LangExpressionStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case IF_STATEMENT:
                processIfStatement((LangIfStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case WHILE_STATEMENT:
                processWhileStatement((LangWhileStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case FOR_STATEMENT:
                processForStatement((LangForStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case SWITCH_STATEMENT:
                processSwitchStatement((LangSwitchStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case TRY_STATEMENT:
                processTryStatement((LangTryStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case BREAK_STATEMENT:
                processBreakStatement((LangBreakStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case CONTINUE_STATEMENT:
                processContinueStatement((LangContinueStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case PASS_STATEMENT:
                processPassStatement((LangPassStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case WITH_STATEMENT:
                processWithStatement((LangWithStatement) statement, composite, sourceFolder, filePath, container);
                break;

            // TODO: Add the new Python-specific statements
            case DEL_STATEMENT:
                processDelStatement((LangDelStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case GLOBAL_STATEMENT:
                processGlobalStatement((LangGlobalStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case ASSERT_STATEMENT:
                processAssertStatement((LangAssertStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case YIELD_STATEMENT:
                processYieldStatement((LangYieldStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case THROW_STATEMENT:
                processThrowStatement((LangThrowStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case NON_LOCAL_STATEMENT:
                processNonLocalStatement((LangNonLocalStatement) statement, composite, sourceFolder, filePath, container);
                break;

            case ASYNC_STATEMENT:
                processAsyncStatement((LangAsyncStatement) statement, composite, sourceFolder, filePath, container);
                break;

//            case "LangLambdaExpression":
//                processLambdaExpression((LangLambdaExpression) statement, composite, sourceFolder, filePath, container);
//                break;


            default:
                System.err.println("Warning: Unknown statement type: " + statement.getClass().getSimpleName());
                break;
        }

    }

    public static void processTypeDeclaration(LangTypeDeclaration typeDecl, CompositeStatementObject composite,
                                              String sourceFolder, String filePath, UMLOperation container) {
        // Create a statement object for the nested class
        StatementObject classStatement = new StatementObject(
                typeDecl.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                typeDecl,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.TYPE_DECLARATION,
                container
        );

        // Process the class body - methods and nested classes within it
        if (typeDecl.getChildren() != null) {
            CompositeStatementObject nestedComposite = new CompositeStatementObject(
                    typeDecl.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    typeDecl,
                    composite.getDepth() + 2,
                    LocationInfo.CodeElementType.BLOCK
            );

            // Process all members of the nested class
            for (LangASTNode child : typeDecl.getChildren()) {
                processStatement(child, nestedComposite, sourceFolder, filePath, container);
            }

            composite.addStatement(nestedComposite);
        }

        composite.addStatement(classStatement);
    }


    public static void processMethodDeclaration(LangMethodDeclaration methodDecl, CompositeStatementObject composite,
                                                String sourceFolder, String filePath, UMLOperation container) {
        // Create the method declaration statement object
        StatementObject methodStatement = new StatementObject(
                methodDecl.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                methodDecl,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.METHOD_DECLARATION,
                container
        );

        // Process method parameters as variable declarations
        if (methodDecl.getParameters() != null) {
            for (LangSingleVariableDeclaration param : methodDecl.getParameters()) {
                VariableDeclaration varDecl = new VariableDeclaration(
                        methodDecl.getRootCompilationUnit(),
                        sourceFolder,
                        filePath,
                        param
                );
                composite.addVariableDeclaration(varDecl);
            }
        }

        // Process the method body recursively (nested functions can have their own nested functions!)
        if (methodDecl.getBody() != null) {
            CompositeStatementObject nestedComposite = new CompositeStatementObject(
                    methodDecl.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    methodDecl.getBody(),
                    composite.getDepth() + 2,
                    LocationInfo.CodeElementType.BLOCK
            );

            // Process nested method body - call the existing method from UMLModelAdapter
            processMethodBodyStatements(methodDecl.getBody(), nestedComposite, sourceFolder, filePath, container);
            composite.addStatement(nestedComposite);
        }

        composite.addStatement(methodStatement);
    }

    private static void processMethodBodyStatements(LangBlock methodBody, CompositeStatementObject composite,
                                                    String sourceFolder, String filePath, UMLOperation container) {
        if (methodBody.getStatements() != null) {
            for (LangASTNode statement : methodBody.getStatements()) {
                if (statement != null) {
                    processStatement(statement, composite, sourceFolder, filePath, container);
                }
            }
        }

    }


    public static void processImportStatement(LangImportStatement importStmt, CompositeStatementObject composite,
                                              String sourceFolder, String filePath, UMLOperation container) {
        // Create the import statement object
        StatementObject importStatement = new StatementObject(
                importStmt.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                importStmt,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.IMPORT_DECLARATION,
                container
        );

        composite.addStatement(importStatement);
    }

    public static void processAnnotation(LangAnnotation statement, CompositeStatementObject composite, String sourceFolder, String filePath, UMLOperation container) {
        // Create the annotation statement object
        StatementObject annotationStatement = new StatementObject(
                statement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                statement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.ANNOTATION,
                container
        );

        composite.addStatement(annotationStatement);
    }


    public static void processAssignment(LangAssignment assignment, CompositeStatementObject composite,
                                         String sourceFolder, String filePath, UMLOperation container) {
        // Create the assignment statement object
        StatementObject assignmentStatement = new StatementObject(
                assignment.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                assignment,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.ASSIGNMENT,
                container
        );

        for (VariableDeclaration varDecl : assignmentStatement.getVariableDeclarations()) {
            composite.addVariableDeclaration(varDecl);
        }

        composite.addStatement(assignmentStatement);
    }



    public static void processFieldAccess(LangFieldAccess fieldAccess, CompositeStatementObject composite,
                                    String sourceFolder, String filePath, UMLOperation container) {
        // Track field access for detecting Move Field/Method refactorings
        StatementObject fieldAccessStatement = new StatementObject(
                fieldAccess.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                fieldAccess,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.FIELD_ACCESS,
                container
        );
        composite.addStatement(fieldAccessStatement);
    }

    public static void processMethodInvocation(LangMethodInvocation methodInvocation, CompositeStatementObject composite,
                                               String sourceFolder, String filePath, UMLOperation container) {
        StatementObject methodInvocationStatement = new StatementObject(
                methodInvocation.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                methodInvocation,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.METHOD_INVOCATION,
                container
        );
        composite.addStatement(methodInvocationStatement);
    }


    public static void processReturnStatement(LangReturnStatement returnStmt, CompositeStatementObject composite,
                                              String sourceFolder, String filePath, UMLOperation container){
        StatementObject returnStatement = new StatementObject(
                returnStmt.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                returnStmt,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.RETURN_STATEMENT,
                container
        );
        composite.addStatement(returnStatement);
    }

    public static void processBlock(LangBlock block, CompositeStatementObject composite, String sourceFolder, String filePath, UMLOperation container) {
        // For blocks, create a composite statement and process its children
        CompositeStatementObject blockObject = new CompositeStatementObject(
                block.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                block,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.BLOCK
        );
        composite.addStatement(blockObject);

        // Process each statement in the block
        for (LangASTNode childStatement : block.getStatements()) {
            processStatement(childStatement, blockObject, sourceFolder, filePath, container);
        }
    }

    public static void processExpressionStatement(LangExpressionStatement expressionStatement, CompositeStatementObject composite, String sourceFolder, String filePath, UMLOperation container) {
        // Check if the expression is a method invocation
        LangASTNode expression = expressionStatement.getExpression();

        if (expression instanceof LangAssignment) {
            StatementObject assignmentStatement = new StatementObject(
                    expressionStatement.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    expressionStatement, // Use the expression statement as the node
                    composite.getDepth() + 1,
                    LocationInfo.CodeElementType.ASSIGNMENT,
                    container
            );
            composite.addStatement(assignmentStatement);
        } else if (expression instanceof LangMethodInvocation methodInvocation) {
            // Create a statement object for the method invocation
            StatementObject methodInvocationStatement = new StatementObject(
                    methodInvocation.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    expressionStatement,
                    composite.getDepth() + 1,
                    LocationInfo.CodeElementType.METHOD_INVOCATION,
                    container
            );
            composite.addStatement(methodInvocationStatement);
        } else {
            // Handle other expression types
            StatementObject expressionStatementObject = new StatementObject(
                    expressionStatement.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    expressionStatement,
                    composite.getDepth() + 1,
                    LocationInfo.CodeElementType.EXPRESSION_STATEMENT,
                    container
            );

            for (VariableDeclaration varDecl : expressionStatementObject.getVariableDeclarations()) {
                composite.addVariableDeclaration(varDecl);
            }

            composite.addStatement(expressionStatementObject);
        }

    }

    public static void processIfStatement(LangIfStatement ifStatement, CompositeStatementObject composite,
                                          String sourceFolder, String filePath, UMLOperation container) {
        // Create composite for if statement
        CompositeStatementObject ifComposite = new CompositeStatementObject(
                ifStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                ifStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.IF_STATEMENT
        );

        // Add the condition as an expression
        if (ifStatement.getCondition() != null) {
            AbstractExpression conditionExpr = new AbstractExpression(
                    ifStatement.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    ifStatement.getCondition(),
                    LocationInfo.CodeElementType.IF_STATEMENT_CONDITION,
                    container
            );
            ifComposite.addExpression(conditionExpr);
        }

        composite.addStatement(ifComposite);

        // Process the condition expression
        if (ifStatement.getCondition() != null) {
            ifStatement.getCondition().accept(new LangVisitor(
                    ifStatement.getRootCompilationUnit(), sourceFolder, filePath, container));
        }


        // Process the else block if it exists
        if (ifStatement.getElseBody() != null) {
            processStatement(ifStatement.getElseBody(), composite, sourceFolder, filePath, container);
        }
    }

    public static void processWhileStatement(LangWhileStatement whileStatement, CompositeStatementObject composite,
                                             String sourceFolder, String filePath, UMLOperation container) {
        // Create a CompositeStatementObject for the while statement
        CompositeStatementObject whileStatementObject = new CompositeStatementObject(
                whileStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                whileStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.WHILE_STATEMENT
        );


        // Process the condition expression
        if (whileStatement.getCondition() != null) {
            AbstractExpression conditionExpr = new AbstractExpression(
                    whileStatement.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    whileStatement.getCondition(),
                    LocationInfo.CodeElementType.WHILE_STATEMENT_CONDITION,
                    container
            );
            whileStatementObject.addExpression(conditionExpr);
        }

        // Process the loop body
        if (whileStatement.getBody() != null) {
            processStatement(whileStatement.getBody(), whileStatementObject, sourceFolder, filePath, container);
        }

        composite.addStatement(whileStatementObject);
    }

    public static void processForStatement(LangForStatement forStatement, CompositeStatementObject composite,
                                           String sourceFolder, String filePath, UMLOperation container) {
        // Create a CompositeStatementObject for the for statement
        CompositeStatementObject forStatementObject = new CompositeStatementObject(
                forStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                forStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.FOR_STATEMENT
        );

        // Process the initializers (loop variables)
        if (forStatement.getInitializers() != null) {
            for (LangSingleVariableDeclaration initializer : forStatement.getInitializers()) {
                AbstractExpression initializerExpr = new AbstractExpression(
                        forStatement.getRootCompilationUnit(),
                        sourceFolder,
                        filePath,
                        initializer,
                        LocationInfo.CodeElementType.FOR_STATEMENT_INITIALIZER,
                        container
                );
                forStatementObject.addExpression(initializerExpr);
            }
        }


        // Process the condition expression (for traditional for loops)
        if (forStatement.getCondition() != null) {
            AbstractExpression conditionExpr = new AbstractExpression(
                    forStatement.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    forStatement.getCondition(),
                    LocationInfo.CodeElementType.FOR_STATEMENT_CONDITION,
                    container
            );
            forStatementObject.addExpression(conditionExpr);
        }

        // Process the update expressions
        if (forStatement.getUpdates() != null) {
            for (LangASTNode update : forStatement.getUpdates()) {
                AbstractExpression updateExpr = new AbstractExpression(
                        forStatement.getRootCompilationUnit(),
                        sourceFolder,
                        filePath,
                        update,
                        LocationInfo.CodeElementType.FOR_STATEMENT_UPDATER,
                        container
                );
                forStatementObject.addExpression(updateExpr);
            }
        }


        // Process the loop body
        if (forStatement.getBody() != null) {
            processStatement(forStatement.getBody(), forStatementObject, sourceFolder, filePath, container);
        }

        // Process the else body (Python for-else construct)
        if (forStatement.getElseBody() != null) {
            processStatement(forStatement.getElseBody(), forStatementObject, sourceFolder, filePath, container);
        }

        // Add the for statement to the parent composite
        composite.addStatement(forStatementObject);
    }


    public static void processSwitchStatement(LangSwitchStatement switchStatement, CompositeStatementObject composite,
                                              String sourceFolder, String filePath, UMLOperation container) {
        // Create a CompositeStatementObject for the switch statement
        CompositeStatementObject switchStatementObject = new CompositeStatementObject(
                switchStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                switchStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.SWITCH_STATEMENT
        );

        // Process the switch expression
        if (switchStatement.getExpression() != null) {
            AbstractExpression switchExpr = new AbstractExpression(
                    switchStatement.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    switchStatement.getExpression(),
                    LocationInfo.CodeElementType.SWITCH_STATEMENT_CONDITION,
                    container
            );
            switchStatementObject.addExpression(switchExpr);
        }


        // Process each case statement
        if (switchStatement.getCases() != null) {
            for (LangASTNode caseStmt : switchStatement.getCases()) {
                processStatement(caseStmt, switchStatementObject, sourceFolder, filePath, container);
            }
        }

        // Add the switch statement to the parent composite
        composite.addStatement(switchStatementObject);
    }


    public static void processTryStatement(LangTryStatement tryStatement, CompositeStatementObject composite,
                                           String sourceFolder, String filePath, UMLOperation container) {
        // Create a TryStatementObject (specialized composite for try statements)
        TryStatementObject tryStatementObject = new TryStatementObject(
                tryStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                tryStatement,
                composite.getDepth() + 1
        );

        // Process the try block
        if (tryStatement.getBody() != null) {
            processStatement(tryStatement.getBody(), tryStatementObject, sourceFolder, filePath, container);
            // Set try container for statements in the try block
            setTryContainerForStatements(tryStatementObject, tryStatementObject);
        }

        // Process catch clauses
        if (tryStatement.getCatchClauses() != null) {
            for (LangCatchClause catchClause : tryStatement.getCatchClauses()) {
                CompositeStatementObject catchStatementObject = new CompositeStatementObject(
                        catchClause.getRootCompilationUnit(),
                        sourceFolder,
                        filePath,
                        catchClause,
                        tryStatementObject.getDepth() + 1,
                        LocationInfo.CodeElementType.CATCH_CLAUSE
                );

                if (catchClause.getBody() != null) {
                    processStatement(catchClause.getBody(), catchStatementObject, sourceFolder, filePath, container);
                    // Set try container for statements in the catch block
                    setTryContainerForStatements(catchStatementObject, tryStatementObject);
                }

                tryStatementObject.addCatchClause(catchStatementObject);
            }
        }

        // Process finally block
        if (tryStatement.getFinallyBlock() != null) {
            CompositeStatementObject finallyStatementObject = new CompositeStatementObject(
                    tryStatement.getFinallyBlock().getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    tryStatement.getFinallyBlock(),
                    tryStatementObject.getDepth() + 1,
                    LocationInfo.CodeElementType.FINALLY_BLOCK
            );

            processStatement(tryStatement.getFinallyBlock(), finallyStatementObject, sourceFolder, filePath, container);
            // Set try container for statements in the finally block
            setTryContainerForStatements(finallyStatementObject, tryStatementObject);
            tryStatementObject.setFinallyClause(finallyStatementObject);
        }

        composite.addStatement(tryStatementObject);
    }

    private static void setTryContainerForStatements(CompositeStatementObject composite, TryStatementObject tryContainer) {
        composite.setTryContainer(tryContainer);
        for (AbstractStatement statement : composite.getStatements()) {
            if (statement instanceof CompositeStatementObject) {
                setTryContainerForStatements((CompositeStatementObject) statement, tryContainer);
            }
        }
    }

    public static void processBreakStatement(LangBreakStatement breakStatement, CompositeStatementObject composite,
                                             String sourceFolder, String filePath, UMLOperation container) {
        // Create a simple statement object for break
        StatementObject breakStmt = new StatementObject(
                breakStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                breakStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.BREAK_STATEMENT,
                container
        );
        composite.addStatement(breakStmt);
    }

    public static void processContinueStatement(LangContinueStatement continueStatement, CompositeStatementObject composite,
                                                String sourceFolder, String filePath, UMLOperation container) {
        // Create a simple statement object for continue
        StatementObject continueStmt = new StatementObject(
                continueStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                continueStatement,
                composite.getDepth() + 1, // depth
                LocationInfo.CodeElementType.CONTINUE_STATEMENT,
                container
        );
        composite.addStatement(continueStmt);
    }

    public static void processPassStatement(LangPassStatement passStatement,
                                            CompositeStatementObject composite,
                                            String sourceFolder, String filePath,
                                            UMLOperation container) {

        // Create a simple StatementObject for the pass statement
        StatementObject stmt = new StatementObject(
                passStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                passStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.EMPTY_STATEMENT,
                container
        );

        composite.addStatement(stmt);
    }

    public static void processWithStatement(LangWithStatement withStatement,
                                            CompositeStatementObject composite,
                                            String sourceFolder, String filePath,
                                            UMLOperation container) {


        // Create composite statement for the with block
        CompositeStatementObject withComposite = new CompositeStatementObject(
                withStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                withStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.BLOCK
        );

        // Process context items (the resources being managed)
        List<LangASTNode> contextItems = withStatement.getContextItems();
        for (LangASTNode contextItem : contextItems) {
            // Create expressions for context managers
            AbstractExpression contextExpr = new AbstractExpression(
                    withStatement.getRootCompilationUnit(),
                    sourceFolder,
                    filePath,
                    contextItem,
                    LocationInfo.CodeElementType.VARIABLE_DECLARATION_STATEMENT,
                    container
            );
            withComposite.addExpression(contextExpr);
        }

        // Process the body of the with statement
        LangBlock body = withStatement.getBody();
        if (body != null && body.getStatements() != null) {
            for (LangASTNode statement : body.getStatements()) {
                processStatement(statement, withComposite, sourceFolder, filePath, container);
            }
        }

        composite.addStatement(withComposite);
    }


    // Python-specific statements
    public static void processDelStatement(LangDelStatement delStatement, CompositeStatementObject composite,
                                           String sourceFolder, String filePath, UMLOperation container) {
        StatementObject delStmt = new StatementObject(
                delStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                delStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.EXPRESSION_STATEMENT,
                container
        );
        composite.addStatement(delStmt);
    }

    public static void processGlobalStatement(LangGlobalStatement globalStatement, CompositeStatementObject composite,
                                              String sourceFolder, String filePath, UMLOperation container) {
        StatementObject globalStmt = new StatementObject(
                globalStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                globalStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.VARIABLE_DECLARATION_STATEMENT,
                container
        );
        composite.addStatement(globalStmt);
    }

    public static void processAssertStatement(LangAssertStatement assertStatement, CompositeStatementObject composite,
                                              String sourceFolder, String filePath, UMLOperation container) {
        StatementObject assertStmt = new StatementObject(
                assertStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                assertStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.ASSERT_STATEMENT,
                container
        );
        composite.addStatement(assertStmt);
    }

    public static void processYieldStatement(LangYieldStatement yieldStatement, CompositeStatementObject composite,
                                             String sourceFolder, String filePath, UMLOperation container) {
        StatementObject yieldStmt = new StatementObject(
                yieldStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                yieldStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.YIELD_STATEMENT,
                container
        );
        composite.addStatement(yieldStmt);
    }

    public static void processThrowStatement(LangThrowStatement throwStatement, CompositeStatementObject composite,
                                             String sourceFolder, String filePath, UMLOperation container) {
        StatementObject throwStmt = new StatementObject(
                throwStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                throwStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.THROW_STATEMENT,
                container
        );
        composite.addStatement(throwStmt);
    }

    public static void processNonLocalStatement(LangNonLocalStatement nonLocalStatement, CompositeStatementObject composite,
                                                String sourceFolder, String filePath, UMLOperation container) {
        StatementObject nonLocalStmt = new StatementObject(
                nonLocalStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                nonLocalStatement,
                composite.getDepth() + 1,
                LocationInfo.CodeElementType.VARIABLE_DECLARATION_STATEMENT,
                container
        );
        composite.addStatement(nonLocalStmt);
    }

    public static void processAsyncStatement(LangAsyncStatement asyncStatement, CompositeStatementObject composite,
                                             String sourceFolder, String filePath, UMLOperation container) {

        LocationInfo.CodeElementType elementType;

        if (asyncStatement.getBody() instanceof LangForStatement) {
            elementType = LocationInfo.CodeElementType.ENHANCED_FOR_STATEMENT;
        } else if (asyncStatement.getBody() instanceof LangWithStatement){
            elementType = LocationInfo.CodeElementType.SYNCHRONIZED_STATEMENT;
        } else {
            elementType = LocationInfo.CodeElementType.BLOCK;
        }

        // Async statements are usually composite (async def, async for, async with)
        CompositeStatementObject asyncComposite = new CompositeStatementObject(
                asyncStatement.getRootCompilationUnit(),
                sourceFolder,
                filePath,
                asyncStatement,
                composite.getDepth() + 1,
                elementType
        );
        composite.addStatement(asyncComposite);

        // Process the wrapped statement
        if (asyncStatement.getBody() != null) {
            processStatement(asyncStatement.getBody(), asyncComposite, sourceFolder, filePath, container);
        }
    }

    //TODO
//    private static void processLambdaExpression(LangLambdaExpression statement, CompositeStatementObject composite, String sourceFolder, String filePath, UMLOperation container) {
//        // Create a LambdaExpressionObject for the lambda expression
//        LambdaExpressionObject lambdaExpressionObject = new LambdaExpressionObject(
//                statement.getRootCompilationUnit(),
//                sourceFolder,
//                filePath,
//                statement,
//                composite.getDepth() + 1,
//                LocationInfo.CodeElementType.LAMBDA_EXPRESSION,
//                container
//        )
//    }


}
