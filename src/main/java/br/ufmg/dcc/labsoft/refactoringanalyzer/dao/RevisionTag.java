package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;

@Entity
@Table(
	name = "revisiontag",
	uniqueConstraints = {@UniqueConstraint(columnNames = {"user", "revision", "tag"})}
)
public class RevisionTag extends AbstractEntity {

	@ManyToOne
	@JoinColumn(name = "user")
	@Index(name="index_revisiontag_user")
	private User user;

	@ManyToOne
	@JoinColumn(name = "revision")
	@Index(name="index_revisiontag_revision")
	private RevisionGit revision;
	
	@ManyToOne
	@JoinColumn(name = "tag")
	@Index(name="index_revisiontag_tag")
	private Tag tag;
	
	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(final Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public RevisionGit getRevision() {
		return revision;
	}

	public void setRevision(RevisionGit revision) {
		this.revision = revision;
	}

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}
	
}
