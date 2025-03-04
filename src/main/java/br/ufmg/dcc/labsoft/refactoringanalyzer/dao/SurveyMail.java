package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.util.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "surveymail",
		indexes = {
			@Index(name = "index_surveymail_recipient", columnList = "recipient"),
			@Index(name = "index_surveymail_alternative", columnList = "alternativeAddress"),
			@Index(name = "index_surveymail_sender", columnList = "sender")
		}
)
public class SurveyMail extends AbstractEntity {

	private String recipient;
	
	private String alternativeAddress;
	
	private String sender;
	
	@Column(length = 1024)
	private String subject;

	private Date sentDate;
	
	@Column(columnDefinition="TEXT")
	private String body;

	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "revision")
	private RevisionGit revision;
	
	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(final Long id) {
		this.id = id;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getAlternativeAddress() {
		return alternativeAddress;
	}

	public void setAlternativeAddress(String alternativeAddress) {
		this.alternativeAddress = alternativeAddress;
	}

}
