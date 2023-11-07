package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import org.hibernate.annotations.Index;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
@Table(name = "failure")
public class Failure {
    @Id
    @GeneratedValue
    private int id;
    @ManyToOne
    @JoinColumn(name = "revision")
    @Index(name="index_failure_revision")
    private RevisionGit revision;
    private String message;
    private String stackTrace;
    private String type;

    public ProjectGit getProject() {
        return revision.getProjectGit();
    }

    public RevisionGit getRevision() {
        return revision;
    }

    public void setRevision(RevisionGit revision) {
        this.revision = revision;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
