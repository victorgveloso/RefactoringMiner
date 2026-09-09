package gui;

import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;

// Ad-hoc diagnostic: for a single (url, commit), dump every operation body mapper's
// mappings / non-mapped leaves / replacements whose text mentions "assert" so we can see
// exactly what RefactoringMiner produced for statements our fluent-assertion detector missed.
public class DebugSingleCommitAssertions {

    private static final File ROOT_FOLDER = new File(System.getProperty("user.home") + "/rm-github-cache");

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "https://github.com/OpenGamma/Strata.git";
        String commit = args.length > 1 ? args[1] : "b2b9b629685ebc7e89e9a1667de88f2e878d5fc4";

        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
        UMLModelDiff diff = miner.detectAtCommitWithGitHubAPI(url, commit, ROOT_FOLDER);
        if (diff == null) {
            System.out.println("diff is null");
            return;
        }
        java.util.Set<String> classFilter = args.length > 2 ?
                new java.util.HashSet<>(java.util.Arrays.asList(args[2].split(","))) : null;
        java.util.Set<String> methodFilter = args.length > 3 ?
                new java.util.HashSet<>(java.util.Arrays.asList(args[3].split(","))) : null;
        System.out.println("commonClassDiffList size = " + diff.getCommonClassDiffList().size());
        for (UMLClassDiff classDiff : diff.getCommonClassDiffList()) {
            if (classFilter != null && !classFilter.contains(classDiff.getOriginalClass().getName())) continue;
            for (UMLOperationBodyMapper mapper : classDiff.getOperationBodyMapperList()) {
                if (methodFilter != null && !methodFilter.contains(mapper.getOperation1().getName())) continue;
                boolean relevant = mentionsAssert(mapper);
                if (!relevant) continue;
                System.out.println("\n=== " + classDiff.getOriginalClass().getName() + " :: "
                        + mapper.getOperation1().getName() + " -> " + mapper.getOperation2().getName() + " ===");
                System.out.println("-- mappings (" + mapper.getMappings().size() + ") --");
                for (AbstractCodeMapping m : mapper.getMappings()) {
                    String s1 = m.getFragment1().getString().trim();
                    String s2 = m.getFragment2().getString().trim();
                    if (s1.contains("assert") || s2.contains("assert")) {
                        System.out.println("  [" + m.getClass().getSimpleName() + "] " + s1 + "   <=>   " + s2);
                    }
                }
                System.out.println("-- nonMappedLeavesT1 (" + mapper.getNonMappedLeavesT1().size() + ") --");
                for (AbstractCodeFragment f : mapper.getNonMappedLeavesT1()) {
                    String s = f.getString().trim();
                    if (s.contains("assert")) System.out.println("  - " + s);
                }
                System.out.println("-- nonMappedLeavesT2 (" + mapper.getNonMappedLeavesT2().size() + ") --");
                for (AbstractCodeFragment f : mapper.getNonMappedLeavesT2()) {
                    String s = f.getString().trim();
                    if (s.contains("assert")) System.out.println("  + " + s);
                }
                System.out.println("-- replacements (" + mapper.getReplacements().size() + ") --");
                for (Replacement r : mapper.getReplacements()) {
                    System.out.println("  " + r.getType() + ": [" + r.getBefore() + "] -> [" + r.getAfter() + "]");
                }
            }
        }
    }

    private static boolean mentionsAssert(UMLOperationBodyMapper mapper) throws Exception {
        for (AbstractCodeFragment f : mapper.getNonMappedLeavesT1()) {
            if (f.getString().contains("assert")) return true;
        }
        for (AbstractCodeFragment f : mapper.getNonMappedLeavesT2()) {
            if (f.getString().contains("assert")) return true;
        }
        for (AbstractCodeMapping m : mapper.getMappings()) {
            if (m.getFragment1().getString().contains("assert") || m.getFragment2().getString().contains("assert")) return true;
        }
        return false;
    }
}
