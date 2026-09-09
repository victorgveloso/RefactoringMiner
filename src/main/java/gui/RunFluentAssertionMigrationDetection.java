package gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.LeafExpression;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// Batch-checks the (url, commit) pairs in fluent_assertion_occurrences.json for genuine evidence
// that assertThat/isEqualTo replaced a traditional JUnit assertion (assertEquals, assertNull, ...)
// within the SAME migration, as opposed to fluent assertions merely being introduced fresh
// alongside unrelated changes in the same commit. Resumable: results are appended incrementally to
// OUTPUT_CSV_PATH, and already-processed pairs (any verdict, including ERROR) are skipped on rerun.
public class RunFluentAssertionMigrationDetection {

    private static final String INPUT_JSON_PATH =
            System.getProperty("user.home") + "/IdeaProjects/RefactoringMiner/fluent_assertion_occurrences.json";
    private static final String OUTPUT_CSV_PATH =
            System.getProperty("user.home") + "/IdeaProjects/RefactoringMiner/fluent_assertion_migration_results.csv";
    private static final File ROOT_FOLDER =
            new File(System.getProperty("user.home") + "/rm-github-cache");
    private static final int LIMIT = 200; // pairs to process THIS invocation; bump for a bigger run
    private static final int THREAD_POOL_SIZE = 12;
    private static final int PROGRESS_LOG_INTERVAL = 25;
    // Bounds the worst case (observed: one 52North/SOS commit alone took 15+ minutes, spinning up
    // thousands of per-file fetch threads, and stalled the whole batch). Each pair runs in its own
    // JVM subprocess (see runInSubprocess) specifically so a timeout can force-kill the OS process:
    // an earlier in-process Thread.interrupt()-based timeout only stopped WAITING on the stuck work,
    // it never actually reclaimed it - the orphaned threads and everything they held kept running and
    // accumulating in the background, and a 1200-pair run crashed with OutOfMemoryError around pair
    // 900 despite an already-generous -Xmx4096M. destroyForcibly() on the child process is the only
    // reliable way to guarantee the memory and threads are actually freed.
    private static final long PER_PAIR_TIMEOUT_SECONDS = 180;
    private static final String WORKER_HEAP_MB = "512";

    private static final Set<String> TRADITIONAL_ASSERTION_NAMES = Set.of(
            "assertEquals", "assertNotEquals", "assertNull", "assertNotNull",
            "assertTrue", "assertFalse", "assertSame", "assertNotSame", "assertArrayEquals"
    );
    private static final Set<String> FLUENT_ASSERTION_NAMES = Set.of("assertThat", "isEqualTo");

    // Common Hamcrest matcher-factory method names: excluded from the Tier 3 token-overlap check
    // below because they're generic vocabulary shared by unrelated assertions, not evidence of a
    // real correspondence between two specific statements.
    private static final Set<String> HAMCREST_STOPWORDS = Set.of(
            "is", "not", "equalTo", "nullValue", "notNullValue", "instanceOf", "hasItem", "hasItems",
            "contains", "empty", "hasSize", "anyOf", "allOf", "containsString", "closeTo",
            "greaterThan", "lessThan", "any"
    );

    private record CommitPair(String url, String commit) {
        String key() {
            return url + "@" + commit;
        }
    }

    private enum Verdict { KEEP, DISCARD, ERROR }

    private record EvidenceResult(Verdict verdict, String evidence) {}

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--worker")) {
            runWorker(args[1], args[2]);
            return;
        }
        runSupervisor();
    }

    // Invoked in a throwaway subprocess (see runInSubprocess) to detect a single pair, then prints
    // one line the supervisor can parse and exits. Evidence is base64-encoded so it can never break
    // the one-line protocol regardless of embedded tabs/newlines from multi-line source statements.
    private static void runWorker(String url, String commit) {
        ROOT_FOLDER.mkdirs();
        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
        EvidenceResult result = processPair(new CommitPair(url, commit), miner);
        String encodedEvidence = Base64.getEncoder().encodeToString(result.evidence().getBytes(StandardCharsets.UTF_8));
        System.out.println("RESULT\t" + result.verdict() + "\t" + encodedEvidence);
        System.out.flush();
    }

    private static void runSupervisor() throws Exception {
        ROOT_FOLDER.mkdirs();

        List<CommitPair> allPairs = loadInputPairs(INPUT_JSON_PATH);
        Set<String> alreadyProcessed = loadAlreadyProcessedKeys(OUTPUT_CSV_PATH);
        List<CommitPair> remaining = allPairs.stream()
                .filter(p -> !alreadyProcessed.contains(p.key()))
                .collect(Collectors.toList());
        List<CommitPair> toProcess = remaining.stream()
                .limit(LIMIT)
                .collect(Collectors.toList());

        System.out.printf("Total pairs: %d, already processed: %d, remaining: %d, processing this run: %d%n",
                allPairs.size(), alreadyProcessed.size(), remaining.size(), toProcess.size());

        if (toProcess.isEmpty()) {
            System.out.println("Nothing to do.");
            return;
        }

        boolean writeHeader = !new File(OUTPUT_CSV_PATH).exists();
        Object writeLock = new Object();
        AtomicInteger completed = new AtomicInteger();
        AtomicInteger keepCount = new AtomicInteger();
        AtomicInteger discardCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        try (Writer writer = new BufferedWriter(new FileWriter(OUTPUT_CSV_PATH, true))) {
            if (writeHeader) {
                writer.write("url,commit,verdict,evidence\n");
                writer.flush();
            }

            // Plain fixed pool is safe again here (unlike the earlier in-process-timeout design):
            // runInSubprocess's process.waitFor(timeout) + destroyForcibly() guarantees each worker
            // thread genuinely returns within PER_PAIR_TIMEOUT_SECONDS, so a stuck commit can no
            // longer permanently strand a pool thread or leak memory past this method's return.
            ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            for (CommitPair pair : toProcess) {
                pool.submit(() -> {
                    EvidenceResult result = runInSubprocess(pair);
                    switch (result.verdict()) {
                        case KEEP -> keepCount.incrementAndGet();
                        case DISCARD -> discardCount.incrementAndGet();
                        case ERROR -> errorCount.incrementAndGet();
                    }
                    synchronized (writeLock) {
                        try {
                            writeCsvRow(writer, pair, result);
                        } catch (IOException e) {
                            System.err.println("Failed to write row for " + pair.key() + ": " + e.getMessage());
                        }
                    }
                    int n = completed.incrementAndGet();
                    if (n % PROGRESS_LOG_INTERVAL == 0 || n == toProcess.size()) {
                        System.out.printf("Progress: %d/%d (KEEP=%d DISCARD=%d ERROR=%d)%n",
                                n, toProcess.size(), keepCount.get(), discardCount.get(), errorCount.get());
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }

        System.out.printf("Done. KEEP=%d DISCARD=%d ERROR=%d%n", keepCount.get(), discardCount.get(), errorCount.get());
    }

    private static EvidenceResult processPair(CommitPair pair, GitHistoryRefactoringMinerImpl miner) {
        try {
            UMLModelDiff diff = miner.detectAtCommitWithGitHubAPI(pair.url(), pair.commit(), ROOT_FOLDER);
            if (diff == null) {
                return new EvidenceResult(Verdict.ERROR, "detectAtCommitWithGitHubAPI returned null");
            }
            return findMigrationEvidence(diff);
        } catch (Throwable t) {
            return new EvidenceResult(Verdict.ERROR, t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static EvidenceResult runInSubprocess(CommitPair pair) {
        Process process = null;
        try {
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String classpath = System.getProperty("java.class.path");
            ProcessBuilder pb = new ProcessBuilder(javaBin, "-Xmx" + WORKER_HEAP_MB + "m", "-cp", classpath,
                    "gui.RunFluentAssertionMigrationDetection", "--worker", pair.url(), pair.commit());
            pb.redirectErrorStream(false);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            process = pb.start();
            Process child = process;

            // Drain stdout on its own thread concurrently with waitFor below, so the child can never
            // block on a full stdout pipe buffer while we're separately timing it out.
            List<String> outputLines = Collections.synchronizedList(new ArrayList<>());
            Thread drainer = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(child.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputLines.add(line);
                    }
                } catch (IOException ignored) {
                }
            });
            drainer.setDaemon(true);
            drainer.start();

            boolean finished = process.waitFor(PER_PAIR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                return new EvidenceResult(Verdict.ERROR, "timeout after " + PER_PAIR_TIMEOUT_SECONDS + "s");
            }
            drainer.join(5000);

            String resultLine = null;
            for (String line : outputLines) {
                if (line.startsWith("RESULT\t")) {
                    resultLine = line;
                }
            }
            if (resultLine == null) {
                return new EvidenceResult(Verdict.ERROR, "worker exited " + process.exitValue() + " with no RESULT line");
            }
            return parseResultLine(resultLine);
        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }
            return new EvidenceResult(Verdict.ERROR, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static EvidenceResult parseResultLine(String line) {
        String[] parts = line.split("\t", 3);
        Verdict verdict = Verdict.valueOf(parts[1]);
        String evidence = parts.length > 2 && !parts[2].isEmpty()
                ? new String(Base64.getDecoder().decode(parts[2]), StandardCharsets.UTF_8)
                : "";
        return new EvidenceResult(verdict, evidence);
    }

    // Three independent, complementary evidence sources, weakest last (see plan for the first two's
    // rationale): a statement pair explicitly flagged as a method-call replacement; any mapped
    // fragment (statement, argument, or literal) whose location falls inside a traditional-assertion
    // call before and a fluent-assertion call after; or - covering the common case where
    // RefactoringMiner's own statement matcher simply gives up on pairing an assertion rewrite at
    // all (confirmed by direct inspection: e.g. `Assert.assertTrue(sa instanceof X)` vs
    // `assertThat(sa, is(instanceOf(X.class)))` land as fully unrelated entries in
    // getNonMappedLeavesT1()/T2() with zero AbstractCodeMapping connecting them) - an unmapped
    // traditional-assertion leaf in this SAME matched method paired with an unmapped fluent-assertion
    // leaf that shares a distinctive identifier/literal (a variable name, invoked method name, or
    // string literal), which is the only signal available once RefactoringMiner declines to map the
    // statements itself. Scoping this to one UMLOperationBodyMapper (i.e. one already-matched
    // method pair) is what keeps it from repeating the file-level false positive of correlating
    // assertions in unrelated methods that merely happen to share a source file.
    private static EvidenceResult findMigrationEvidence(UMLModelDiff diff) throws Exception {
        for (UMLClassDiff classDiff : diff.getCommonClassDiffList()) {
            for (UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
                Optional<String> replacementEvidence = findReplacementEvidence(mapper);
                if (replacementEvidence.isPresent()) {
                    return new EvidenceResult(Verdict.KEEP, replacementEvidence.get());
                }
                Optional<String> spanEvidence = findSpanContainmentEvidence(mapper);
                if (spanEvidence.isPresent()) {
                    return new EvidenceResult(Verdict.KEEP, spanEvidence.get());
                }
                Optional<String> unmappedEvidence = findUnmappedCorrelationEvidence(mapper);
                if (unmappedEvidence.isPresent()) {
                    return new EvidenceResult(Verdict.KEEP, unmappedEvidence.get());
                }
            }
        }
        return new EvidenceResult(Verdict.DISCARD, "");
    }

    private static Optional<String> findReplacementEvidence(UMLOperationBodyMapper mapper) {
        for (Replacement r : mapper.getReplacements()) {
            if (r instanceof MethodInvocationReplacement mir) {
                String beforeName = mir.getInvokedOperationBefore().getName();
                String afterName = mir.getInvokedOperationAfter().getName();
                if (TRADITIONAL_ASSERTION_NAMES.contains(beforeName) && FLUENT_ASSERTION_NAMES.contains(afterName)) {
                    return Optional.of("replacement: " + beforeName + " -> " + afterName);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> findSpanContainmentEvidence(UMLOperationBodyMapper mapper) throws Exception {
        List<LocationInfo> beforeSpans = assertionCallSpans(mapper.getOperation1(), TRADITIONAL_ASSERTION_NAMES);
        List<LocationInfo> afterSpans = assertionCallSpans(mapper.getOperation2(), FLUENT_ASSERTION_NAMES);
        if (beforeSpans.isEmpty() || afterSpans.isEmpty()) {
            return Optional.empty();
        }
        for (AbstractCodeMapping mapping : mapper.getMappings()) {
            LocationInfo loc1 = mapping.getFragment1().getLocationInfo();
            LocationInfo loc2 = mapping.getFragment2().getLocationInfo();
            boolean beforeMatch = beforeSpans.stream().anyMatch(s -> s.subsumes(loc1));
            boolean afterMatch = afterSpans.stream().anyMatch(s -> s.subsumes(loc2));
            if (beforeMatch && afterMatch) {
                return Optional.of("span-contained: before=[" + mapping.getFragment1().getString().trim()
                        + "] after=[" + mapping.getFragment2().getString().trim() + "]");
            }
        }
        return Optional.empty();
    }

    private static Optional<String> findUnmappedCorrelationEvidence(UMLOperationBodyMapper mapper) {
        List<AbstractCodeFragment> beforeCandidates = leavesContainingCall(mapper.getNonMappedLeavesT1(), TRADITIONAL_ASSERTION_NAMES);
        List<AbstractCodeFragment> afterCandidates = leavesContainingCall(mapper.getNonMappedLeavesT2(), FLUENT_ASSERTION_NAMES);
        for (AbstractCodeFragment before : beforeCandidates) {
            Set<String> beforeTokens = significantTokens(before);
            for (AbstractCodeFragment after : afterCandidates) {
                Set<String> shared = new HashSet<>(beforeTokens);
                shared.retainAll(significantTokens(after));
                if (!shared.isEmpty()) {
                    return Optional.of("unmapped-correlated: shared=" + shared
                            + " before=[" + before.getString().trim() + "] after=[" + after.getString().trim() + "]");
                }
            }
        }
        return Optional.empty();
    }

    private static List<AbstractCodeFragment> leavesContainingCall(List<AbstractCodeFragment> leaves, Set<String> names) {
        List<AbstractCodeFragment> result = new ArrayList<>();
        for (AbstractCodeFragment leaf : leaves) {
            for (AbstractCall call : leaf.getMethodInvocations()) {
                if (names.contains(call.getName())) {
                    result.add(leaf);
                    break;
                }
            }
        }
        return result;
    }

    private static Set<String> significantTokens(AbstractCodeFragment fragment) {
        Set<String> tokens = new HashSet<>();
        for (LeafExpression variable : fragment.getVariables()) {
            tokens.add(variable.getString());
        }
        for (LeafExpression literal : fragment.getStringLiterals()) {
            tokens.add(literal.getString());
        }
        for (AbstractCall call : fragment.getMethodInvocations()) {
            String name = call.getName();
            if (!TRADITIONAL_ASSERTION_NAMES.contains(name) && !FLUENT_ASSERTION_NAMES.contains(name)
                    && !HAMCREST_STOPWORDS.contains(name)) {
                tokens.add(name);
            }
        }
        return tokens;
    }

    private static List<LocationInfo> assertionCallSpans(UMLOperation operation, Set<String> names) {
        List<LocationInfo> spans = new ArrayList<>();
        if (operation == null || operation.getBody() == null) {
            return spans;
        }
        CompositeStatementObject body = operation.getBody().getCompositeStatement();
        for (AbstractStatement statement : body.getAllStatements()) {
            for (AbstractCall call : statement.getMethodInvocations()) {
                if (names.contains(call.getName())) {
                    spans.add(statement.getLocationInfo());
                    break;
                }
            }
        }
        return spans;
    }

    private static List<CommitPair> loadInputPairs(String jsonPath) throws IOException {
        try (Reader reader = new FileReader(jsonPath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray entries = root.entrySet().iterator().next().getValue().getAsJsonArray();
            List<CommitPair> pairs = new ArrayList<>(entries.size());
            for (JsonElement el : entries) {
                JsonObject obj = el.getAsJsonObject();
                pairs.add(new CommitPair(obj.get("url").getAsString(), obj.get("current_commit").getAsString()));
            }
            return pairs;
        }
    }

    // Any row already present in the output CSV - regardless of verdict, including ERROR - is
    // treated as done and never retried automatically. To force a retry of specific ERROR rows,
    // manually delete those lines from the CSV before rerunning.
    private static Set<String> loadAlreadyProcessedKeys(String csvPath) throws IOException {
        Set<String> keys = new HashSet<>();
        File file = new File(csvPath);
        if (!file.exists()) {
            return keys;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                int firstComma = line.indexOf(',');
                int secondComma = line.indexOf(',', firstComma + 1);
                if (firstComma < 0 || secondComma < 0) {
                    continue;
                }
                String url = line.substring(0, firstComma);
                String commit = line.substring(firstComma + 1, secondComma);
                keys.add(url + "@" + commit);
            }
        }
        return keys;
    }

    private static void writeCsvRow(Writer writer, CommitPair pair, EvidenceResult result) throws IOException {
        writer.write(escapeCsv(pair.url()) + "," + escapeCsv(pair.commit()) + ","
                + result.verdict() + "," + escapeCsv(result.evidence()) + "\n");
        writer.flush();
    }

    private static String escapeCsv(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
