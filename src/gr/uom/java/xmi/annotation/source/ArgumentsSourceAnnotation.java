package gr.uom.java.xmi.annotation.source;

import gr.uom.java.xmi.SourceAnnotation;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.annotation.SingleMemberAnnotation;

import java.util.List;

public class ArgumentsSourceAnnotation extends SourceAnnotation implements SingleMemberAnnotation {
    public static final String ANNOTATION_TYPENAME = "ArgumentsSource";
    public ArgumentsSourceAnnotation(UMLAnnotation annotation) {
        super(annotation, ANNOTATION_TYPENAME);
    }

    @Override
    public List<String> getValue() {
        return null;
    }

    @Override
    public List<List<String>> getTestParameters() {
        return null;
    }
}
