package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;

public class ArgumentsSourceAnnotationFactory implements SourceAnnotationFactory {

    @Override
    public String getTypeName() {
        return ArgumentsSourceAnnotation.ANNOTATION_TYPENAME;
    }

    @Override
    public SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        return new ArgumentsSourceAnnotation(annotation);
    }
}
