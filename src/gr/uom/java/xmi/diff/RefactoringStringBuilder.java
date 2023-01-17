package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import org.refactoringminer.api.Refactoring;

import java.util.Objects;

class RefactoringStringBuilder {
    StringBuilder sb = new StringBuilder();
    Refactoring ref;
    String umlNodeString;
    UMLOperation operation = null;

    RefactoringStringBuilder (Refactoring ref) {
        this.ref = ref;
        sb.append(ref.getName()).append("\t");
    }

    RefactoringStringBuilder (Refactoring ref, String umlNodeString) {
        this.ref = ref;
        sb.append(ref.getName()).append("\t");
        addNode(umlNodeString);
    }

    RefactoringStringBuilder addNode(Object umlNodeString) {
        this.umlNodeString = umlNodeString.toString();
        sb.append(umlNodeString);
        return this;
    }

    RefactoringStringBuilder inMethod(UMLOperation operation) {
        this.operation = operation;
        sb.append(" in method ");
        sb.append(operation);
        return this;
    }

    RefactoringStringBuilder fromClass() {
        if (Objects.isNull(operation)) {
            throw new IllegalStateException("This method requires a previous call to inMethod");
        }
        sb.append(" from class ");
        sb.append(operation.getClassName());
        return this;
    }

    String build() {
        return sb.toString();
    }
}
