package gui;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.PathFileUtils;

import java.io.File;
import java.util.*;

// Reimplements the model-building side of diffAtDirectories (as used by RunWithTwoDirectories -> WebDiff)
// so we can inspect the underlying UMLOperationBodyMapper mappings/replacements directly,
// exactly as DebugSingleCommitAssertions does for detectAtCommitWithGitHubAPI.
public class DebugTwoDirectoriesAssertions {

    public static void main(String[] args) throws Exception {
        String folder1 = args.length > 0 ? args[0] : "/Users/victor/IdeaProjects/RefactoringMiner/tmp/v1/";
        String folder2 = args.length > 1 ? args[1] : "/Users/victor/IdeaProjects/RefactoringMiner/tmp/v2/";
        boolean useASTDiffModel = args.length <= 2 || Boolean.parseBoolean(args[2]);
        Set<String> classFilter = args.length > 3 ?
                new HashSet<>(Arrays.asList(args[3].split(","))) : null;
        Set<String> methodFilter = args.length > 4 ?
                new HashSet<>(Arrays.asList(args[4].split(","))) : null;

        File previousFile = new File(folder1);
        File nextFile = new File(folder2);

        Set<String> repositoryDirectoriesBefore = new LinkedHashSet<>();
        Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<>();
        Map<String, String> fileContentsBefore = new LinkedHashMap<>();
        Map<String, String> fileContentsCurrent = new LinkedHashMap<>();
        populateFileContents(nextFile, getJavaFilePaths(nextFile), fileContentsCurrent, repositoryDirectoriesCurrent);
        populateFileContents(previousFile, getJavaFilePaths(previousFile), fileContentsBefore, repositoryDirectoriesBefore);

        UMLModel parentUMLModel = useASTDiffModel ?
                GitHistoryRefactoringMinerImpl.createModelForASTDiff(fileContentsBefore, repositoryDirectoriesBefore) :
                GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
        UMLModel currentUMLModel = useASTDiffModel ?
                GitHistoryRefactoringMinerImpl.createModelForASTDiff(fileContentsCurrent, repositoryDirectoriesCurrent) :
                GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
        UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
        // force refactoring detection too, in case it affects lazily computed state
        modelDiff.getRefactorings();

        System.out.println("useASTDiffModel = " + useASTDiffModel);
        System.out.println("commonClassDiffList size = " + modelDiff.getCommonClassDiffList().size());
        for (UMLClassDiff classDiff : modelDiff.getCommonClassDiffList()) {
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

    private static List<String> getJavaFilePaths(File dir) {
        List<String> paths = new ArrayList<>();
        collect(dir, dir, paths);
        return paths;
    }

    private static void collect(File root, File dir, List<String> paths) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collect(root, f, paths);
            } else if (PathFileUtils.isJavaFile(f.getName())) {
                paths.add(root.toPath().relativize(f.toPath()).toString());
            }
        }
    }

    private static void populateFileContents(File rootFolder, List<String> filePaths, Map<String, String> fileContents,
            Set<String> repositoryDirectories) throws java.io.IOException {
        for (String path : filePaths) {
            File f = new File(rootFolder, path);
            String content = new String(java.nio.file.Files.readAllBytes(f.toPath()));
            fileContents.put(path.replace(File.separatorChar, '/'), content);
        }
        repositoryDirectories.add("");
    }
}
