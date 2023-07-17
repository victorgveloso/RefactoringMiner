package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;

public class EmptySourceAnnotationFactory implements SourceAnnotationFactory {

    @Override
    public String getTypeName() {
        return EmptySourceAnnotation.ANNOTATION_TYPENAME;
    }

    @Override
    public SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        return new EmptySourceAnnotation(annotation, operation, model);
    }
}
