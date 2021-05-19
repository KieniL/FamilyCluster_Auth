package com.kienast.authservice.model;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

@Entity
public class User2App {

	@EmbeddedId
    User2AppKey id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne
    @MapsId("appId")
    @JoinColumn(name = "app_id")
    App app;
    
    public User2App() {
    	
    }

	public User2App(User2AppKey id, User user, App app) {
		this.id = id;
		this.user = user;
		this.app = app;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public App getApp() {
		return app;
	}

	public void setApp(App app) {
		this.app = app;
	}

	public User2AppKey getId() {
		return id;
	}

	public void setId(User2AppKey id) {
		this.id = id;
	}

    
}
