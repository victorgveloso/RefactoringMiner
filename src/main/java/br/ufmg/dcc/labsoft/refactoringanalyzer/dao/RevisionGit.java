package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.util.Date;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import org.eclipse.jgit.revwalk.RevCommit;

@Entity
@NamedQueries({
	@NamedQuery(name = "revisionGit.findByProjectAndCommit", query = "SELECT i FROM RevisionGit i where i.project = :project and i.commitId = :commitId"),
	@NamedQuery(name = "revisionGit.findByProjectAndExtractMethod", query = "select distinct rev from RefactoringGit ref join ref.revision as rev join rev.project as p where refactoringType in ('Extract Operation', 'Extract & Move Operation') and p.cloneUrl = :cloneUrl")
})
@Table(
	name = "revisiongit",
	uniqueConstraints = {@UniqueConstraint(columnNames = {"project", "commitId"})}
)
public class RevisionGit extends AbstractEntity {
	public enum Status {
		NEW,
		SEEN,
		AUTHOR_CONTACTED,
		EMAIL_SENT
	}

	private String commitId;
	private String commitIdParent;
	private String committerName;
	private String committerEmail;               
	private String authorName;
	private String authorEmail;               
	private String encoding;                
	private Date commitTime;
	private int linesAdded;
	private int linesRemoved;

	private Boolean mentionsRefactoring;
	private Boolean describesRefactoring;
	private Boolean describesFeatureOrBug;
	private Boolean ok;
	
	@Column(columnDefinition="TEXT")
	private String FullMessage;

	@Column(length = 5000)
	private String shortMessage;

	@ManyToOne(cascade = CascadeType.PERSIST) 
	@JoinColumn(name="project")
	private ProjectGit project; 

	@OneToMany(mappedBy = "revision", targetEntity = RefactoringGit.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<RefactoringGit> refactorings;

	@OneToMany(mappedBy = "revision", targetEntity = SurveyMail.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<SurveyMail> surveyMails;

	@Enumerated(EnumType.STRING)
	@Column(columnDefinition = "VARCHAR(30) DEFAULT 'NEW'")
	private Status status = Status.NEW;

	public RevisionGit() {
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public ProjectGit getProjectGit() {
		return project;
	}

	public void setProjectGit(ProjectGit projectGit) {
		this.project = projectGit;
	}


	public String getIdCommit() {
		return commitId;
	}

	public void setIdCommit(String idCommit) {
		this.commitId = idCommit;
	}

	public String getIdCommitParent() {
		return commitIdParent;
	}

	public void setIdCommitParent(String idCommitPai) {
		this.commitIdParent = idCommitPai;
	}

	public String getAuthorEmail() {
		return authorEmail;
	}

	public void setAuthorEmail(String authorIdent) {
		this.authorEmail = authorIdent;
	}

	public String getCommiterEmail() {
		return committerEmail;
	}
	
	public void setCommitterEmail(String authorIdent) {
		this.committerEmail = authorIdent;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public Date getCommitTime() {
		return commitTime;
	}

	public void setCommitTime(Date commitTime) {
		this.commitTime = commitTime;
	}

	public String getFullMessage() {
		return FullMessage;
	}

	public void setFullMessage(String FullMessage) {
		this.FullMessage = FullMessage;
	}

	public String getShortMessage() {
		return shortMessage;
	}

	public void setShortMessage(String shortMessage) {
		this.shortMessage = shortMessage;
	}

	public Set<RefactoringGit> getRefactorings() {
		return refactorings;
	}

	public void setRefactorings(Set<RefactoringGit> refactorygit) {
		this.refactorings = refactorygit;
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public String getCommiterName() {
		return committerName;
	}
	
	public void setCommitterName(String authorName) {
		this.committerName = authorName;
	}

	public Boolean getMentionsRefactoring() {
		return mentionsRefactoring;
	}

	public void setMentionsRefactoring(Boolean mentionsRefactoring) {
		this.mentionsRefactoring = mentionsRefactoring;
	}

	public Boolean getDescribesRefactoring() {
		return describesRefactoring;
	}

	public void setDescribesRefactoring(Boolean describesRefactoring) {
		this.describesRefactoring = describesRefactoring;
	}

	public Boolean getDescribesFeatureOrBug() {
		return describesFeatureOrBug;
	}

	public void setDescribesFeatureOrBug(Boolean describesFeatureOrBug) {
		this.describesFeatureOrBug = describesFeatureOrBug;
	}

	public Boolean getOk() {
		return ok;
	}

	public void setOk(Boolean ok) {
		this.ok = ok;
	}
	
	public void setLinesAdded(int linesAdded) {
		this.linesAdded = linesAdded;
	}
	
	public int getLinesAdded() {
		return this.linesAdded;
	}
	
	public void setLinesRemoved(int linesRemoved) {
		this.linesRemoved = linesRemoved;
	}
	
	public int getLinesRemoved() {
		return this.linesRemoved;
	}

	public static RevisionGit getFromRevCommit(RevCommit curRevision, ProjectGit project) {
		RevisionGit revision = new RevisionGit();
		revision.setProjectGit(project);
		revision.setIdCommit(curRevision.getId().getName());
		revision.setAuthorName(curRevision.getAuthorIdent().getName());
		revision.setAuthorEmail(curRevision.getAuthorIdent().getEmailAddress());
		revision.setCommitterName(curRevision.getCommitterIdent().getName());
		revision.setCommitterEmail(curRevision.getCommitterIdent().getEmailAddress());
		revision.setEncoding(curRevision.getEncoding().name());
		revision.setIdCommitParent(curRevision.getParent(0).getId().getName());
		if (curRevision.getShortMessage().length() >= 4999) {
			revision.setShortMessage(curRevision.getShortMessage().substring(0, 4999));
		} else {
			revision.setShortMessage(curRevision.getShortMessage());
		}
		String fullMessage = curRevision.getFullMessage();
		if (fullMessage.length() > 10000) {
			revision.setFullMessage(fullMessage.substring(0, 10000));
		} else {
			revision.setFullMessage(fullMessage);
		}
		revision.setCommitTime(new java.util.Date((long) curRevision.getCommitTime() * 1000));
		return revision;
	}

}
