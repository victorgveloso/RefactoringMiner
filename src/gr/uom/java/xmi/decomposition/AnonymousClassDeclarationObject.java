package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.LocationInfoProvider;
import gr.uom.java.xmi.diff.CodeRange;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnonymousClassDeclarationObject implements LocationInfoProvider {
	private final LocationInfo locationInfo;
	private AnonymousClassDeclaration astNode;
	private final String astNodeString;
	private final List<String> variables = new ArrayList<>();
	private final List<String> types = new ArrayList<>();
	private final Map<String, List<AbstractCall>> methodInvocationMap = new LinkedHashMap<>();
	private final List<VariableDeclaration> variableDeclarations = new ArrayList<>();
	private final List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<>();
	private final List<String> stringLiterals = new ArrayList<>();
	private final List<String> numberLiterals = new ArrayList<>();
	private final List<String> nullLiterals = new ArrayList<>();
	private final List<String> booleanLiterals = new ArrayList<>();
	private final List<String> typeLiterals = new ArrayList<>();
	private final Map<String, List<ObjectCreation>> creationMap = new LinkedHashMap<>();
	private final List<String> infixExpressions = new ArrayList<>();
	private final List<String> infixOperators = new ArrayList<>();
	private final List<String> arrayAccesses = new ArrayList<>();
	private final List<String> prefixExpressions = new ArrayList<>();
	private final List<String> postfixExpressions = new ArrayList<>();
	private final List<String> arguments = new ArrayList<>();
	private final List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<>();
	private final List<LambdaExpressionObject> lambdas = new ArrayList<>();
	
	public AnonymousClassDeclarationObject(CompilationUnit cu, String filePath, AnonymousClassDeclaration anonymous) {
		this.locationInfo = new LocationInfo(cu, filePath, anonymous, CodeElementType.ANONYMOUS_CLASS_DECLARATION);
		this.astNode = anonymous;
		this.astNodeString = anonymous.toString();
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public AnonymousClassDeclaration getAstNode() {
		return astNode;
	}

	public void setAstNode(AnonymousClassDeclaration node) {
		this.astNode = node;
	}
	
	public String toString() {
		return astNodeString;
	}

	public Map<String, List<AbstractCall>> getMethodInvocationMap() {
		return this.methodInvocationMap;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public List<String> getTypes() {
		return types;
	}

	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	public List<String> getStringLiterals() {
		return stringLiterals;
	}

	public List<String> getNumberLiterals() {
		return numberLiterals;
	}

	public List<String> getNullLiterals() {
		return nullLiterals;
	}

	public List<String> getBooleanLiterals() {
		return booleanLiterals;
	}

	public List<String> getTypeLiterals() {
		return typeLiterals;
	}

	public Map<String, List<ObjectCreation>> getCreationMap() {
		return creationMap;
	}

	public List<String> getInfixExpressions() {
		return infixExpressions;
	}

	public List<String> getInfixOperators() {
		return infixOperators;
	}

	public List<String> getArrayAccesses() {
		return arrayAccesses;
	}

	public List<String> getPrefixExpressions() {
		return prefixExpressions;
	}

	public List<String> getPostfixExpressions() {
		return postfixExpressions;
	}

	public List<String> getArguments() {
		return this.arguments;
	}

	public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
		return ternaryOperatorExpressions;
	}

	public List<String> getVariables() {
		return variables;
	}

	public List<LambdaExpressionObject> getLambdas() {
		return lambdas;
	}

	public CodeRange codeRange() {
		return locationInfo.codeRange();
	}
}
