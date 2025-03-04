package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "revisiontag",
	uniqueConstraints = {@UniqueConstraint(columnNames = {"user", "revision", "tag"})},
	indexes = {
		@Index(name="index_revisiontag_user", columnList = "user"),
		@Index(name="index_revisiontag_revision", columnList = "revision"),
		@Index(name="index_revisiontag_tag", columnList = "tag")
	}
)
public class RevisionTag extends AbstractEntity {

	@ManyToOne
	@JoinColumn(name = "user")
	private User user;

	@ManyToOne
	@JoinColumn(name = "revision")
	private RevisionGit revision;
	
	@ManyToOne
	@JoinColumn(name = "tag")
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
