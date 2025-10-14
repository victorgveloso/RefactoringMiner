package extension.base.treesitter.python;

import extension.ast.node.LangASTNode;
import io.github.treesitter.jtreesitter.Node;

public class PyTSBuilder {
    private final Node rootNode;

    public PyTSBuilder(Node rootNode) {
        this.rootNode = rootNode;
        HackyVisitor hackyVisitor = new HackyVisitor();
        hackyVisitor.visit(rootNode, new HackyPrinter());
    }

    public PyTSBuilder(Node rootNode, Printer printer) {
        this.rootNode = rootNode;
        HackyVisitor.visit(rootNode, printer);
    }

    public LangASTNode build() {
        return null;
    }
}
