package extension.base.treesitter.python;

import org.treesitter.TSNode;

import java.util.Objects;

public class HackyPrinter implements Printer {
    public void parentBegin(TSNode parent) {
        System.out.printf("%s", parent.getType());
    }

    public void parentEnd(TSNode parent) {
        if (parent.isNamed()) {
            System.out.print("}");
        }
    }

    public void childBegin(TSNode child, String field) {
        if (Objects.nonNull(field)) {
            System.out.printf("%s: ", field);
        }
    }

    public void childEnd(TSNode child, String field) {
        System.out.println(",");
    }
}
