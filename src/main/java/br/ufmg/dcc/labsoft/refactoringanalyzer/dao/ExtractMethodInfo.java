package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

//DISABLED
//@Entity
@NamedQueries({
})
@Table(
	name = "extractmethodinfo",
	uniqueConstraints = {@UniqueConstraint(columnNames = {"project", "method"})}
)
public class ExtractMethodInfo extends AbstractEntity {

	@ManyToOne(cascade = CascadeType.PERSIST) 
	@JoinColumn(name="project")
	private ProjectGit project;
	@Column(length = 500)
	private String method;
	private int countDup;
	private int countInit;
	private int countCurrent;
	private int countMax;
	private String revExtracted;
	private boolean dead;
	private boolean extracted;
	private boolean moved;
	private String visibility;

	public ProjectGit getProject() {
		return project;
	}
	public void setProject(ProjectGit project) {
		this.project = project;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public int getCountDup() {
		return countDup;
	}
	public void setCountDup(int countDup) {
		this.countDup = countDup;
	}
	public int getCountInit() {
		return countInit;
	}
	public void setCountInit(int countInit) {
		this.countInit = countInit;
	}
	public int getCountCurrent() {
		return countCurrent;
	}
	public void setCountCurrent(int countCurrent) {
		this.countCurrent = countCurrent;
	}
	public int getCountMax() {
		return countMax;
	}
	public void setCountMax(int countMax) {
		this.countMax = countMax;
	}
	public String getRevExtracted() {
		return revExtracted;
	}
	public void setRevExtracted(String revExtracted) {
		this.revExtracted = revExtracted;
	}
	public boolean isDead() {
		return dead;
	}
	public void setDead(boolean dead) {
		this.dead = dead;
	}
	public boolean isExtracted() {
		return extracted;
	}
	public void setExtracted(boolean extracted) {
		this.extracted = extracted;
	}
	public boolean isMoved() {
		return moved;
	}
	public void setMoved(boolean moved) {
		this.moved = moved;
	}
	public String getVisibility() {
		return visibility;
	}
	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}
	
}
