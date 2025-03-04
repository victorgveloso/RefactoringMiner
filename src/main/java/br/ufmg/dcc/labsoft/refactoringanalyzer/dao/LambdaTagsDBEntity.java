package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "lambda_tags",
		uniqueConstraints = {
			@UniqueConstraint(columnNames = { "lambda", "user", "tag" })
		},
		indexes = {
				@Index(name = "index_lambdatag_lambda", columnList = "lambda"),
				@Index(name = "index_lambdatag_user", columnList = "user"),
				@Index(name = "index_lambdatag_tag", columnList = "tag")
		}
)
public class LambdaTagsDBEntity extends AbstractEntity {

	private static final long serialVersionUID = 2789968607998931526L;
	
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "lambda")
	private LambdaDBEntity lambda;

	@ManyToOne(cascade = CascadeType.PERSIST) 
	@JoinColumn(name="user")
	private User user;

	@ManyToOne(cascade = CascadeType.PERSIST) 
	@JoinColumn(name="tag")
	private Tag tag;
	
	public LambdaDBEntity getLambda() {
		return lambda;
	}
	
	public User getUser() {
		return user;
	}
	
	public Tag getTag() {
		return tag;
	}

}
