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
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

public class AnalyzeCommitsRange extends AnalyzeNewCommits {

	private static Logger logger = LoggerFactory.getLogger(AnalyzeCommitsRange.class);
	static Pid pid = new Pid();
	private LocalDate commitsSinceDate;
	private LocalDate commitsUntilDate;
	private Date startTime = new Date();
	private Path dir = null;

	public static void main(String[] args) {
		try {
			AnalyzeCommitsRange task = new AnalyzeCommitsRange(args);
			task.doTask(pid);
		} catch (Exception e) {
			logger.error("Fatal error", e);
		}
	}

	public AnalyzeCommitsRange(String[] args) throws Exception {
		super(args);
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				// If arg is in a valid date format, take it as commitsSinceDate
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
					LocalDate localDate = LocalDate.parse(args[i], formatter);
					if (commitsSinceDate == null) {
						commitsSinceDate = localDate;
					} else {
						commitsUntilDate = localDate;
						break;
					}
				} catch (DateTimeParseException pass) {}
			}
		}
		if (commitsUntilDate == null) {
			throw new IllegalArgumentException("Please provide a date range as two dates (yyyy-MM-dd) separated by space for the filtering of commits to be analyzed.");
		}
	}

	@Override
	protected void doTask(Database db, ProjectGit project) throws Exception {
		logger.info("Processing project {}", project.getCloneUrl());
		final Database db1 = db;
		GitService gitService = new GitServiceImpl();
		File projectDir = new File(workingDir, project.getOwner());
		projectDir.mkdirs();
		File projectFile = new File(projectDir, project.getName());
		Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), project.getCloneUrl());

		GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
		detector.fetchAndDetectBetweenDates(repo, new AnalyzeNewCommitsHandler(db1, project, repo), commitsSinceDate, commitsUntilDate, dir);
		repo.close();
	}

	public Path getDir() {
		return dir;
	}

	public void setDir(Path dir) {
		this.dir = dir;
	}
}
