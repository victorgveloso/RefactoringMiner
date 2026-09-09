package gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// Batch-runs the same GitHub-API refactoring detection pipeline as RunWithGitHubAPI over every
// commit listed in example_data_for_test_fixture_analysis.csv, and stores the detected refactorings
// for each commit as one JSON file under test_fixture_analysis/. Resumable: a commit whose output
// file already exists is skipped on rerun.
public class RunTestFixtureAnalysis {

    private static final Path INPUT_CSV = Paths.get("example_data_for_test_fixture_analysis.csv");
    private static final Path OUTPUT_DIR = Paths.get("test_fixture_analysis");
    private static final int TIMEOUT_SECONDS = 1000; // same per-commit timeout RunWithGitHubAPI uses for diffAtCommit

    private record CsvRow(String commitUrl, List<String> filepaths) {}

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        List<CsvRow> rows = readCsv(INPUT_CSV);
        System.out.println("Loaded " + rows.size() + " commits from " + INPUT_CSV);

        GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
        int skipped = 0, analyzed = 0, failed = 0;
        for (int i = 0; i < rows.size(); i++) {
            CsvRow row = rows.get(i);
            String repo;
            String commit;
            try {
                repo = URLHelper.getRepo(row.commitUrl());
                commit = URLHelper.getCommit(row.commitUrl());
            } catch (RuntimeException e) {
                System.err.println("Skipping malformed commit URL: " + row.commitUrl());
                failed++;
                continue;
            }

            Path outputFile = OUTPUT_DIR.resolve(outputFileName(row.commitUrl(), commit));
            if (Files.exists(outputFile)) {
                skipped++;
                continue;
            }

            System.out.printf("[%d/%d] %s%n", i + 1, rows.size(), row.commitUrl());
            List<Refactoring> refactorings = new ArrayList<>();
            AtomicBoolean handled = new AtomicBoolean(false);
            AtomicReference<String> error = new AtomicReference<>();
            RefactoringHandler handler = new RefactoringHandler() {
                @Override
                public void handle(String commitId, List<Refactoring> refs) {
                    refactorings.addAll(refs);
                    handled.set(true);
                }

                @Override
                public void handleException(String commitId, Exception e) {
                    error.set(e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            };

            detector.detectAtCommit(repo, commit, handler, TIMEOUT_SECONDS);

            if (!handled.get()) {
                System.err.println("  -> no result (" +
                        (error.get() != null ? error.get() : "timed out after " + TIMEOUT_SECONDS + "s") +
                        "), will retry next run");
                failed++;
                continue;
            }

            writeResult(outputFile, row, repo, commit, refactorings);
            analyzed++;
        }

        System.out.printf("Done. analyzed=%d skipped=%d failed=%d total=%d%n", analyzed, skipped, failed, rows.size());
    }

    private static String outputFileName(String commitUrl, String commit) {
        String owner = URLHelper.getOwnerStringOnly(commitUrl);
        String repoName = URLHelper.getRepoStringOnly(commitUrl);
        return owner + "__" + repoName + "__" + commit + ".json";
    }

    private static void writeResult(Path outputFile, CsvRow row, String repo, String commit,
                                     List<Refactoring> refactorings) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("commitUrl", row.commitUrl());
        root.addProperty("repository", repo);
        root.addProperty("sha1", commit);

        JsonArray filepaths = new JsonArray();
        for (String filepath : row.filepaths()) {
            filepaths.add(filepath);
        }
        root.add("testFixtureFilepaths", filepaths);

        JsonArray refactoringsJson = new JsonArray();
        for (Refactoring refactoring : refactorings) {
            refactoringsJson.add(JsonParser.parseString(refactoring.toJSON(row.commitUrl())));
        }
        root.add("refactorings", refactoringsJson);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(outputFile, gson.toJson(root), StandardCharsets.UTF_8);
    }

    private static List<CsvRow> readCsv(Path csvPath) throws IOException {
        List<CsvRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath.toFile(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header: "commit_url","filepaths"
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = parseCsvLine(line);
                if (fields.size() < 2) {
                    continue;
                }
                String commitUrl = fields.get(0).trim();
                List<String> filepaths = new ArrayList<>();
                try {
                    for (JsonElement el : JsonParser.parseString(fields.get(1)).getAsJsonArray()) {
                        filepaths.add(el.getAsString());
                    }
                } catch (RuntimeException e) {
                    System.err.println("Skipping row with malformed filepaths column: " + commitUrl);
                    continue;
                }
                rows.add(new CsvRow(commitUrl, filepaths));
            }
        }
        return rows;
    }

    // Minimal RFC4180 field splitter: handles quoted fields containing commas and doubled ("") quotes,
    // which is how the "filepaths" JSON-array column is CSV-escaped in the input file.
    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
