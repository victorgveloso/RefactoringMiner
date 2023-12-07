package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.RefactoringGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.RevisionGit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AnalyzeProjectsHandler extends RefactoringHandler {
	protected final Database db;
	protected final ProjectGit project;
	private Repository repository;

	private static final Logger logger = LoggerFactory.getLogger(AnalyzeProjectsHandler.class);


	AnalyzeProjectsHandler(Database db, ProjectGit project, Repository repository) {
		this.db = db;
		this.project = project;
		this.repository = repository;
	}

	@Override
	public boolean skipCommit(String curRevision) {
		return db.getRevisionById(project, curRevision) != null;
	}

	@Override
	public void handle(String commitId, List<Refactoring> refactorings) {
		logger.info("Analyzing commit {}", commitId);
		try (Git git = new Git(repository)) {
			RevWalk revWalk = new RevWalk(repository);
			ObjectId commitObjectId = repository.resolve(commitId);
			RevCommit commit = revWalk.parseCommit(commitObjectId);
			if (!handle(refactorings, commit)) {
				logger.warn("Skipping commit {} because it has no test java file", commitId);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	List<DiffEntry> getDiffEntries(RevCommit commit) throws IOException {
		assert commit.getParentCount() != 0;
		RevCommit parent = commit.getParent(0);
		DiffFormatter diffFormatter = new DiffFormatter(new ByteArrayOutputStream());
		diffFormatter.setRepository(repository);
		List<DiffEntry> diffs = diffFormatter.scan(parent.getId(), commit.getId());
		return diffs;
	}

	private boolean handle(List<Refactoring> refactorings, RevCommit commit) throws IOException {
		for (DiffEntry diff : getDiffEntries(commit)) {
			switch (diff.getChangeType()) {
			case COPY:
			case MODIFY:
			case RENAME:
				if (diff.getNewPath().endsWith("Test.java") || (diff.getNewPath().endsWith(".java") && diff.getNewPath().contains("/test/"))) {
					handle(commit, refactorings);
					return true;
				}
				logger.info("Detected non-test changed file {} in commit {}", diff.getNewPath(), commit.getName());
				break;
			case ADD:
				if (diff.getNewPath().endsWith(".java")) {
					logger.info("Detected added file {} in commit {}", diff.getNewPath(), commit.getName());
				}
			case DELETE:
				if (diff.getOldPath().endsWith(".java")) {
					logger.info("Detected removed file {} in commit {}", diff.getOldPath(), commit.getName());
				}
			default:
				break;
			}
		}
		return false;
	}

	public void handle(RevCommit curRevision, List<Refactoring> refactorings) {
		logger.info("Commit changing tests found! Saving commit {}", curRevision.getName());
		RevisionGit revision = RevisionGit.getFromRevCommit(curRevision, db.getProjectById(project.getId()));
		if (db.isAuthorContacted(revision.getAuthorEmail())) {
			revision.setStatus(RevisionGit.Status.AUTHOR_CONTACTED);
		}
		Set<RefactoringGit> refactoringSet = new HashSet<RefactoringGit>();
		for (Refactoring refactoring : refactorings) {
			RefactoringGit refact = new RefactoringGit();
			refact.setRefactoringType(refactoring.getName());
			refact.setDescription(refactoring.toString());
			refact.setRevision(revision);
			refact.setCodeRangeBefore(refactoring.leftSide()); //.forEach(db::insert);
			refact.setCodeRangeAfter(refactoring.rightSide()); //.forEach(db::insert);
			refactoringSet.add(refact);
		}
		revision.setRefactorings(refactoringSet);
		db.insert(revision);
	}

	@Override
	public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
		project.setAnalyzed(true);
		project.setStatus("analyzed");
		project.setMachine(AnalyzeProjects.pid.getMachine());
		if (project.getCommits_count() <= 0) {
			project.setCommits_count(commitsCount);
		}
		project.setError_commits_count(errorCommitsCount);
		db.update(project);
	}

	@Override
	public void handleException(String commitId, Exception e) {
		logger.error("Error analyzing commit " + commitId, e);
		try {
			RevisionGit revision = db.getRevisionById(project, commitId);
			if (revision == null) {
				revision = new RevisionGit();
				revision.setProjectGit(project);
				revision.setIdCommit(commitId);
				db.insert(revision);
			}
		} catch (Exception ignored) {
			// ignore
		} finally {
			logger.error("Error saving failure of commit " + commitId);
		}
		e.printStackTrace();
	}
}