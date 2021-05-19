package com.kienast.authservice.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class User2AppKey implements Serializable {

	@Column(name = "user_id")
    Long userId;

    @Column(name = "app_id")
    Long appId;
    
    public User2AppKey() {
    	
    }

	public User2AppKey(Long userId, Long appId) {
		this.userId = userId;
		this.appId = appId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getAppId() {
		return appId;
	}

	public void setAppId(Long appId) {
		this.appId = appId;
	}
    
}
