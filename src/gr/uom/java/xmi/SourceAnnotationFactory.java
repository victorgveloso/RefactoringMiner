package gr.uom.java.xmi;

public interface SourceAnnotationFactory<T> {
    String getTypeName();
    SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model);
}
