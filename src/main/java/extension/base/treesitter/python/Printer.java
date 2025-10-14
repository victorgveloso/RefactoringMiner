package extension.base.treesitter.python;

import io.github.treesitter.jtreesitter.Node;

public interface Printer {
    void parentBegin(Node node);

    void childBegin(Node node, String name);

    void childEnd(Node node, String name);

    void parentEnd(Node node);
}
