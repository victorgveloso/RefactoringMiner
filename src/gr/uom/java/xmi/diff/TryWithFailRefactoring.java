package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.TryStatementObject;

public interface TryWithFailRefactoring extends OperationRefactoring {
    TryStatementObject getTryStatement();

    AbstractCall getAssertFailInvocation();

    String getException();
}
