package extension.base.treesitter.python;

import extension.base.LangASTUtil;
import org.apache.commons.io.IOUtils;
import org.treesitter.TSNode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class HackyVisitor {
    public static void visit(TSNode node, Printer printer) {
        printer.parentBegin(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            TSNode child = node.getChild(i);
            printer.childBegin(child, node.getFieldNameForChild(i));
            visit(child, printer);
            printer.childEnd(child, node.getFieldNameForChild(i));
        }
        printer.parentEnd(node);
    }

    public static void main(String[] args) {
        try (FileInputStream fis = new FileInputStream("code.py");
             FileOutputStream fos = new FileOutputStream("ast.txt");) {
            String code = IOUtils.toString(fis.readAllBytes(), "UTF-8");
            HackyFileWriter hackyFileWriter = new HackyFileWriter(fos, code);
            new PyTSBuilder(LangASTUtil.prepareTSNodeForTreeSitterPythonAST(code), hackyFileWriter);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
