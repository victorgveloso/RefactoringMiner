package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

public class AnalyzeAllCommits extends TaskWithProjectLock{

    private static Logger logger = LoggerFactory.getLogger(AnalyzeAllCommits.class);
    static Pid pid = new Pid();
    private Date startTime = new Date();

    public static void main(String[] args) {
        try {
            AnalyzeAllCommits task = new AnalyzeAllCommits(args);
            task.doTask(pid);
        } catch (Exception e) {
            logger.error("Fatal error", e);
        }
    }

    public AnalyzeAllCommits(String[] args) throws Exception {
        super(new Database());
        initWorkingDir(args, pid);
    }

    @Override
    protected ProjectGit findNextProject(Database db, Pid pid) throws Exception {
        return db.findProjectToMonitorAndLock(pid.toString(), startTime);
    }

    @Override
    protected void doTask(Database db, Pid pid, ProjectGit project) throws Exception {
        this.analyzeProject(db, project);
    }

    public void analyzeProject(final Database db, final ProjectGit project) throws Exception {
        GitService gitService = new GitServiceImpl();
        File projectFile = new File(workingDir, project.getName());
        Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), project.getCloneUrl()/*, project.getDefault_branch()*/);

        GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
        detector.detectAll(repo, project.getDefault_branch(), new AnalyzeProjectsHandler(db, project, repo));
        repo.close();
    }

}
