package extension.base.treesitter.python;

import io.github.treesitter.jtreesitter.Node;

import java.util.Objects;

public class HackyPrinter implements Printer {
    public void parentBegin(Node parent) {
        System.out.printf("%s", parent.getType());
    }

    public void parentEnd(Node parent) {
        if (parent.isNamed()) {
            System.out.print("}");
        }
    }

    public void childBegin(Node child, String field) {
        if (Objects.nonNull(field)) {
            System.out.printf("%s: ", field);
        }
    }

    public void childEnd(Node child, String field) {
        System.out.println(",");
    }
}
