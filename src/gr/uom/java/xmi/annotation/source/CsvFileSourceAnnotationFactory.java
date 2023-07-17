package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.*;

public class CsvFileSourceAnnotationFactory implements SourceAnnotationFactory {

    @Override
    public String getTypeName() {
        return CsvFileSourceAnnotation.ANNOTATION_TYPENAME;
    }

    @Override
    public SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        return new CsvFileSourceAnnotation(annotation, operation, model);
    }
}
