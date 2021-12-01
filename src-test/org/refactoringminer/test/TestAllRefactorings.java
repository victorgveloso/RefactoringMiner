package org.refactoringminer.test;

import org.junit.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Systems;

public class TestAllRefactorings {

	@Test
	public void testAllRefactorings() throws Exception {
		GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
		TestBuilder test = new TestBuilder(detector, "tmp1", Refactorings.All.getValue());
		RefactoringPopulator.feedRefactoringsInstances(Refactorings.All.getValue(), Systems.FSE.getValue(), test);
		test.assertExpectations(10636, 37, 356);
	}

	@Test
	public void testARefactoring_ReplaceExpectAnnotationWithAssertThrows() throws Exception {
		GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
		TestBuilder test = new TestBuilder(detector, "tmp1", Refactorings.All.getValue());
		test.project("https://github.com/hmcts/rd-caseworker-ref-api.git", "master")
				.atCommit("a48dc2b9f76b57da03a14af4b9c6103c69c04135")
				.containsOnly("Replace Expect Annotation With Assert Throws InvalidRequestException.class from method public testInvalidRequestExceptionForInvalidPageSize() : void in class uk.gov.hmcts.reform.cwrdapi.util.RequestUtilsTest",
						"Replace Expect Annotation With Assert Throws Exception.class from method public testAuditJsrWithException() : void in class uk.gov.hmcts.reform.cwrdapi.service.ValidationServiceFacadeTest",
						"Replace Expect Annotation With Assert Throws InvalidRequestException.class from method public createCaseWorkerProfilesShouldThrow400() : void in class uk.gov.hmcts.reform.cwrdapi.controllers.CaseWorkerRefUsersControllerTest",
						"Replace Expect Annotation With Assert Throws InvalidRequestException.class from method public fetchCaseworkersByIdShouldThrow400() : void in class uk.gov.hmcts.reform.cwrdapi.controllers.CaseWorkerRefUsersControllerTest",
						"Replace Expect Annotation With Assert Throws Exception.class from method public shouldProcessCaseWorkerFileFailure() : void in class uk.gov.hmcts.reform.cwrdapi.service.impl.CaseWorkerServiceFacadeImplTest",
						"Replace Expect Annotation With Assert Throws InvalidRequestException.class from method public testInvalidRequestExceptionForInvalidPageNumber() : void in class uk.gov.hmcts.reform.cwrdapi.util.RequestUtilsTest",
						"Replace Expect Annotation With Assert Throws IdamRolesMappingException.class from method public test_buildIdamRoleMappings_exception() : void in class uk.gov.hmcts.reform.cwrdapi.service.impl.CaseWorkerServiceImplTest",
						"Replace Expect Annotation With Assert Throws CaseworkerMessageFailedException.class from method public shouldThrowExceptionForConnectionIssues() : void in class uk.gov.hmcts.reform.cwrdapi.servicebus.TopicPublisherTest",
						"Replace Expect Annotation With Assert Throws InvalidRequestException.class from method public shouldThrowInvalidRequestExceptionForEmptyServiceName() : void in class uk.gov.hmcts.reform.cwrdapi.controllers.internal.StaffReferenceInternalControllerTest",
						"Replace Expect Annotation With Assert Throws StaffReferenceException.class from method public test_mapObjectToEmptyList() : void in class uk.gov.hmcts.reform.cwrdapi.util.JsonFeignResponseUtilTest",
						"Replace Expect Annotation With Assert Throws ExcelValidationException.class from method public testPreHandleWithNobody() : void in class uk.gov.hmcts.reform.cwrdapi.util.AuditInterceptorTest",
						"Replace Expect Annotation With Assert Throws InvalidRequestException.class from method public testInvalidRequestExceptionForInvalidSortDirection() : void in class uk.gov.hmcts.reform.cwrdapi.util.RequestUtilsTest",
						"Replace Expect Annotation With Assert Throws InvalidRequestException.class from method public should_throw_exception_for_invalid_request() : void in class uk.gov.hmcts.reform.cwrdapi.controllers.CaseWorkerRefControllerTest",
						"Replace Expect Annotation With Assert Throws StaffReferenceException.class from method public testRefreshRoleAllocationWhenLrdResponseReturns400() : void in class uk.gov.hmcts.reform.cwrdapi.service.impl.CaseWorkerServiceImplTest",
						"Replace Expect Annotation With Assert Throws InvalidRequestException.class from method public shouldProcessServiceRoleMappingFileFailure() : void in class uk.gov.hmcts.reform.cwrdapi.service.impl.CaseWorkerServiceFacadeImplTest",
						"Replace Expect Annotation With Assert Throws ResourceNotFoundException.class from method public test_shouldThrow404WhenCaseworker_profile_not_found() : void in class uk.gov.hmcts.reform.cwrdapi.service.impl.CaseWorkerServiceImplTest",
						"Replace Expect Annotation With Assert Throws StaffReferenceException.class from method public testRefreshRoleAllocationWhenLrdResponseIsEmpty() : void in class uk.gov.hmcts.reform.cwrdapi.service.impl.CaseWorkerServiceImplTest",
						"Replace Expect Annotation With Assert Throws ResourceNotFoundException.class from method public testRefreshRoleAllocationWhenCrdResponseIsEmpty() : void in class uk.gov.hmcts.reform.cwrdapi.service.impl.CaseWorkerServiceImplTest",
						"Replace Expect Annotation With Assert Throws StaffReferenceException.class from method public testRefreshRoleAllocationWhenLrdResponseIsNon200() : void in class uk.gov.hmcts.reform.cwrdapi.service.impl.CaseWorkerServiceImplTest");
		test.assertExpectations(19,0,0);
	}
}
