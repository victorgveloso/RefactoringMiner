package gr.uom.java.xmi.diff;

import org.eclipse.jdt.core.dom.IExtendedModifier;

public enum Modifier implements IExtendedModifier {
    FINAL("final"),
    ABSTRACT("abstract"),
    STATIC("static"),
    SYNCHRONIZED("synchronized");

    String mod;

    Modifier(String mod) {
        this.mod = mod;
    }

    @Override
    public boolean isModifier() {
        return true;
    }

    @Override
    public boolean isAnnotation() {
        return false;
    }
}
