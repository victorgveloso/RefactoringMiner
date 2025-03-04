package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "projectsadditionalinfo",
		indexes = {
			@Index(name = "index_project_additional_info", columnList = "project")
		}
)
public class ProjectsAdditionalInfo extends AbstractEntity {

	private static final long serialVersionUID = -5254386622041377955L;
	
	@ManyToOne(cascade = CascadeType.PERSIST) 
	@JoinColumn(name = "project")
	private ProjectGit project;
	
	private double lambdaDensityPerClass;
	private double lambdaDensityPerMethod;
	private double numberOfLambdasInHead;
	
	public ProjectGit getProject() {
		return project;
	}
	public double getLambdaDensityPerClass() {
		return lambdaDensityPerClass;
	}
	public double getLambdaDensityPerMethod() {
		return lambdaDensityPerMethod;
	}
	public double getNumberOfLambdasInHead() {
		return numberOfLambdasInHead;
	}
	
}
