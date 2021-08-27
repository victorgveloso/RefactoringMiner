package gr.uom.java.xmi;

/**
 * Model representation of the Generalization relationship from UML
 */
public class UMLGeneralization implements Comparable<UMLGeneralization> {
	/**
	 * The subclass (more specific classifier)
	 */
    private UMLClass child;
	/**
	 * The superclass' name (more general classifier)
	 */
    private String parent;

    public UMLGeneralization(UMLClass child, String parent) {
        this.child = child;
        this.parent = parent;
    }

    public UMLClass getChild() {
        return child;
    }

	public String getParent() {
        return parent;
    }

	public boolean equals(Object o) {
    	if(this == o) {
    		return true;
    	}
    	
    	if(o instanceof UMLGeneralization) {
    		UMLGeneralization umlGeneralization = (UMLGeneralization)o;
    		return this.child.equals(umlGeneralization.child) &&
    			this.parent.equals(umlGeneralization.parent);
    	}
    	return false;
    }

    public String toString() {
    	return child + "->" + parent;
    }

	public int compareTo(UMLGeneralization generalization) {
		return this.toString().compareTo(generalization.toString());
	}
}
