package com.kienast.authservice.dto;

import com.kienast.authservice.rest.api.model.TokenModel;

public class TokenAdapter {
	private String token;
	private String username;

	public TokenAdapter(String token, String username) {
		this.token = token;
		this.username = username;
	}

	public TokenModel createJson() {
		return new TokenModel().token(token).username(username);
	}
}
