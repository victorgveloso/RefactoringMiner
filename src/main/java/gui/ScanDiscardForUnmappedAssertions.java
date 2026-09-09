package gui;

import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// Broader, full-coverage investigation (not textual-diff-prefiltered) of every currently-DISCARD
// commit: for each already-matched method, does RefactoringMiner leave BOTH an unmapped removed
// assertion-like call (any assert*/isEqualTo) AND an unmapped added assertion-like call - regardless
// of whether they satisfy the stricter traditional-before/fluent-after + token-overlap requirement
// that RunFluentAssertionMigrationDetection's Tier 3 imposes? This tells us whether that stricter
// requirement is hiding real candidates versus correctly filtering noise.
public class ScanDiscardForUnmappedAssertions {

    private static final File ROOT_FOLDER = new File(System.getProperty("user.home") + "/rm-github-cache");
    private static final String CSV_PATH =
            System.getProperty("user.home") + "/IdeaProjects/RefactoringMiner/fluent_assertion_migration_results.csv";
    private static final int THREAD_POOL_SIZE = 8;

    private record CommitPair(String url, String commit) {}

    public static void main(String[] args) throws Exception {
        List<CommitPair> discardPairs = loadDiscardPairs();
        System.out.println("DISCARD pairs to scan: " + discardPairs.size());

        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
        AtomicInteger scanned = new AtomicInteger();
        AtomicInteger withSignal = new AtomicInteger();
        Object lock = new Object();

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        for (CommitPair pair : discardPairs) {
            pool.submit(() -> {
                try {
                    UMLModelDiff diff = miner.detectAtCommitWithGitHubAPI(pair.url(), pair.commit(), ROOT_FOLDER);
                    if (diff != null) {
                        List<String> hits = scanDiff(diff);
                        if (!hits.isEmpty()) {
                            withSignal.incrementAndGet();
                            synchronized (lock) {
                                System.out.println("\n### " + pair.url() + " " + pair.commit());
                                for (String h : hits) {
                                    System.out.println(h);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    synchronized (lock) {
                        System.out.println("ERROR " + pair.url() + " " + pair.commit() + " " + t);
                    }
                }
                int n = scanned.incrementAndGet();
                if (n % 25 == 0) {
                    System.err.println("scanned " + n + "/" + discardPairs.size() + " withSignal=" + withSignal.get());
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        System.out.println("\nTOTAL scanned=" + scanned.get() + " withSignal=" + withSignal.get());
    }

    private static List<String> scanDiff(UMLModelDiff diff) {
        List<String> hits = new ArrayList<>();
        for (UMLClassDiff classDiff : diff.getCommonClassDiffList()) {
            for (UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
                List<AbstractCodeFragment> removed = filterAssertionLike(mapper.getNonMappedLeavesT1());
                List<AbstractCodeFragment> added = filterAssertionLike(mapper.getNonMappedLeavesT2());
                if (!removed.isEmpty() && !added.isEmpty()) {
                    hits.add("  method " + mapper.getOperation1().getName() + " -> " + mapper.getOperation2().getName()
                            + "  removed=" + removed.size() + " added=" + added.size());
                    for (AbstractCodeFragment f : removed) {
                        hits.add("    - " + f.getString().trim());
                    }
                    for (AbstractCodeFragment f : added) {
                        hits.add("    + " + f.getString().trim());
                    }
                }
            }
        }
        return hits;
    }

    private static List<AbstractCodeFragment> filterAssertionLike(List<AbstractCodeFragment> leaves) {
        List<AbstractCodeFragment> result = new ArrayList<>();
        for (AbstractCodeFragment leaf : leaves) {
            for (AbstractCall call : leaf.getMethodInvocations()) {
                String name = call.getName();
                if (name.startsWith("assert") || name.equals("isEqualTo")) {
                    result.add(leaf);
                    break;
                }
            }
        }
        return result;
    }

    private static List<CommitPair> loadDiscardPairs() throws Exception {
        List<CommitPair> pairs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_PATH))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                int firstComma = line.indexOf(',');
                int secondComma = line.indexOf(',', firstComma + 1);
                int thirdComma = line.indexOf(',', secondComma + 1);
                if (firstComma < 0 || secondComma < 0 || thirdComma < 0) continue;
                String verdict = line.substring(secondComma + 1, thirdComma);
                if (verdict.equals("DISCARD")) {
                    pairs.add(new CommitPair(line.substring(0, firstComma), line.substring(firstComma + 1, secondComma)));
                }
            }
        }
        return pairs;
    }
}
