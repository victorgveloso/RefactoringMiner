package extension.base.treesitter.python;

import extension.ast.node.LangASTNode;
import org.treesitter.TSNode;

public class PyTSBuilder {
    private final TSNode rootNode;

    public PyTSBuilder(TSNode rootNode) {
        this.rootNode = rootNode;
        HackyVisitor hackyVisitor = new HackyVisitor();
        hackyVisitor.visit(rootNode, new HackyPrinter());
    }

    public PyTSBuilder(TSNode rootNode, Printer printer) {
        this.rootNode = rootNode;
        HackyVisitor.visit(rootNode, printer);
    }

    public LangASTNode build() {
        return null;
    }
}
