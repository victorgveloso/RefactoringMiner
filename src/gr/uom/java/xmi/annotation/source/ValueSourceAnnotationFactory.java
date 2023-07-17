package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;

public class ValueSourceAnnotationFactory implements SourceAnnotationFactory {

    @Override
    public String getTypeName() {
        return ValueSourceAnnotation.ANNOTATION_TYPENAME;
    }

    @Override
    public SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        return new ValueSourceAnnotation(annotation, operation, model);
    }
}
