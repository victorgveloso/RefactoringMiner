package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.OperationInvocation;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;

import java.util.List;

/**
 * Detects expecting exception test encoding migration (from JUnit 4 to JUnit 5)
 * JUnit4 usually relies on @ExpectedException, a Single Member annotation, with an exception type as its only member
 * JUnit5 introduces the assertThrows method that expects both an exception type and a lambda function
 */
public class ExpectedExceptionToAssertThrowsDetection {
    private UMLOperationBodyMapper mapper;
    private List<OperationInvocation> operationInvocations;
    private UMLOperation operationBefore;
    private UMLOperation operationAfter;
}
