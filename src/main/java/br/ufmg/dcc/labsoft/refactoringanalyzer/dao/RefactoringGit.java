package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import gr.uom.java.xmi.diff.CodeRange;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@NamedQueries({
		@NamedQuery(
				name = "refactoringGit.extractMethods",
				query = "select ref from RefactoringGit ref join ref.revision as rev join rev.project as p where ref.refactoringType in ('Extract Operation', 'Extract & Move Operation') and rev.commitId = :commitId and p.cloneUrl = :cloneUrl"
		)
})
@Table(name = "refactoringgit",
		indexes = {
				@Index(name = "index_refactoringgit_revision", columnList = "revision"),
				@Index(name = "index_refactoringgit_entity", columnList = "entity")
		})
public class RefactoringGit extends AbstractEntity {

	private String refactoringType;

	@Column(length = 15000)
	private String description;

	@ManyToOne
	@JoinColumn(name = "revision")
	private RevisionGit revision;

	@OneToMany(mappedBy = "refactoring", targetEntity = CodeRangeGit.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<CodeRangeGit> codeRanges = new ArrayList<>();

	private Boolean truePositive;

	@Column(length = 255)
	private String entity;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(final Long id) {
		this.id = id;
	}

	public String getRefactoringType() {
		return refactoringType;
	}

	public void setRefactoringType(String tipoOperacao) {
		this.refactoringType = tipoOperacao;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String operacaoCompleta) {
		this.description = operacaoCompleta;
	}

	public RevisionGit getRevision() {
		return revision;
	}

	public void setRevision(RevisionGit revisiongit) {
		this.revision = revisiongit;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 89 * hash + Objects.hashCode(this.refactoringType);
		hash = 89 * hash + Objects.hashCode(this.revision);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final RefactoringGit other = (RefactoringGit) obj;
		if (!Objects.equals(this.description, other.description)) {
			return false;
		}
		if (!Objects.equals(this.revision, other.revision)) {
			return false;
		}
		return true;
	}

	public Boolean getTruePositive() {
		return truePositive;
	}

	public void setTruePositive(Boolean truePositive) {
		this.truePositive = truePositive;
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}

	public void setCodeRangeBefore(List<CodeRange> leftSide) {
		codeRanges.addAll(leftSide.stream().map(codeRange ->
				CodeRangeGit.fromCodeRange(codeRange, this, CodeRangeGit.DiffSide.LEFT)
		).collect(Collectors.toList()));
	}

	public void setCodeRangeAfter(List<CodeRange> rightSide) {
		codeRanges.addAll(rightSide.stream().map(codeRange ->
				CodeRangeGit.fromCodeRange(codeRange, this, CodeRangeGit.DiffSide.RIGHT)
		).collect(Collectors.toList()));
	}
}
