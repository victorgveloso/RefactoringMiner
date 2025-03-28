package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.RevisionGit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class AnalyzeAllCommitAuthors extends AnalyzeCommitsRange {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeAllCommitAuthors.class);

    public static void main(String[] args) {
        try {
            AnalyzeCommitsRange task = new AnalyzeCommitsRange(args);
            task.doTask(pid);
        } catch (Exception e) {
            logger.error("Fatal error", e);
        }
    }

    public AnalyzeAllCommitAuthors(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected void doTask(Database db, ProjectGit project) throws Exception {
        GitService gitService = new GitServiceImpl();
        File projectDir = new File(workingDir, project.getOwner());
        projectDir.mkdirs();
        File projectFile = new File(projectDir, project.getName());
        try (Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), project.getCloneUrl())) {
            RevWalk walk = gitService.fetchAndCreateRevsWalkInRange(repo, null, commitsSinceDate, commitsUntilDate);
            for (RevCommit commit : walk) {
                RevisionGit revisionGit = RevisionGit.getFromRevCommit(commit, project);
                if (db.getRevisionById(project, revisionGit.getIdCommit()) == null) {
                    db.insert(revisionGit);
                }
            }
        }

    }
}
