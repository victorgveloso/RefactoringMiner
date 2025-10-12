package extension.base.treesitter.python;

import org.treesitter.TSNode;

public interface Printer {
    void parentBegin(TSNode node);

    void childBegin(TSNode node, String name);

    void childEnd(TSNode node, String name);

    void parentEnd(TSNode node);
}
