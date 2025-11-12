package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import extension.ast.node.expression.LangComprehensionExpression;
import extension.ast.node.expression.LangComprehensionExpression.LangComprehensionClause;
import extension.ast.node.unit.LangCompilationUnit;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;

public class ComprehensionExpression extends LeafExpression {
	private AbstractExpression expression;
	private AbstractExpression keyExpression;
	private AbstractExpression valueExpression;
	private List<ComprehensionClause> clauses;

	public ComprehensionExpression(LangCompilationUnit cu, String sourceFolder, String filePath, LangComprehensionExpression expression, VariableDeclarationContainer container, Map<String, Set<VariableDeclaration>> activeVariableDeclarations, String fileContent) {
		super(cu, sourceFolder, filePath, expression, CodeElementType.COMPREHENSION, container);
		if(expression.getExpression() != null) {
			this.expression = new AbstractExpression(cu, sourceFolder, filePath, expression.getExpression(), CodeElementType.COMPREHENSION_EXPRESSION, container, activeVariableDeclarations, fileContent);
		}
		if(expression.getKeyExpression() != null) {
			this.keyExpression = new AbstractExpression(cu, sourceFolder, filePath, expression.getKeyExpression(), CodeElementType.COMPREHENSION_KEY_EXPRESSION, container, activeVariableDeclarations, fileContent);
		}
		if(expression.getValueExpression() != null) {
			this.valueExpression = new AbstractExpression(cu, sourceFolder, filePath, expression.getValueExpression(), CodeElementType.COMPREHENSION_VALUE_EXPRESSION, container, activeVariableDeclarations, fileContent);
		}
		this.clauses = new ArrayList<>();
		for(LangComprehensionClause clause : expression.getClauses()) {
			clauses.add(new ComprehensionClause(cu, sourceFolder, filePath, clause, container, activeVariableDeclarations, fileContent));
		}
	}
}
