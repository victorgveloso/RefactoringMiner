package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CallTree {
	private CallTreeNode root;
	
	public CallTree(CallTreeNode root) {
		this.root = root;
	}
	
	public List<CallTreeNode> getNodesInBreadthFirstOrder() {
		List<CallTreeNode> nodes = new ArrayList<>();
		List<CallTreeNode> queue = new LinkedList<>();
		nodes.add(root);
		queue.add(root);
		while(!queue.isEmpty()) {
			CallTreeNode node = queue.remove(0);
			nodes.addAll(node.getChildren());
			queue.addAll(node.getChildren());
		}
		return nodes;
	}
	
	public boolean containsInPathToRootOrSibling(CallTreeNode parent, UMLOperation invokedOperation) {
		for(CallTreeNode sibling : parent.getChildren()) {
			if(sibling.getInvokedOperation().equals(invokedOperation)) {
				return true;
			}
		}
		CallTreeNode currentParent = parent;
		while(currentParent != null) {
			if(currentParent.getInvokedOperation().equals(invokedOperation)) {
				return true;
			}
			currentParent = currentParent.getParent();
		}
		return false;
	}
}
