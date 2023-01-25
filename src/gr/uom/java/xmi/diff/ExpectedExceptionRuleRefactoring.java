package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.AbstractCall;

public interface ExpectedExceptionRuleRefactoring extends OperationRefactoring {
    String getException();

    AbstractCall getThrownExpectInvocations();

    UMLAttribute getRuleFieldDeclaration();
}
