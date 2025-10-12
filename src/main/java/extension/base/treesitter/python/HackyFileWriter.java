package extension.base.treesitter.python;

import org.treesitter.TSNode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

public class HackyFileWriter implements Printer {
    private final String code;
    int indent = 0;
    FileOutputStream fos;

    private void printf(String format, Object... args) {
        try {
            fos.write(format.formatted(args).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HackyFileWriter(FileOutputStream fos, String code) {
        this.fos = fos;
        this.code = code;
    }

    public void parentBegin(TSNode parent) {
        indent++;
        if (parent.isNamed()) {
            printf("%s", parent.getType());
        }
        else {
            printf("\"%s\"", parent.getType());
        }
        if (parent.getNamedChildCount() > 0) {
            printf(" {\n");
            for (int i = 0; i < indent; i++) {
                printf("\t");
            }
        }
        else if (Stream.of("identifier", "string", "comment", "integer", "float", "none").anyMatch(t -> t.equals(parent.getType()))) {
            printf("(%s)", code.substring(parent.getStartByte(), parent.getEndByte()));
        }
    }

    public void parentEnd(TSNode parent) {
        indent--;
        if (parent.getNamedChildCount() > 0) {
            for (int i = 0; i < indent; i++) {
                if (i == 0) {
                    printf("\n");
                }
                printf("\t");
            }
            printf("}");
        }
    }

    public void childBegin(TSNode child, String field) {
        if (Objects.nonNull(field)) {
            printf("\"%s\": ", field);
        }
    }

    public void childEnd(TSNode child, String field) {
        if (!child.getNextSibling().isNull()) {
            printf(",\n");
            for (int i = 0; i < indent; i++) {
                printf("\t");
            }
        }
    }
}
