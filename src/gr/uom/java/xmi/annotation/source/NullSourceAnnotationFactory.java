package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;

public class NullSourceAnnotationFactory implements SourceAnnotationFactory {

    @Override
    public String getTypeName() {
        return NullSourceAnnotation.ANNOTATION_TYPENAME;
    }

    @Override
    public SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        return new NullSourceAnnotation(annotation, operation, model);
    }
}
