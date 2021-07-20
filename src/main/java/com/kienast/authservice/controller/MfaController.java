package com.kienast.authservice.controller;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import com.kienast.authservice.exception.NotAuthorizedException;
import com.kienast.authservice.model.User;
import com.kienast.authservice.repository.UserRepository;
import com.kienast.authservice.rest.api.MfaApi;
import com.kienast.authservice.rest.api.model.JWTTokenModel;
import com.kienast.authservice.rest.api.model.MFATokenVerificationModel;
import com.kienast.authservice.rest.api.model.QRCodeModel;
import com.kienast.authservice.rest.api.model.VerifiedModel;
import com.kienast.authservice.service.TokenService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import io.swagger.v3.oas.annotations.Operation;

@RestController
public class MfaController implements MfaApi {

	@Autowired
	private SecretGenerator secretGenerator;

	@Autowired
	private QrDataFactory qrDataFactory;

	@Autowired
	private QrGenerator qrGenerator;

	@Autowired
	private CodeVerifier verifier;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TokenService tokenService;

	private static Logger logger = LogManager.getLogger(AppController.class.getName());

	@Value("${logging.level.com.kienast.authservice}")
	private String loglevel;

	@Value("${companyName}")
	private String companyName;

	@Override
	@Operation(description = "setup MFA")
	public ResponseEntity<QRCodeModel> mfaSetup(String JWT, String xRequestID, String SOURCE_IP,
			@Valid JWTTokenModel tokenModel) {

		initializeLogInfo(xRequestID, SOURCE_IP, "1");
		logger.info("Got Request (MFA Setup)");

		User user = null;

		logger.info("Try to validate Token for Request (MFA Setup Application)");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}

		try {
			user = findByUsername(tokenModel.getUsername());
		} catch (java.util.NoSuchElementException e) {
			logger.error("User does not exist" + e.getMessage());
			throw (new NotAuthorizedException(tokenModel.getUsername()));
		}

		// Generate and store the secret
		String secret = secretGenerator.generate();

		user.setSecret(secret);
		userRepository.save(user);

		logger.info("Try to generate QR Code for MFA Setup");
		QrData data = qrDataFactory.newBuilder().label(user.getUsername()).secret(secret).issuer(companyName).build();

		// Generate the QR code image data as a base64 string which
		// can be used in an <img>

		String qrCodeImage = "";
		try {
			qrCodeImage = getDataUriForImage(qrGenerator.generate(data), qrGenerator.getImageMimeType());
		} catch (QrGenerationException e) {
			logger.debug("Error on QR Generation" + e.getMessage());
		}
		// return qrCodeImage;
		Map<String, Object> map = new HashMap<>();
		map.put("qrCode", qrCodeImage);

		QRCodeModel qrCodeModel = new QRCodeModel();
		qrCodeModel.setQrcode(map.get("qrCode").toString());

		logger.info("QR Generation was successfull");
		return ResponseEntity.ok(qrCodeModel);
	}

	@Override
	@Operation(description = "verify MFA")
	public ResponseEntity<VerifiedModel> mfaVerify(String JWT, String xRequestID, String SOURCE_IP,
			@Valid MFATokenVerificationModel mfATokenVerificationModel) {

		initializeLogInfo(xRequestID, SOURCE_IP, "1");
		logger.info("Got Request (MFA Verify)");

		VerifiedModel verified = new VerifiedModel();
		User user = null;

		logger.info("Try to validate Token for Request (MFA Verify)");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}

		try {
			user = findByUsername(mfATokenVerificationModel.getUsername());
		} catch (java.util.NoSuchElementException e) {
			logger.error("User does not exist" + e.getMessage());
			throw (new NotAuthorizedException(mfATokenVerificationModel.getUsername()));
		}

		if (verifier.isValidCode(user.getSecret(), mfATokenVerificationModel.getMfaToken())) {
			logger.info("User does have a valid MFA Code");
			verified.setVerificationMessage("CORRECT CODE");
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.WEEK_OF_YEAR, +1);
			long nextWeek = calendar.getTime().getTime();

			user.setNextVerification(new Timestamp(nextWeek));
			userRepository.save(user);
		} else {
			logger.warn("User does not have a valid MFA Code");
			verified.setVerificationMessage("INCORRECT CODE");
		}

		return ResponseEntity.ok(verified);

	}

	private User findByUsername(String username) {
		return userRepository.findAll().stream().filter(item -> item.getUsername().equals(username)).findFirst().get();
	}

	private void initializeLogInfo(String requestId, String sourceIP, String userId) {
		MDC.put("SYSTEM_LOG_LEVEL", loglevel);
		MDC.put("REQUEST_ID", requestId);
		MDC.put("SOURCE_IP", sourceIP);
		MDC.put("USER_ID", userId);
	}

}
