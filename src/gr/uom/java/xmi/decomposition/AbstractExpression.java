package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import java.util.List;
import java.util.Map;

public class AbstractExpression extends AbstractCodeFragment {

	@Getter private final String expression;
	@Getter private final LocationInfo locationInfo;
	@Getter @Setter private CompositeStatementObject owner;
	@Getter private final List<String> variables;
	@Getter private final List<String> types;
	@Getter private final List<VariableDeclaration> variableDeclarations;
	@Getter private final Map<String, List<AbstractCall>> methodInvocationMap;
	@Getter private final List<AnonymousClassDeclarationObject> anonymousClassDeclarations;
	@Getter private final List<String> stringLiterals;
	@Getter private final List<String> numberLiterals;
	@Getter private final List<String> nullLiterals;
	@Getter private final List<String> booleanLiterals;
	@Getter private final List<String> typeLiterals;
	@Getter private final Map<String, List<ObjectCreation>> creationMap;
	@Getter private final List<String> infixExpressions;
	@Getter private final List<String> infixOperators;
	@Getter private final List<String> arrayAccesses;
	@Getter private final List<String> prefixExpressions;
	@Getter private final List<String> postfixExpressions;
	@Getter private final List<String> arguments;
	@Getter private final List<TernaryOperatorExpression> ternaryOperatorExpressions;
	@Getter private final List<LambdaExpressionObject> lambdas;
    
    public AbstractExpression(CompilationUnit cu, String filePath, Expression expression, CodeElementType codeElementType) {
    	this.locationInfo = new LocationInfo(cu, filePath, expression, codeElementType);
    	SubMethodNodeVisitor visitor = new SubMethodNodeVisitor(cu, filePath);
    	expression.accept(visitor);
		this.variables = visitor.getVariables();
		this.types = visitor.getTypes();
		this.variableDeclarations = visitor.getVariableDeclarations();
		this.methodInvocationMap = visitor.getMethodInvocationMap();
		this.anonymousClassDeclarations = visitor.getAnonymousClassDeclarations();
		this.stringLiterals = visitor.getStringLiterals();
		this.numberLiterals = visitor.getNumberLiterals();
		this.nullLiterals = visitor.getNullLiterals();
		this.booleanLiterals = visitor.getBooleanLiterals();
		this.typeLiterals = visitor.getTypeLiterals();
		this.creationMap = visitor.getCreationMap();
		this.infixExpressions = visitor.getInfixExpressions();
		this.infixOperators = visitor.getInfixOperators();
		this.arrayAccesses = visitor.getArrayAccesses();
		this.prefixExpressions = visitor.getPrefixExpressions();
		this.postfixExpressions = visitor.getPostfixExpressions();
		this.arguments = visitor.getArguments();
		this.ternaryOperatorExpressions = visitor.getTernaryOperatorExpressions();
		this.lambdas = visitor.getLambdas();
    	this.expression = expression.toString();
    	this.owner = null;
    }

	@Override
	public CompositeStatementObject getParent() {
		return getOwner();
	}

	public String getString() {
    	return toString();
    }
  
	public String toString() {
		return getExpression();
	}

	public VariableDeclaration searchVariableDeclaration(String variableName) {
		VariableDeclaration variableDeclaration = this.getVariableDeclaration(variableName);
		if(variableDeclaration != null) {
			return variableDeclaration;
		}
		else if(owner != null) {
			return owner.searchVariableDeclaration(variableName);
		}
		return null;
	}

	public VariableDeclaration getVariableDeclaration(String variableName) {
		List<VariableDeclaration> variableDeclarations = getVariableDeclarations();
		for(VariableDeclaration declaration : variableDeclarations) {
			if(declaration.getVariableName().equals(variableName)) {
				return declaration;
			}
		}
		return null;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
