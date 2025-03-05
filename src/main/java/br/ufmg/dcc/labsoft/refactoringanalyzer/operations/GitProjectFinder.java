package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;
import br.ufmg.dcc.labsoft.refactoringanalyzer.operations.utils.StringToDate;
import jakarta.ws.rs.core.HttpHeaders;

import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedIterator;
import org.kohsuke.github.PagedSearchIterable;
import org.kohsuke.github.GHRepositorySearchBuilder;
import org.kohsuke.github.GHDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitProjectFinder {

	Logger logger = LoggerFactory.getLogger(GitProjectFinder.class);

	private Database db = new Database();

	public static void main(String[] args) throws IOException {
		switch (args.length) {
			case 1:
				paginatePopularRepos(args);
				break;
			case 2:
				fetchSpecificRepos(args);
				break;
			default:
				throw new IllegalArgumentException("Please specify a valid API auth token and (optionally) the path to a valid txt file listing repos to load.");
		}
	}

	private static void fetchSpecificRepos(String[] args) throws IOException {
		if (Files.notExists(Path.of(args[1]))) {
			throw new IllegalArgumentException("File not found at " + args[1] + ".\nPlease specify a valid API auth token and the path to a valid txt file listing repos to load.");
		}
		GitProjectFinder gitProjectFinder = new GitProjectFinder();
		List<String> repos = Files.readAllLines(Path.of(args[1]));
		GitHub github = new GitHubBuilder().withOAuthToken(args[0]).build();
		gitProjectFinder.loadRepos(github, repos);
	}

	private static void paginatePopularRepos(String[] args) throws IOException {
		GitProjectFinder gitProjectFinder = new GitProjectFinder();
		for (int i = 1; i <= 10; i++) {
			gitProjectFinder.findRepos(i, args[0]);
		}
	}

	private void findRepos(int page, String token) throws IOException {
		GitHub github = new GitHubBuilder().withOAuthToken(token).build();
		findRepos(page, github);
	}

	// (Optional) Basic authentication version – note that token-based auth is recommended.
	private void findRepos(int page, String username, String password) throws IOException {
		GitHub github = GitHub.connectUsingPassword(username, password);
		findRepos(page, github);
	}

	private static ProjectGit createProjectGit(GHRepository repo) throws IOException {
		ProjectGit p = new ProjectGit();
		p.setName(repo.getName());
		p.setSize(repo.getSize());
		p.setFork(repo.isFork());
		p.setStargazers_count(repo.getStargazersCount());
		p.setWatchers_count(repo.getWatchersCount());
		p.setForks_count(repo.getForksCount());
		p.setDefault_branch(repo.getDefaultBranch());
		p.setOpen_issues(repo.getOpenIssueCount());
		// The hub4j API already returns Date objects:
		p.setCreated_at(repo.getCreatedAt());
		p.setUpdated_at(repo.getUpdatedAt());
		p.setPushed_at(repo.getPushedAt());
		p.setLast_update(repo.getPushedAt());
		// Use the HTTP clone URL, similar to the previous "clone_url" field.
		p.setCloneUrl(repo.getHttpTransportUrl());
		p.setStatus("new");
		p.setMonitoring_enabled(false);
		if (repo.getDescription() != null) {
			p.setDescription(repo.getDescription());
		}
		p.setAnalyzed(false);
		return p;
	}

	private void loadRepos(GitHub github, List<String> repos) throws IOException {
		for (String r : repos) {
			if (r.stripLeading().startsWith("#")) {
				continue;
			}
			if (r.stripTrailing().endsWith("/")) {
				r = r.strip();
				r = r.substring(0, r.length() - 1);
			}
			logger.debug("Fetching repo {}", r);
			String fullName = extractFullName(r);
			GHRepository repo = null;
			try {
				repo = github.getRepository(fullName);
			} catch (GHFileNotFoundException e) {
				logger.error("Repository {} not found", r);
				continue;
			}

			logger.debug("Repo {} fetched. Parsing data...", r);
			ProjectGit p = db.getProjectByCloneUrl(repo.getHttpTransportUrl());
			String found = Objects.isNull(p) ? "not found" : "found";
			logger.debug("Project {} {} in DB", repo.getHttpTransportUrl(), found);
			if (p != null) {
				logger.info("Found existing project {}", repo.getHttpTransportUrl());
				p.setCreated_at(repo.getCreatedAt());
				p.setUpdated_at(repo.getUpdatedAt());
				p.setPushed_at(repo.getPushedAt());
				p.setLast_update(repo.getPushedAt());
				db.update(p);
			} else {
				logger.info("Not found project in DB. Creating new project {}", repo.getHttpTransportUrl());
				p = createProjectGit(repo);
				db.insertIfNotExists(p);
			}
			logger.info("Project {} processed", repo.getHttpTransportUrl());
		}
	}

	private void findRepos(int page, GitHub github) throws IOException {
		String query = "stars:>=500 pushed:>2023-12-01 language:Java created:<=2021-12-01 forks:>=120";
		PagedSearchIterable<GHRepository> search = github.searchRepositories()
				.q(query)
				.sort(GHRepositorySearchBuilder.Sort.STARS)
				.order(GHDirection.DESC)
				.list();
		// Retrieve a specific page (with 100 items per page) using an iterator over pages
		int currentPage = 0;
		var pages = search.withPageSize(100)._iterator(100);
		List<GHRepository> reposPage = pages.nextPage();
		for (; pages.hasNext() && currentPage < page; currentPage++, reposPage = pages.nextPage()) {
		}
		if (currentPage < page) {
			return;
		}
		for (GHRepository repo : reposPage) {
			ProjectGit p = createProjectGit(repo);
			db.insertIfNotExists(p);
			logger.info("Project {}", repo.getHttpTransportUrl());
		}
	}


	/**
	 * Helper method to extract the "owner/repo" full name from a GitHub URL.
	 * Assumes the URL is in the form "https://github.com/owner/repo".
	 */
	private String extractFullName(String repoUrl) {
		if (repoUrl.startsWith("https://github.com/")) {
			return repoUrl.substring("https://github.com/".length());
		} else if (repoUrl.startsWith("http://github.com/")) {
			return repoUrl.substring("http://github.com/".length());
		}
		return repoUrl;
	}
}
