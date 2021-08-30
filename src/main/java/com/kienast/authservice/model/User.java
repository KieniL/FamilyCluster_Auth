package com.kienast.authservice.model;

import java.sql.Timestamp;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User extends AuditModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@NotBlank
	@Size(min = 1, max = 40)
	@Column(name = "uuid", columnDefinition = "text")
	private String uuid;

	@NotBlank
	@Size(min = 1, max = 40)
	@Column(name = "password")
	private String password;

	@NotBlank
	@Size(min = 1, max = 100)
	@Column(name = "secret")
	private String secret;

	@NotBlank
	@Size(min = 1, max = 40)
	@Column(name = "username", columnDefinition = "text")
	private String username;

	@NotBlank
	@Column(name = "next_verification", columnDefinition = "timestamp")
	private Timestamp nextVerification;

	@NotBlank
	@Column(name = "already_logged_in", columnDefinition = "boolean default false")
	private boolean alreadyLoggedIn = true;

	@NotBlank
	@Column(name = "logged_in", columnDefinition = "boolean default false")
	private boolean isLoggedIn = true;

	@NotBlank
	@Column(name = "login_attempts", columnDefinition = "integer default 0")
	private int loginAttempts = 0;

	@NotBlank
	@Column(name = "is_blocked", columnDefinition = "boolean default false")
	private boolean isBlocked = false;

	@NotBlank
	@Column(name = "blocked_until", columnDefinition = "timestamp")
	private Timestamp blockedUntil;

	@OneToMany(mappedBy = "user")
	Set<User2App> userApp;

	public User() {
	}

	public User(String username, String password, Timestamp nextVerification, boolean alreadyLoggedIn, String uuid,
			int loginAttempts, boolean isBlocked) {
		this.password = password;
		this.username = username;
		this.nextVerification = nextVerification;
		this.alreadyLoggedIn = alreadyLoggedIn;
		this.uuid = uuid;
		this.loginAttempts = loginAttempts;
		this.isBlocked = isBlocked;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Timestamp getNextVerification() {
		return nextVerification;
	}

	public void setNextVerification(Timestamp nextVerification) {
		this.nextVerification = nextVerification;
	}

	public boolean hasAlreadyLoggedIn() {
		return alreadyLoggedIn;
	}

	public void setAlreadyLoggedIn(boolean alreadyLoggedIn) {
		this.alreadyLoggedIn = alreadyLoggedIn;
	}

	public boolean isLoggedIn() {
		return isLoggedIn;
	}

	public void setLoggedIn(boolean isLoggedIn) {
		this.isLoggedIn = isLoggedIn;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public Set<User2App> getUserApp() {
		return userApp;
	}

	public void setUserApp(Set<User2App> userApp) {
		this.userApp = userApp;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public boolean isAlreadyLoggedIn() {
		return alreadyLoggedIn;
	}

	public int getLoginAttempts() {
		return loginAttempts;
	}

	public void setLoginAttempts(int loginAttempts) {
		this.loginAttempts = loginAttempts;
	}

	public boolean isBlocked() {
		return isBlocked;
	}

	public void setBlocked(boolean isBlocked) {
		this.isBlocked = isBlocked;
	}

	public Timestamp getBlockedUntil() {
		return blockedUntil;
	}

	public void setBlockedUntil(Timestamp blockedUntil) {
		this.blockedUntil = blockedUntil;
	}

	
}
