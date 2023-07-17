package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;

public class MethodSourceAnnotationFactory implements SourceAnnotationFactory {

    @Override
    public String getTypeName() {
        return MethodSourceAnnotation.ANNOTATION_TYPENAME;
    }

    @Override
    public SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        return new MethodSourceAnnotation(annotation, operation, model);
    }
}
