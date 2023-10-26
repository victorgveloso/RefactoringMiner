package br.ufmg.dcc.labsoft.refactoringanalyzer.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.json.*;

import br.ufmg.dcc.labsoft.refactoringanalyzer.operations.utils.StringToDate;
import com.jcabi.github.Coordinates;
import com.jcabi.github.Repo;
import jakarta.ws.rs.core.HttpHeaders;

import com.jcabi.github.FromProperties;
import com.jcabi.http.request.JdkRequest;
import com.jcabi.http.wire.AutoRedirectingWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.Database;
import br.ufmg.dcc.labsoft.refactoringanalyzer.dao.ProjectGit;

import com.jcabi.github.Github;
import com.jcabi.github.RtGithub;
import com.jcabi.http.Request;
import com.jcabi.http.response.JsonResponse;

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
		gitProjectFinder.loadRepos(new RtGithub(args[0]), repos);
	}

	private static void paginatePopularRepos(String[] args) throws IOException {
		GitProjectFinder gitProjectFinder = new GitProjectFinder();
		for (int i = 1; i <= 10; i++) {
			gitProjectFinder.findRepos(i, args[0]);
		}
	}

	private void findRepos(int page, String password) throws IOException {
		Request req = new JdkRequest("https://api.github.com")
				.header(
						HttpHeaders.USER_AGENT,
						new FromProperties("jcabigithub.properties").format()
				)
				.header(
						HttpHeaders.AUTHORIZATION,
						String.format("token %s", password)
				)
				.header(HttpHeaders.ACCEPT, "application/json")
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.through(AutoRedirectingWire.class);
		Github github = new RtGithub(req);
		findRepos(page, github);
	}
	private void findRepos(int page, String username, String password) throws IOException {
		Github github = new RtGithub(username, password);
		findRepos(page, github);
	}

	private static ProjectGit createProjectGit(JsonObject repoData) {
		ProjectGit p = new ProjectGit();
		p.setName(repoData.getString("name"));
		p.setSize(repoData.getInt("size"));
		p.setFork(repoData.getBoolean("fork"));
		p.setStargazers_count(repoData.getInt("stargazers_count"));
		p.setWatchers_count(repoData.getInt("watchers_count"));
		p.setForks_count(repoData.getInt("forks_count"));
		p.setDefault_branch(repoData.getString("default_branch"));
		p.setOpen_issues(repoData.getInt("open_issues"));
		p.setCreated_at(StringToDate.parseDatePatterns(repoData.getString("created_at")));
		p.setUpdated_at(StringToDate.parseDatePatterns(repoData.getString("updated_at")));
		p.setPushed_at(StringToDate.parseDatePatterns(repoData.getString("pushed_at")));
		p.setLast_update(StringToDate.parseDatePatterns(repoData.getString("pushed_at")));
		p.setLanguage(repoData.getString("language"));
		p.setCloneUrl(repoData.getString("clone_url"));
		p.setStatus("new");
		p.setMonitoring_enabled(false);

		if (!repoData.isNull("description")) {
			p.setDescription(repoData.getString("description"));
		}
		p.setAnalyzed(false);
		return p;
	}

	private void loadRepos(Github github, List<String> repos) throws IOException {
		for (String r : repos) {
			if (r.stripLeading().startsWith("#")) {
				continue;
			}
			if (r.stripTrailing().endsWith("/")) {
				r = r.strip();
				r = r.substring(0, r.length() - 1);
			}
			Repo repo = github.repos().get(new Coordinates.Https(r));
			JsonObject repoData = repo.json();
			ProjectGit p = createProjectGit(repoData);

			db.insertIfNotExists(p);
			this.logger.info("Project {}", p.getCloneUrl());
		}
	}

	private void findRepos(int page, Github github) throws IOException {
		Request request = github.entry()
				.uri().path("/search/repositories")
				.queryParam("q", "language:Java created:<=2014-06-01")
				.queryParam("sort", "stars")
				.queryParam("order", "desc")
				.queryParam("per_page", "100")
				.queryParam("page", "" + page).back()
				.method(Request.GET);

		JsonArray items = request.fetch().as(JsonResponse.class).json().readObject().getJsonArray("items");
		for (JsonValue item : items) {
			JsonObject repoData = (JsonObject) item;
			ProjectGit p = createProjectGit(repoData);

			db.insertIfNotExists(p);
			this.logger.info("Project {}", p.getCloneUrl());
		}
	}

}
