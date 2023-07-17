package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;

public class NullAndEmptySourceAnnotationFactory implements SourceAnnotationFactory {

    @Override
    public String getTypeName() {
        return NullAndEmptySourceAnnotation.ANNOTATION_TYPENAME;
    }

    @Override
    public SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        return new NullAndEmptySourceAnnotation(annotation, operation, model);
    }
}
