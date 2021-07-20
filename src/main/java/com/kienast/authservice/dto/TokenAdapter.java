package com.kienast.authservice.dto;

import com.kienast.authservice.rest.api.model.JWTTokenModel;

public class TokenAdapter {
	private String token;
	private String username;

	public TokenAdapter(String token, String username) {
		this.token = token;
		this.username = username;
	}

	public JWTTokenModel createJson() {
		return new JWTTokenModel().jwt(token).username(username);
	}
}
