package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;

public class CsvSourceAnnotationFactory implements SourceAnnotationFactory {

    @Override
    public String getTypeName() {
        return CsvSourceAnnotation.ANNOTATION_TYPENAME;
    }

    @Override
    public SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        return new CsvSourceAnnotation(annotation, operation, model);
    }
}
