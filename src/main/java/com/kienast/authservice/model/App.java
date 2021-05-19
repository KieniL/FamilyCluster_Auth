package com.kienast.authservice.model;

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
@Table(name = "apps")
public class App extends AuditModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
    private Long id;
	
	@NotBlank
	@Size(min = 1, max = 40)
	@Column(name = "appname", columnDefinition = "text")
    private String appname;
	
	
	@NotBlank
	@Size(min = 1, max = 800)
	@Column(name = "url", columnDefinition = "text")
    private String url;
	
	
	@Size(min = 1, max = 800)
	@Column(name = "cssclasses", columnDefinition = "text")
    private String cssClasses;
	
	@OneToMany(mappedBy = "app")
    Set<User2App> userApp;
	
	public App() {
		
	}


	public App(String appname, String url, String cssClasses) {
		this.appname = appname;
		this.url = url;
		this.cssClasses = cssClasses;
	}


	public String getAppname() {
		return appname;
	}


	public void setAppname(String appname) {
		this.appname = appname;
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public Set<User2App> getUserApp() {
		return userApp;
	}


	public void setUserApp(Set<User2App> userApp) {
		this.userApp = userApp;
	}


	public String getCssClasses() {
		return cssClasses;
	}


	public void setCssClasses(String cssClasses) {
		this.cssClasses = cssClasses;
	}
	
	
	
}
