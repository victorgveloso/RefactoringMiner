package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users")
public class User extends AbstractEntity {

	public enum UserRole {
		ADMIN,
		NORMAL
	}
	private static final long serialVersionUID = 7784934699567379073L;

	@Column(name = "userName", nullable = false, unique = true)
	private String userName;
	private String name;
	private String familyName;
	@Column(name = "password", nullable = false)
	private String password;
	private String email;
	@Enumerated(EnumType.STRING)
	private UserRole userRole;

	public String getUserName() {
		return userName;
	}

	public String getName() {
		return name;
	}

	public String getFamilyName() {
		return familyName;
	}

	public String getPassword() {
		return password;
	}
	
	public String getEmail() {
		return email;
	}
	
	public UserRole getUserRole() {
		return userRole;
	}
		
}
