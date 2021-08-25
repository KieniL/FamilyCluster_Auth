package com.kienast.authservice.controller;

import com.kienast.authservice.exception.NotAuthorizedException;
import com.kienast.authservice.rest.api.JwtApi;
import com.kienast.authservice.rest.api.model.TokenVerificationResponseModel;
import com.kienast.authservice.service.TokenService;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class JwtController implements JwtApi {

	@Autowired
	private TokenService tokenService;

	private static Logger logger = LogManager.getLogger(JwtController.class.getName());

	@Value("${logging.level.com.kienast.authservice}")
	private String loglevel;

	@Value("${companyName}")
	private String companyName;

	@Override
	@Operation(description = "verify JWT")
	public ResponseEntity<TokenVerificationResponseModel> verifyJwt(String JWT, String xRequestID, String SOURCE_IP) {
		TokenVerificationResponseModel tokenVerificationResponse = new TokenVerificationResponseModel();

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Verify JWT)");

		logger.info("Try to validate Token");

		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
			tokenVerificationResponse.setUserId(userId);
		}else{
			throw (new NotAuthorizedException(JWT));
		}

		

		return ResponseEntity.ok(tokenVerificationResponse);
	}

	private void initializeLogInfo(String requestId, String sourceIP, String userId) {
		MDC.put("SYSTEM_LOG_LEVEL", loglevel);
		MDC.put("REQUEST_ID", requestId);
		MDC.put("SOURCE_IP", sourceIP);
		MDC.put("USER_ID", userId);
	}

}
