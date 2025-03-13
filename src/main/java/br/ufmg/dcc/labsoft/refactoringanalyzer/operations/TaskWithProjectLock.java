package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.File;
import java.io.IOException;

//import org.apache.log4j.FileAppender;
//import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

public abstract class TaskWithProjectLock {

	static final Logger logger = LoggerFactory.getLogger(TaskWithProjectLock.class);
	
	protected Database db;
	protected File workingDir = new File("tmp");

	protected TaskWithProjectLock(Database db) {
		this.db = db;
	}

	public final void doTask(final Pid pid) {
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                db.releaseLocks(pid.toString());
                logger.info("Locks released at shutdown");
            }
        });
		
		try {
			ProjectGit project = null;
			ProjectGit previousProject = null;
			while ((project = this.findNextProject(db, pid)) != null) {
				if (previousProject != null && previousProject.equals(project)) {
					logger.warn("Skipping project " + project.getId() + " because it was already processed");
					continue;
				}
				try {
					this.doTask(db, project);
				} catch (Exception e) {
					// This may be a temporary connection problem with github, so log the error and move on ...
					logger.warn("Skipping project due to error", e);
				}
				finally {
					db.releaseLocks(pid.toString());
					logger.info("Locks released");
				}
				previousProject = project;
			}
			logger.info("No more projects");
			
		} catch (Exception e) {
			logger.error("Fatal error", e);
		}
	}
	
	protected abstract void doTask(Database db, ProjectGit project) throws Exception;

	protected abstract ProjectGit findNextProject(Database db, Pid pid) throws Exception;

	protected void initWorkingDir(String[] args, Pid pid) throws IOException {
		if (args.length > 0) {
			workingDir = new File(args[0]);
		}
		if (!workingDir.exists()) {
			workingDir.mkdir();
		}
	}
}
