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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

public class AnalyzeCommitsSince extends AnalyzeNewCommits {

	private static Logger logger = LoggerFactory.getLogger(AnalyzeCommitsSince.class);
	static Pid pid = new Pid();
	private LocalDate commitsSinceDate;
	private Date startTime = new Date();

	public static void main(String[] args) {
		try {
			AnalyzeCommitsSince task = new AnalyzeCommitsSince(args);
			task.doTask(pid);
		} catch (Exception e) {
			logger.error("Fatal error", e);
		}
	}

	public AnalyzeCommitsSince(String[] args) throws Exception {
		super(args);
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				// If arg is in a valid date format, take it as commitsSinceDate
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
					commitsSinceDate = LocalDate.parse(args[1], formatter);
					break;
				} catch (DateTimeParseException pass) {}
			}
		}
		if (commitsSinceDate == null) {
			throw new IllegalArgumentException("Please provide a date (yyyy-MM-dd) for the commits to be analyzed since.");
		}
	}

	@Override
	protected void doTask(Database db, ProjectGit project) throws Exception {
		final Database db1 = db;
		GitService gitService = new GitServiceImpl();
		File projectDir = new File(workingDir, project.getOwner());
		projectDir.mkdirs();
		File projectFile = new File(projectDir, project.getName());
		Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), project.getCloneUrl());
		
		GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
		detector.fetchAndDetectFromDate(repo, new AnalyzeNewCommitsHandler(db1, project, repo), commitsSinceDate, null);
		repo.close();
	}

}
