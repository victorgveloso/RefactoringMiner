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
import java.util.Locale;

public class AnalyzeCommitsRange extends AnalyzeNewCommits {

	private static final Logger logger = LoggerFactory.getLogger(AnalyzeCommitsRange.class);
	static Pid pid = new Pid();
	protected LocalDate commitsSinceDate;
	protected LocalDate commitsUntilDate;

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
        for (String arg : args) {
            // If arg is in a valid date format, take it as commitsSinceDate
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
                LocalDate localDate = LocalDate.parse(arg, formatter);
                if (commitsSinceDate == null) {
                    commitsSinceDate = localDate;
                } else {
                    commitsUntilDate = localDate;
                    break;
                }
            } catch (DateTimeParseException ignored) { /* Try next arg*/ }
        }
        if (commitsUntilDate == null) {
			throw new IllegalArgumentException("Please provide a date range as two dates (yyyy-MM-dd) separated by space for the filtering of commits to be analyzed.");
		}
	}

	@Override
	protected void doTask(Database db, ProjectGit project) throws Exception {
		logger.info("Processing project {}", project.getCloneUrl());
        GitService gitService = new GitServiceImpl();
		File projectDir = new File(workingDir, project.getOwner());
		projectDir.mkdirs();
		File projectFile = new File(projectDir, project.getName());
		Repository repo = gitService.cloneIfNotExists(projectFile.getPath(), project.getCloneUrl());

		GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
		detector.fetchAndDetectInDateRange(repo, new AnalyzeNewCommitsHandler(db, project, repo), commitsSinceDate, commitsUntilDate);
		repo.close();
	}
}
