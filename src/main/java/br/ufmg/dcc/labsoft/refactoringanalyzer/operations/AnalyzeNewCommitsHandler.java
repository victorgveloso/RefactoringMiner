package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.util.Date;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import org.eclipse.jgit.lib.Repository;

public class AnalyzeNewCommitsHandler extends AnalyzeProjectsHandler {

	public AnalyzeNewCommitsHandler(Database db, ProjectGit project, Repository repo) {
		super(db, project, repo);
	}

	@Override
	public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
		project.setLast_update(new Date());
		db.update(project);
	}

}