package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.diff.CodeRange;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "coderangegit")
public class CodeRangeGit extends AbstractEntity {
    public static CodeRangeGit fromCodeRange(CodeRange from, RefactoringGit refactoring, DiffSide diffSide) {
        Objects.requireNonNull(refactoring, "refactoring cannot be null");
        CodeRangeGit to = new CodeRangeGit();
        to.filePath = from.getFilePath();
        to.startLine = from.getStartLine();
        to.endLine = from.getEndLine();
        to.startColumn = from.getStartColumn();
        to.endColumn = from.getEndColumn();
        to.codeElementType = from.getCodeElementType().toString();
        to.description = from.getDescription();
        to.codeElement = from.getCodeElement();
        to.refactoring = refactoring;
        to.diffSide = diffSide;
        return to;
    }

    public enum DiffSide {
        LEFT, RIGHT;
    }
    @Enumerated(EnumType.STRING)
    @Column(name = "diffSide", nullable = false)
    DiffSide diffSide;
    @ManyToOne
    @JoinColumn(name = "refactoring")
    private RefactoringGit refactoring;

    @Column(length = 255, nullable = false)
    @Index(name="index_refactoringgit_filePath")
    private String filePath;

    @Column(length = 127, nullable = false)
    @Index(name="index_refactoringgit_codeElementType")
    private String codeElementType;
    private int startLine;
    private int endLine;
    private int startColumn;
    private int endColumn;
    @Column(length = 5000)
    private String description;
    @Column(length = 5000)
    private String codeElement;
    public String getFilePath() {
        return filePath;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public String getCodeElementType() {
        return codeElementType;
    }

    public String getDescription() {
        return description;
    }

    public CodeRangeGit setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getCodeElement() {
        return codeElement;
    }

    public RefactoringGit getRefactoring() {
        return refactoring;
    }

    public void setRefactoring(RefactoringGit refactoring) {
        this.refactoring = refactoring;
    }

    public CodeRangeGit setCodeElement(String codeElement) {
        this.codeElement = codeElement;
        return this;
    }

    public boolean subsumes(CodeRangeGit other) {
        return this.filePath.equals(other.filePath) &&
                this.startLine <= other.startLine &&
                this.endLine >= other.endLine;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("\n");
        encodeStringProperty(sb, "filePath", filePath, false);
        encodeIntProperty(sb, "startLine", startLine, false);
        encodeIntProperty(sb, "endLine", endLine, false);
        encodeIntProperty(sb, "startColumn", startColumn, false);
        encodeIntProperty(sb, "endColumn", endColumn, false);
        encodeStringProperty(sb, "codeElementType", codeElementType, false);
        encodeStringProperty(sb, "description", description, false);
        encodeStringProperty(sb, "codeElement", escapeQuotes(codeElement), true);
        sb.append("}");
        return sb.toString();
    }

    private String escapeQuotes(String s) {
        if(s != null) {
            StringBuilder sb = new StringBuilder();
            JsonStringEncoder encoder = JsonStringEncoder.getInstance();
            encoder.quoteAsString(s, sb);
            return sb.toString();
        }
        return s;
    }

    private void encodeStringProperty(StringBuilder sb, String propertyName, String value, boolean last) {
        if(value != null)
            sb.append("\t").append("\t").append("\"" + propertyName + "\"" + ": " + "\"" + value + "\"");
        else
            sb.append("\t").append("\t").append("\"" + propertyName + "\"" + ": " + value);
        insertNewLine(sb, last);
    }

    private void encodeIntProperty(StringBuilder sb, String propertyName, int value, boolean last) {
        sb.append("\t").append("\t").append("\"" + propertyName + "\"" + ": " + value);
        insertNewLine(sb, last);
    }

    private void insertNewLine(StringBuilder sb, boolean last) {
        if(last)
            sb.append("\n");
        else
            sb.append(",").append("\n");
    }

    public static CodeRange computeRange(Set<AbstractCodeFragment> codeFragments) {
        String filePath = null;
        int minStartLine = 0;
        int maxEndLine = 0;
        int startColumn = 0;
        int endColumn = 0;

        for(AbstractCodeFragment fragment : codeFragments) {
            LocationInfo info = fragment.getLocationInfo();
            filePath = info.getFilePath();
            if(minStartLine == 0 || info.getStartLine() < minStartLine) {
                minStartLine = info.getStartLine();
                startColumn = info.getStartColumn();
            }
            if(info.getEndLine() > maxEndLine) {
                maxEndLine = info.getEndLine();
                endColumn = info.getEndColumn();
            }
        }
        return new CodeRange(filePath, minStartLine, maxEndLine, startColumn, endColumn, LocationInfo.CodeElementType.LIST_OF_STATEMENTS);
    }
}
