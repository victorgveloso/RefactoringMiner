package extension.ast.builder.python.component;

import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.ast.node.LangASTNodeFactory;
import extension.ast.node.PositionInfo;
import extension.ast.node.PositionUtils;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.statement.LangImportStatement;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangSupportedEnum;
import extension.base.lang.python.Python3Parser;

import java.util.ArrayList;
import java.util.List;

public class PyCompilationUnitASTBuilder extends PyBaseASTBuilder {

    public PyCompilationUnitASTBuilder(PyASTBuilder mainBuilder) {
        super(mainBuilder);
    }

    public LangASTNode visitFile_input(Python3Parser.File_inputContext ctx) {
        LangCompilationUnit compilationUnit = LangASTNodeFactory.createCompilationUnit(ctx);
        compilationUnit.setLanguage(LangSupportedEnum.PYTHON);

        // Process each statement in the file
        for (Python3Parser.StmtContext stmtCtx : ctx.stmt()) {
            LangASTNode stmt = mainBuilder.visit(stmtCtx);

            if (stmt == null) continue;

            // Sort statements by type
            if (stmt instanceof LangTypeDeclaration) {
                compilationUnit.addType((LangTypeDeclaration) stmt);
            } else if (stmt instanceof LangMethodDeclaration) {
                compilationUnit.addMethod((LangMethodDeclaration) stmt);
            } else if (stmt instanceof LangComment){
                compilationUnit.addComment((LangComment) stmt);
            } else if (stmt instanceof LangImportStatement){
                compilationUnit.addImport((LangImportStatement) stmt);
            } else {
                compilationUnit.addStatement(stmt);
            }

        }

        return compilationUnit;
    }


    public LangASTNode visitImport_stmt(Python3Parser.Import_stmtContext ctx) {
        PositionInfo positionInfo = PositionUtils.getPositionInfo(ctx);

        List<LangImportStatement> importStatements = new ArrayList<>();

        if (ctx.import_name() != null) {
            // Handle regular import statements like "import module" or "import module.submodule as alias"
            Python3Parser.Import_nameContext importNameCtx = ctx.import_name();
            Python3Parser.Dotted_as_namesContext dottedAsNames = importNameCtx.dotted_as_names();

            for (Python3Parser.Dotted_as_nameContext dottedAsName : dottedAsNames.dotted_as_name()) {
                // Get the full module path (e.g., "module.submodule")
                String modulePath = dottedAsName.dotted_name().getText();

                // Get the alias if present
                String alias = null;
                if (dottedAsName.AS() != null && dottedAsName.name() != null) {
                    alias = dottedAsName.name().getText();
                }

                // Create import statement
                LangImportStatement importStmt = LangASTNodeFactory.createImportStatement(modulePath, alias, positionInfo);

                // If we're in a compilation unit, add it there
                if (importStmt.getRootCompilationUnit() != null) {
                    importStmt.getRootCompilationUnit().addImport(importStmt);
                }

                // Track all created import statements
                importStatements.add(importStmt);
            }
        } else if (ctx.import_from() != null) {
            // Handle from-import statements like "from module import name" or "from module import *"
            Python3Parser.Import_fromContext importFromCtx = ctx.import_from();

            // Get the module from which to import (could be null for relative imports like "from . import name")
            String fromModule = null;
            if (importFromCtx.dotted_name() != null) {
                fromModule = importFromCtx.dotted_name().getText();
            }

            // Handle relative imports (dots before module name)
            int relativeLevel = 0;
            if (importFromCtx.DOT() != null && !importFromCtx.DOT().isEmpty()) {
                relativeLevel = importFromCtx.DOT().size();
            } else if (importFromCtx.ELLIPSIS() != null) {
                relativeLevel = 3; // ... is 3 dots
            }

            // Handle imports with specific names or *
            if (importFromCtx.import_as_names() != null) {
                // Import specific names, possibly with aliases
                for (Python3Parser.Import_as_nameContext importAsName : importFromCtx.import_as_names().import_as_name()) {
                    String importedName = importAsName.name(0).getText();
                    String alias = null;
                    if (importAsName.AS() != null && importAsName.name().size() > 1) {
                        alias = importAsName.name(1).getText();
                    }

                    LangImportStatement importStmt = new LangImportStatement(fromModule, importedName, alias, relativeLevel, positionInfo);

                    if (importStmt.getRootCompilationUnit() != null) {
                        importStmt.getRootCompilationUnit().addImport(importStmt);
                    }

                    importStatements.add(importStmt);
                }
            } else if (importFromCtx.STAR() != null) {
                // Import all (*) from module
                LangImportStatement importStmt = new LangImportStatement(fromModule, "*", null, relativeLevel, positionInfo);

                if (importStmt.getRootCompilationUnit() != null) {
                    importStmt.getRootCompilationUnit().addImport(importStmt);
                }

                importStatements.add(importStmt);
            }
        }

        // Return the last import statement created, or null if no imports were processed
        if (!importStatements.isEmpty()) {
            return importStatements.get(importStatements.size() - 1);
        }


        return null;
    }

    // TODO: Handle comments

}
