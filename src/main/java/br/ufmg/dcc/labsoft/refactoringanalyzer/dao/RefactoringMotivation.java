package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "refactoringmotivation",
	uniqueConstraints = {@UniqueConstraint(columnNames = {"refactoring", "tag"})},
	indexes = {
		@Index(name="index_refactoringmotivation_ref", columnList = "refactoring"),
		@Index(name="index_refactoringmotivation_tag", columnList = "tag")
	}
)
public class RefactoringMotivation extends AbstractEntity {

	@ManyToOne
	@JoinColumn(name = "refactoring")
	private RefactoringGit refactoring;
	
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

	public RefactoringGit getRefactoring() {
		return refactoring;
	}

	public void setRefactoring(RefactoringGit refactoring) {
		this.refactoring = refactoring;
	}

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}

}
