package gr.uom.java.xmi;

import gr.uom.java.xmi.annotation.source.*;
import org.apache.commons.lang3.function.TriFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.HashMap;

public abstract class SourceAnnotation {
    protected static final Map<String, TriFunction<UMLAnnotation, UMLOperation, UMLModel, SourceAnnotation>> implementations = new HashMap<>();
    protected List<List<String>> testParameters;
    protected UMLAnnotation annotation;

    static {
        loadSourceAnnotationProviders();
    }

    protected SourceAnnotation(UMLAnnotation annotation, String typeName) {
        assert annotation.getTypeName().equals(typeName) : "Annotation is not a " + typeName + " annotation";
        this.annotation = annotation;
        this.testParameters = new ArrayList<>();
    }

    public static SourceAnnotation create(UMLAnnotation annotation, UMLOperation operation, UMLModel model) {
        if (implementations.containsKey(annotation.getTypeName())) {
            return implementations.get(annotation.getTypeName()).apply(annotation, operation, model);
        }
        throw new IllegalArgumentException("Annotation type " + annotation.getTypeName() + " is not supported");
    }

    private static void loadSourceAnnotationProviders() {
        if (implementations.isEmpty()) {
            ServiceLoader<SourceAnnotationFactory> loader = ServiceLoader.load(SourceAnnotationFactory.class);
            for (SourceAnnotationFactory impl : loader) {
                if (impl != null) {
                    implementations.put(impl.getTypeName(), (a, o, m) -> impl.create(a,o,m));
                }
            }
        }
    }

    protected static String sanitizeLiteral(String literal) {
        literal = literal.startsWith("\"") ? literal.substring(1) : literal;
        literal = literal.endsWith("\"") ? literal.substring(0, literal.length() - 1) : literal;
        literal = literal.endsWith(".class") ? literal.substring(0, literal.length() - 6) : literal;
        return literal;
    }

    public abstract List<List<String>> getTestParameters();
}
