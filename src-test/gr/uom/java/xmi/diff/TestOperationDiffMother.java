package gr.uom.java.xmi.diff;

public class TestOperationDiffMother {
    static String createExampleClassCode() {
        return "package ca.concordia.victor.exception;\n" +
                "\n" +
                "class ExampleClass {\n" +
                "    private int exampleField;\n" +
                "\n" +
                "    ExampleClass(int exampleField) {\n" +
                "        this.exampleField = exampleField;\n" +
                "    }\n" +
                "\n" +
                "    void exampleMethod(int guessingField) {\n" +
                "        if (guessingField != exampleField) {\n" +
                "            throw new IllegalArgumentException(\"Wrong guess!\");\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    static String createExampleClassTestCode(String exampleTestMethod) {
        return "package ca.concordia.victor.exception;\n" +
                "\n" +
                "import org.junit.Assert;\n" +
                "import org.junit.Before;\n" +
                "import org.junit.Rule;\n" +
                "import org.junit.Test;\n" +
                "import org.junit.rules.ExpectedException;\n" +
                "public class ExampleClassTest {\n" +
                "    private ExampleClass exampleObj;\n" +
                "    @Before\n" +
                "    public void setUp() {\n" +
                "        exampleObj = new ExampleClass(42);\n" +
                "    }\n" +
                exampleTestMethod + "\n" +
                "}";
    }

    static String createExampleTestMethod_TryCatchVersion() {
        return "    @Test\n" +
                "    public void testExampleMethod_WrongGuess() {\n" +
                "        try {\n" +
                "            exampleObj.exampleMethod(0);\n" +
                "            Assert.fail(\"IllegalArgumentException expected\");\n" +
                "        } catch (IllegalArgumentException ignored) {}\n" +
                "    }";
    }

    static String createExampleTestMethod_InlineVersion() {
        return "    @Test(expected = IllegalArgumentException.class)\n" +
                "    public void testExampleMethod_WrongGuess() {\n" +
                "        exampleObj.exampleMethod(0);\n" +
                "    }";
    }

    static String createExampleTestMethod_RuleVersion() {
        return "    @Rule public ExpectedException thrown = ExpectedException.none();\n" +
                "    @Test\n" +
                "    public void testExampleMethod_WrongGuess() {\n" +
                "        thrown.expect(IllegalArgumentException.class);\n" +
                "        exampleObj.exampleMethod(0);\n" +
                "    }";
    }

    static String createExampleTestMethod_AssertVersion() {
        return "    @Test\n" +
                "    public void testExampleMethod_WrongGuess() {\n" +
                "        Assert.assertThrows(IllegalArgumentException.class, () -> {\n" +
                "            exampleObj.exampleMethod(0);\n" +
                "        });\n" +
                "    }";
    }

    static String createExampleTestMethod_CorrectGuess() {
        return "    @Test\n" +
                "    public void testExampleMethod_CorrectGuess() {\n" +
                "        exampleObj.exampleMethod(42);\n" +
                "    }";
    }

    static String createExampleTestClass_InlineVersion2() {
        return "package uk.gov.hmcts.reform.cwrdapi.service.impl;\n" +
                "\n" +
                "public class CaseWorkerServiceImplTest {\n" +
                "    @Test(expected = InvalidRequestException.class)\n" +
                "    public void testInvalidRequestExceptionForInvalidSortColumn() {\n" +
                "        validateAndBuildPaginationObject(0, 1,\n" +
                "                \"invalid\", \"ASC\",\n" +
                "                20, \"invalid\", CaseWorkerProfile.class);\n" +
                "    }\n" +
                "    @Test(expected = StaffReferenceException.class)\n" +
                "    public void testRefreshRoleAllocationWhenLrdResponseReturns400() throws JsonProcessingException {\n" +
                "        ErrorResponse errorResponse = ErrorResponse\n" +
                "                .builder()\n" +
                "                .errorCode(400)\n" +
                "                .errorDescription(\"testErrorDesc\")\n" +
                "                .errorMessage(\"testErrorMsg\")\n" +
                "                .build()\n" +
                "                ;\n" +
                "        String body = mapper.writeValueAsString(errorResponse);\n" +
                "\n" +
                "        when(locationReferenceDataFeignClient.getLocationRefServiceMapping(\"cmc\"))\n" +
                "                .thenReturn(Response.builder()\n" +
                "                        .request(mock(Request.class)).body(body, defaultCharset()).status(400).build());\n" +
                "        PageRequest pageRequest = RequestUtils.validateAndBuildPaginationObject(0, 1,\n" +
                "                \"caseWorkerId\", \"ASC\",\n" +
                "                20, \"id\", CaseWorkerProfile.class);\n" +
                "\n" +
                "        caseWorkerServiceImpl\n" +
                "                .fetchStaffProfilesForRoleRefresh(\"cmc\", pageRequest);\n" +
                "    }\n" +
                "}";
    }

    static String createExampleTestClass_AssertVersion2() {
        return "package uk.gov.hmcts.reform.cwrdapi.service.impl;\n" +
                "\n" +
                "public class CaseWorkerServiceImplTest {\n" +
                "    @Test\n" +
                "    public void testInvalidRequestExceptionForInvalidSortColumn() {\n" +
                "        Assertions.assertThrows(Exception.class, () -> {\n" +
                "            validateAndBuildPaginationObject(0, 1,\n" +
                "                    \"invalid\", \"ASC\",\n" +
                "                    20, \"invalid\", CaseWorkerProfile.class);\n" +
                "        });\n" +
                "    }\n" +
                "    @Test\n" +
                "    public void testRefreshRoleAllocationWhenLrdResponseReturns400() throws JsonProcessingException {\n" +
                "        ErrorResponse errorResponse = ErrorResponse\n" +
                "                .builder()\n" +
                "                .errorCode(400)\n" +
                "                .errorDescription(\"testErrorDesc\")\n" +
                "                .errorMessage(\"testErrorMsg\")\n" +
                "                .build();\n" +
                "        String body = mapper.writeValueAsString(errorResponse);\n" +
                "\n" +
                "        when(locationReferenceDataFeignClient.getLocationRefServiceMapping(\"cmc\"))\n" +
                "                .thenReturn(Response.builder()\n" +
                "                        .request(mock(Request.class)).body(body, defaultCharset()).status(400).build());\n" +
                "        PageRequest pageRequest = RequestUtils.validateAndBuildPaginationObject(0, 1,\n" +
                "                \"caseWorkerId\", \"ASC\",\n" +
                "                20, \"id\", CaseWorkerProfile.class);\n" +
                "\n" +
                "        Assertions.assertThrows(StaffReferenceException.class, () -> {\n" +
                "            caseWorkerServiceImpl\n" +
                "                    .fetchStaffProfilesForRoleRefresh(\"cmc\", pageRequest);\n" +
                "        });\n" +
                "    }\n" +
                "}";
    }
}
