package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "surveyresponse",
		indexes = {
			@Index(name = "index_surveyresponse_fromAddress", columnList = "fromAddress"),
			@Index(name = "index_surveyresponse_survey", columnList = "survey")
		}
)
public class SurveyResponse extends AbstractEntity {

	private String fromAddress;

	private String subject;

	private Date sentDate;
	
	@Column(columnDefinition="TEXT")
	private String bodyPlain;

	@Column(columnDefinition="TEXT")
	private String bodyHtml;
	
	@ManyToOne
	@JoinColumn(name = "survey")
	private SurveyMail survey;
	
	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(final Long id) {
		this.id = id;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String from) {
		this.fromAddress = from;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public String getBodyPlain() {
		return bodyPlain;
	}

	public void setBodyPlain(String bodyPlain) {
		this.bodyPlain = bodyPlain;
	}

	public String getBodyHtml() {
		return bodyHtml;
	}

	public void setBodyHtml(String bodyHtml) {
		this.bodyHtml = bodyHtml;
	}

	public SurveyMail getSurvey() {
		return survey;
	}

	public void setSurvey(SurveyMail survey) {
		this.survey = survey;
	}

}
