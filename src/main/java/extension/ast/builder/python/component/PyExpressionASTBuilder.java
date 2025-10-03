package extension.ast.builder.python.component;

import extension.ast.builder.python.PyASTBuilder;
import extension.ast.builder.python.PyASTBuilderUtil;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.OperatorEnum;
import extension.ast.node.expression.LangAssignment;
import extension.ast.node.expression.LangMethodInvocation;
import extension.ast.node.literal.LangDictionaryLiteral;
import extension.ast.node.literal.LangStringLiteral;
import extension.base.lang.python.Python3Parser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

public class PyExpressionASTBuilder extends PyBaseASTBuilder {

    public PyExpressionASTBuilder(PyASTBuilder mainBuilder) {
        super(mainBuilder);
    }

    public LangASTNode visitAtom(Python3Parser.AtomContext ctx) {
        // Handle numbers
        if (ctx.NUMBER() != null) {
            return LangASTNodeFactory.createNumberLiteral(ctx, ctx.NUMBER().getText());
        }

        // Handle None
        if (ctx.name() != null && "None".equals(ctx.name().getText())) {
            return LangASTNodeFactory.createNullLiteral(ctx);
        }
        if ("None".equals(ctx.getText())) {
            return LangASTNodeFactory.createNullLiteral(ctx);
        }

        // Handle ALL string literals
        if (ctx.getText() != null && PyASTBuilderUtil.isStringLiteral(ctx)) {
            return LangASTNodeFactory.createStringLiteral(ctx, ctx.getText());
        }

        // Handle boolean literals
        if (ctx.TRUE() != null || ctx.FALSE() != null) {
            return LangASTNodeFactory.createBooleanLiteral(ctx, Boolean.parseBoolean(ctx.getText()));
        }

        // Handle empty dictionary {} - FIXED
        if (ctx.OPEN_BRACE() != null && ctx.CLOSE_BRACE() != null) {
            if (ctx.dictorsetmaker() == null) {
                // Empty dictionary
                return LangASTNodeFactory.createDictionaryLiteral(ctx);
            } else {
                // Non-empty dictionary
                LangDictionaryLiteral dict = LangASTNodeFactory.createDictionaryLiteral(ctx);
                Python3Parser.DictorsetmakerContext dictCtx = ctx.dictorsetmaker();
                if (dictCtx.test().size() % 2 == 0) {
                    for (int i = 0; i < dictCtx.test().size(); i += 2) {
                        LangASTNode key = mainBuilder.visit(dictCtx.test(i));
                        LangASTNode value = mainBuilder.visit(dictCtx.test(i + 1));
                        dict.addEntry(key, value);
                    }
                }
                return dict;
            }
        }

        // Handle list literals
        if (ctx.getText() != null && PyASTBuilderUtil.isListLiteral(ctx)) {
            List<LangASTNode> elements = new ArrayList<>();
            if (ctx.testlist_comp() != null) {
                for (Python3Parser.TestContext testCtx : ctx.testlist_comp().test()) {
                    LangASTNode element = mainBuilder.visit(testCtx);
                    elements.add(element != null ? element :
                            LangASTNodeFactory.createSimpleName("PARSE_ERROR", testCtx));
                }
            }
            return LangASTNodeFactory.createListLiteral(ctx, elements);
        }

        // Handle parenthesized expressions and tuples
        if (ctx.OPEN_PAREN() != null && ctx.CLOSE_PAREN() != null) {
            if (ctx.testlist_comp() != null) {
                List<Python3Parser.TestContext> tests = ctx.testlist_comp().test();
                if (tests != null && !tests.isEmpty()) {
                    if (tests.size() == 1) {
                        // Single parenthesized expression
                        return mainBuilder.visit(tests.get(0));
                    } else {
                        // Tuple literal
                        List<LangASTNode> elements = new ArrayList<>();
                        for (Python3Parser.TestContext testCtx : tests) {
                            LangASTNode element = mainBuilder.visit(testCtx);
                            elements.add(element != null ? element :
                                    LangASTNodeFactory.createSimpleName("PARSE_ERROR", testCtx));
                        }
                        return LangASTNodeFactory.createTupleLiteral(ctx, elements);
                    }
                }
            }
            // Empty parentheses ()
            return LangASTNodeFactory.createTupleLiteral(ctx, new ArrayList<>());
        }

        // Handle regular identifiers/names
        if (ctx.name() != null) {
            return LangASTNodeFactory.createSimpleName(ctx.name().getText(), ctx);
        }

        // Fallback
        String atomText = ctx.getText();
        if (atomText != null && !atomText.isEmpty()) {
            System.err.println("Warning: Unhandled atom type, creating SimpleName fallback: " + atomText);
            System.err.println("Context: " + ctx.getText());
            return LangASTNodeFactory.createSimpleName(atomText, ctx);
        }

        System.err.println("Warning: Empty atom context, creating placeholder");
        return LangASTNodeFactory.createSimpleName("UNKNOWN_ATOM", ctx);
    }

    public LangASTNode visitAtom_expr(Python3Parser.Atom_exprContext ctx) {
        // Handle the base atom (which could be a function name like "print")
        LangASTNode baseExpr = mainBuilder.visit(ctx.atom());

        // If there are no trailers, just return the atom
        if (ctx.trailer().isEmpty()) {
            return baseExpr;
        }

        if (ctx.AWAIT() != null) {
            LangASTNode innerExpr = mainBuilder.visit(ctx.atom());
            return LangASTNodeFactory.createAwaitExpression(ctx, innerExpr);
        }

        // Process trailers (method calls, attribute access, etc.)
        LangASTNode result = baseExpr;
        for (Python3Parser.TrailerContext trailerCtx : ctx.trailer()) {
            if (trailerCtx.OPEN_PAREN() != null) {
                // This is a function call like print(i) or a method call like obj.method()
                LangMethodInvocation methodInvocation = LangASTNodeFactory.createMethodInvocation(ctx.getParent());

                // Set the expression - this is the object on which the method is called
                methodInvocation.setExpression(result);

                // Process arguments if they exist
                if (trailerCtx.arglist() != null) {
                    for (Python3Parser.ArgumentContext argCtx : trailerCtx.arglist().argument()) {
                        // Visit each argument and add it to the method invocation
                        LangASTNode argNode = mainBuilder.visit(argCtx);
                        if (argNode != null) {
                            methodInvocation.addArgument(argNode);
                        }
                    }
                }

                result = methodInvocation;
            } else if (trailerCtx.DOT() != null && trailerCtx.name() != null) {
                // This is attribute access: obj.attr
                String attrName = trailerCtx.name().getText();

                // Create a field access node that combines the object and the field name
                result = LangASTNodeFactory.createFieldAccess(result, attrName, trailerCtx);
            }
        }
        return result;
    }

    public LangASTNode visitStar_expr(Python3Parser.Star_exprContext ctx) {
        LangASTNode expr = mainBuilder.visit(ctx.expr());
        return LangASTNodeFactory.createPrefixExpression(expr, OperatorEnum.fromSymbol("*").getSymbol(), ctx);
    }


    public LangASTNode visitTestlist_star_expr(Python3Parser.Testlist_star_exprContext ctx) {
        // Handle single element case
        if (ctx.test().size() == 1 && ctx.star_expr().isEmpty()) {
            return mainBuilder.visit(ctx.test(0));
        }

        // Handle multiple elements or star expressions - create a list/tuple
        List<LangASTNode> elements = new ArrayList<>();

        // Add regular test expressions
        for (Python3Parser.TestContext testCtx : ctx.test()) {
            elements.add(mainBuilder.visit(testCtx));
        }

        // Add star expressions
        for (Python3Parser.Star_exprContext starCtx : ctx.star_expr()) {
            elements.add(mainBuilder.visit(starCtx));
        }

        if (elements.size() > 1) {
            return LangASTNodeFactory.createTupleLiteral(ctx, elements);
        }

        // Single element
        return elements.get(0);
    }

    public LangASTNode visitExpr_stmt(Python3Parser.Expr_stmtContext ctx) {
        if (ctx.ASSIGN().isEmpty() && ctx.augassign() == null) {
            LangASTNode expr = mainBuilder.visit(ctx.testlist_star_expr(0));

            if (expr instanceof LangStringLiteral && isModuleLevelDocstring(ctx)) {
                return LangASTNodeFactory.createComment(ctx, ((LangStringLiteral) expr).getValue(), false, true);
            }

            return LangASTNodeFactory.createExpressionStatement(expr, ctx);
        }

        // Augmented assignment (+=, -=, etc.)
        if (ctx.augassign() != null) {
            LangASTNode left = mainBuilder.visit(ctx.testlist_star_expr(0));

            // Grammar: augassign (yield_expr | testlist)
            LangASTNode right = null;
            if (ctx.yield_expr() != null && !ctx.yield_expr().isEmpty()) {
                right = mainBuilder.visit(ctx.yield_expr(0));
            } else if (ctx.testlist() != null) {
                right = mainBuilder.visit(ctx.testlist());
            }

            if (left == null) left = LangASTNodeFactory.createSimpleName("UNKNOWN_LEFT", ctx);
            if (right == null) right = LangASTNodeFactory.createSimpleName("UNKNOWN_RIGHT", ctx);

            String operator = ctx.augassign().getText();
            LangAssignment assignment = LangASTNodeFactory.createAssignment(operator, left, right, ctx);
            return LangASTNodeFactory.createExpressionStatement(assignment, ctx);
        }


        List<Python3Parser.Testlist_star_exprContext> parts = ctx.testlist_star_expr();
        int count = parts != null ? parts.size() : 0;

        if (count == 1 && ctx.yield_expr() != null && !ctx.yield_expr().isEmpty()) {
            LangASTNode left = mainBuilder.visit(parts.get(0));
            LangASTNode right = mainBuilder.visit(ctx.yield_expr(0));

            if (left == null) left = LangASTNodeFactory.createSimpleName("UNKNOWN_LEFT", ctx);
            if (right == null) right = LangASTNodeFactory.createSimpleName("UNKNOWN_RIGHT", ctx);

            LangAssignment assignment = LangASTNodeFactory.createAssignment("=", left, right, ctx);
            return LangASTNodeFactory.createExpressionStatement(assignment, ctx);
        }

        if (count >= 2 && (ctx.yield_expr() == null || ctx.yield_expr().isEmpty())) {
            LangASTNode right = mainBuilder.visit(parts.get(count - 1));
            for (int i = count - 2; i >= 0; i--) {
                LangASTNode left = mainBuilder.visit(parts.get(i));
                if (left == null) left = LangASTNodeFactory.createSimpleName("UNKNOWN_LEFT", ctx);
                if (right == null) right = LangASTNodeFactory.createSimpleName("UNKNOWN_RIGHT", ctx);
                right = LangASTNodeFactory.createAssignment("=", left, right, ctx);
            }
            return LangASTNodeFactory.createExpressionStatement(right, ctx);
        }

        if (count >= 1 && ctx.yield_expr() != null && !ctx.yield_expr().isEmpty()) {
            LangASTNode right = mainBuilder.visit(ctx.yield_expr(0));
            if (right == null) right = LangASTNodeFactory.createSimpleName("UNKNOWN_RIGHT", ctx);

            for (int i = count - 1; i >= 0; i--) {
                LangASTNode left = mainBuilder.visit(parts.get(i));
                if (left == null) left = LangASTNodeFactory.createSimpleName("UNKNOWN_LEFT", ctx);
                right = LangASTNodeFactory.createAssignment("=", left, right, ctx);
            }
            return LangASTNodeFactory.createExpressionStatement(right, ctx);
        }

        LangASTNode fallbackExpr = null;
        if (count >= 1) {
            fallbackExpr = mainBuilder.visit(parts.get(0));
            if (fallbackExpr == null) fallbackExpr = LangASTNodeFactory.createSimpleName("UNKNOWN_EXPR", ctx);
        } else if (ctx.yield_expr() != null && !ctx.yield_expr().isEmpty()) {
            fallbackExpr = mainBuilder.visit(ctx.yield_expr(0));
            if (fallbackExpr == null) fallbackExpr = LangASTNodeFactory.createSimpleName("UNKNOWN_YIELD", ctx);
        }
        if (fallbackExpr != null) {
            return LangASTNodeFactory.createExpressionStatement(fallbackExpr, ctx);
        }

        System.err.println("Warning: Unhandled expr_stmt context, creating placeholder");
        LangASTNode placeholder = LangASTNodeFactory.createSimpleName("UNSUPPORTED_EXPR", ctx);
        return LangASTNodeFactory.createExpressionStatement(placeholder, ctx);
    }


    private boolean isModuleLevelDocstring(Python3Parser.Expr_stmtContext ctx) {
        ParserRuleContext parent = ctx.getParent();
        while (parent != null) {
            if (parent instanceof Python3Parser.FuncdefContext ||
                    parent instanceof Python3Parser.ClassdefContext) {
                return false; // Inside a function or class
            }
            if (parent instanceof Python3Parser.File_inputContext) {
                return true; // At module level
            }
            parent = parent.getParent();
        }
        return false;
    }

    public LangASTNode visitExpr(Python3Parser.ExprContext ctx) {
        if (ctx.atom_expr() != null) {
            return mainBuilder.visitAtom_expr(ctx.atom_expr());
        }

        if (ctx.expr().size() == 2) {
            LangASTNode leftNode = mainBuilder.visit(ctx.expr(0));
            LangASTNode rightNode = mainBuilder.visit(ctx.expr(1));
            String operator = PyASTBuilderUtil.extractOperator(ctx);
            return LangASTNodeFactory.createInfixExpression(leftNode, rightNode, operator, ctx);
        }

        return mainBuilder.visitChildren(ctx);
    }

    public LangASTNode visitComparison(Python3Parser.ComparisonContext ctx) {
        // If there's only one expr, just visit it
        if (ctx.expr().size() == 1) {
            return mainBuilder.visit(ctx.expr(0));
        }

        // If there are multiple expressions connected by comparison operators
        if (ctx.expr().size() >= 2) {
            LangASTNode leftNode = mainBuilder.visit(ctx.expr(0));

            // Check if the first expression failed to parse
            if (leftNode == null) {
                System.err.println("Warning: Left operand is null in comparison: " + ctx.getText());
                return null;
            }

            // For each comparison operator and right operand
            for (int i = 0; i < ctx.comp_op().size(); i++) {
                LangASTNode rightNode = mainBuilder.visit(ctx.expr(i + 1));

                // Check if the right operand is null
                if (rightNode == null) {
                    System.err.println("Warning: Right operand is null in comparison: " + ctx.getText());
                    return leftNode;
                }

                String operator = ctx.comp_op(i).getText();

                // Validate operator as well
                if (operator == null || operator.isEmpty()) {
                    System.err.println("Warning: Operator is null/empty in comparison: " + ctx.getText());
                    return leftNode;
                }

                // Create an infix expression for this comparison
                leftNode = LangASTNodeFactory.createInfixExpression(leftNode, rightNode, operator, ctx);
            }

            return leftNode;
        }

        return mainBuilder.visitChildren(ctx);
    }

    public LangASTNode visitPattern(Python3Parser.PatternContext ctx) {
        // If we have an as_pattern, visit it
        if (ctx.as_pattern() != null) {
            return mainBuilder.visit(ctx.as_pattern());
        }

        //TODO
//        if (ctx.or_pattern() != null) {
//            return mainBuilder.visit(ctx.or_pattern());
//        }

        // Should not occur, but as fallback
        return super.mainBuilder.visitPattern(ctx);
    }

}
