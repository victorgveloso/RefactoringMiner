package org.refactoringminer.test.python;

import extension.umladapter.UMLAdapterUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UMLAdapterUtilTest {

    @Test
    void testExtractSourceFolder() {
        assertEquals("src", UMLAdapterUtil.extractSourceFolder("src/mypkg/my_module.py"));
        assertEquals("tests", UMLAdapterUtil.extractSourceFolder("tests/utils/test_utils.py"));
        assertEquals("project", UMLAdapterUtil.extractSourceFolder("project/file.py"));
        assertEquals("", UMLAdapterUtil.extractSourceFolder("file.py")); // No folder
    }

    @Test
    void testExtractPackageName() {
        assertEquals("mypkg", UMLAdapterUtil.extractPackageName("src/mypkg/my_module.py"));
        assertEquals("mypkg.subpkg", UMLAdapterUtil.extractPackageName("src/mypkg/subpkg/mod.py"));
        assertEquals("utils", UMLAdapterUtil.extractPackageName("tests/utils/test_utils.py"));
        assertEquals("", UMLAdapterUtil.extractPackageName("src/my_module.py")); // No subpackage
        assertEquals("", UMLAdapterUtil.extractPackageName("file.py")); // No path
    }

    @Test
    void testExtractFilePath() {
        assertEquals("src/mypkg/my_module.py", UMLAdapterUtil.extractFilePath("src/mypkg/my_module.py"));
        assertEquals("file.py", UMLAdapterUtil.extractFilePath("file.py"));
        assertEquals("tests/utils/test_utils.py", UMLAdapterUtil.extractFilePath("tests\\utils\\test_utils.py")); // Handles backslash
    }
}