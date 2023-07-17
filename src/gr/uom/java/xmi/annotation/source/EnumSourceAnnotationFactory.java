package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;

public class EnumSourceAnnotationFactory implements SourceAnnotationFactory {

    @Override
    public String getTypeName() {
        return EnumSourceAnnotation.ANNOTATION_TYPENAME;
    }

    @Override
    public SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        return new EnumSourceAnnotation(annotation, operation, model);
    }
}
