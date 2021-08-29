package com.kienast.authservice.controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.kienast.authservice.dto.TokenAdapter;
import com.kienast.authservice.exception.BusinessValidationException;
import com.kienast.authservice.exception.NotAuthorizedException;
import com.kienast.authservice.exception.WrongCredentialsException;
import com.kienast.authservice.model.App;
import com.kienast.authservice.model.User;
import com.kienast.authservice.model.User2App;
import com.kienast.authservice.repository.User2AppRepository;
import com.kienast.authservice.repository.UserRepository;
import com.kienast.authservice.rest.api.AuthApi;
import com.kienast.authservice.rest.api.model.AllowedApplicationModel;
import com.kienast.authservice.rest.api.model.AuthenticationModel;
import com.kienast.authservice.rest.api.model.ChangedModel;
import com.kienast.authservice.rest.api.model.JWTTokenModel;
import com.kienast.authservice.rest.api.model.LoginModel;
import com.kienast.authservice.rest.api.model.PasswordModel;
import com.kienast.authservice.rest.api.model.ResettedModel;
import com.kienast.authservice.rest.api.model.TokenVerifiyResponseModel;
import com.kienast.authservice.rest.api.model.TokenVerifiyResponseModel.MfaActionEnum;
import com.kienast.authservice.rest.api.model.UserModel;
import com.kienast.authservice.service.TokenService;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class AuthController implements AuthApi {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private User2AppRepository userAppRepository;

	@Autowired
	private TokenService tokenService;

	private static Logger logger = LogManager.getLogger(AppController.class.getName());

	@Value("${logging.level.com.kienast.authservice}")
	private String loglevel;

	@Override
	@Operation(description = "Authenticate a customer")
	public ResponseEntity<AuthenticationModel> authenticate(String xRequestID, String SOURCE_IP,
			@Valid LoginModel loginModel) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Authenticate User)");

		AuthenticationModel response = new AuthenticationModel();
		User user = null;
		try {
			logger.info("Check if User and pw exists");
			user = findByUsernameAndPassword(loginModel.getUsername(), loginModel.getPassword());
			if (StringUtils.isEmpty(user.getUUID())) {
				user.setUUID(UUID.randomUUID().toString().replace("-", ""));
				userRepository.save(user);
			}

			initializeLogInfo(xRequestID, SOURCE_IP, String.valueOf(user.getId()));
			logger.info("Added UserId to log");

			checkUserPassword(loginModel.getPassword());

			List<User2App> userApps = findUserApps(user);

			List<AllowedApplicationModel> allowedApplicatiions = new ArrayList<>();

			logger.info("Search for apps of that user");
			for (User2App ua : userApps) {
				App app = ua.getApp();
				AllowedApplicationModel allowedAppliation = new AllowedApplicationModel();
				allowedAppliation.setId(app.getId().toString());
				allowedAppliation.setAppname(app.getAppname());
				allowedAppliation.setUrl(app.getUrl());
				if (app.getCssClasses() == null) {
					allowedAppliation.setCssClasses("");
				} else {
					allowedAppliation.setCssClasses(app.getCssClasses());
				}
				allowedApplicatiions.add(allowedAppliation);
			}

			response.setAllowedApplicationList(allowedApplicatiions);

		} catch (java.util.NoSuchElementException e) {
			logger.error(e.getMessage());
			throw (new NotAuthorizedException(loginModel.getUsername()));
		}

		String userCred = "";

		try {
			logger.info("Try to generate a Token for the user");
			userCred = tokenService.generateToken(user.getUUID().toString());
		} catch (WrongCredentialsException e) {
			logger.error("Token generation failed" + e.getMessage());
			throw (new NotAuthorizedException(user.getUsername()));
		}

		JWTTokenModel tokenModel = new TokenAdapter(userCred, user.getUsername()).createJson();

		response.setToken(tokenModel.getJwt());

		logger.info("Token was generated. Authentication was successfull.");
		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "Register a customer")
	public ResponseEntity<JWTTokenModel> register(String JWT, String xRequestID, String SOURCE_IP,
			@Valid LoginModel loginModel) throws NotAuthorizedException {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Register User)");

		logger.info("Try to validate Token for Request (Register User)");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		User user = null;
		try {
			logger.info("Check if User not already exists");
			user = findByUsernameAndPassword(loginModel.getUsername(), loginModel.getPassword());
		} catch (java.util.NoSuchElementException e) {
			logger.debug("Error on searching: " + e.getMessage());
		}

		User entity = null;

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.WEEK_OF_YEAR, +1);
		long nextWeek = calendar.getTime().getTime();

		if (user == null) {
			if (loginModel.getUsername() != null && loginModel.getPassword() != null) {
				entity = saveNewUser(loginModel, nextWeek);
			} else {
				logger.error("Some data was missing");
				throw (new NotAuthorizedException("Data missing"));
			}

		} else {
			logger.error("User already exists");
			throw (new NotAuthorizedException("already exists"));
		}

		String userCred = "";

		try {
			logger.info("Try to generate a Token for the user");
			userCred = tokenService.generateToken(entity.getUUID().toString());
		} catch (WrongCredentialsException e) {
			logger.error("Token generation failed" + e.getMessage());
			throw (new NotAuthorizedException(entity.getUsername()));
		}

		JWTTokenModel response = new TokenAdapter(userCred, entity.getUsername()).createJson();

		logger.info("User was successfully added");
		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "Verify JWT")
	public ResponseEntity<TokenVerifiyResponseModel> verifyToken(String JWT, String xRequestID, String SOURCE_IP,
			@Valid JWTTokenModel tokenModel) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Verify Jwt)");

		TokenVerifiyResponseModel response = new TokenVerifiyResponseModel();

		logger.info("Try to validate the JWT");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		User user = findByUsername(tokenModel.getUsername());

		// MFA needed if not logged in until now
		if (!user.hasAlreadyLoggedIn()) {
			logger.debug("User needs to log in and setup MFA");
			response.setMfaNeeded(true);
			response.setMfaAction(MfaActionEnum.SETUP);
			user.setAlreadyLoggedIn(true);
			// No secret already --> setup needed
		} else if (user.getSecret() == null) {
			logger.debug("User needs to log in since there is no secret");
			response.setMfaNeeded(true);
			response.setMfaAction(MfaActionEnum.SETUP);
			// Verification Time is there
		} else if (user.getNextVerification().before(new Timestamp(Calendar.getInstance().getTime().getTime()))) {
			logger.debug("User needs to log in again since verification time exceeded");
			response.setMfaNeeded(true);
			response.setMfaAction(MfaActionEnum.VERIFIY);
			// Check if the user is already logged ind
		} else {
			logger.debug("User does not need a MFA Action");
			response.setMfaNeeded(false);
		}

		user.setLoggedIn(true);
		userRepository.save(user);

		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "Reset Mfa")
	public ResponseEntity<ResettedModel> resetMfa(String username, String JWT, String xRequestID, String SOURCE_IP,
			@Valid JWTTokenModel tokenModel) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Reset MFA)");

		ResettedModel response = new ResettedModel();
		User user = null;

		logger.info("Try to validate the JWT");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		try {
			logger.info("Check if user exists");
			user = findByUsername(username);
		} catch (java.util.NoSuchElementException e) {
			logger.error(e.getMessage());
			throw (new NotAuthorizedException(username));
		}

		try {
			logger.info("Try to update verification time on user");
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.WEEK_OF_YEAR, -1);
			long lastWeek = calendar.getTime().getTime();

			user.setNextVerification(new Timestamp(lastWeek));
			user.setLoggedIn(false);
			userRepository.save(user);

			response.setResetted(true);
		} catch (Exception e) {
			logger.error("Error on update verification time occured");
			response.setResetted(false);
		}

		return ResponseEntity.ok(response);

	}

	@Override
	@Operation(description = "Change Password")
	public ResponseEntity<ChangedModel> changePassword(String username, String JWT, String xRequestID, String SOURCE_IP,
			@Valid PasswordModel passwordModel) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Change PW)");

		logger.info("Try to validate the JWT");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		ChangedModel response = new ChangedModel();

		User user = null;

		try {
			logger.info("Check if user exists");
			user = findByUsername(username);
		} catch (java.util.NoSuchElementException e) {
			logger.error(e.getMessage());
			throw (new NotAuthorizedException(username));
		}

		checkUserPassword(passwordModel.getPassword());
		

		try {
			logger.info("Try to set the new password");
			if (!passwordModel.getPassword().isEmpty()) {
				user.setPassword(hashPassword(passwordModel.getPassword()));
				userRepository.save(user);
				response.setChanged(true);
			} else {
				logger.error("Password is empty");
				throw (new NotAuthorizedException("password is empty"));
			}

		} catch (Exception e) {
			logger.info("Error occured: " + e.getMessage());
			response.setChanged(false);
		}

		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "Get all users")
	public ResponseEntity<List<UserModel>> getUsers(String JWT, String xRequestID, String SOURCE_IP) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Get all Users)");

		logger.info("Try to validate Token for Request (Get Users)");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		List<UserModel> usersResponse = new ArrayList<>();
		List<User> users = userRepository.findAll();

		logger.info("Try to search for the Users");
		for (User u : users) {
			UserModel model = new UserModel();
			model.setId(u.getId().toString());
			model.setUsername(u.getUsername());
			usersResponse.add(model);
		}

		logger.info("Retrieval was succesfull");
		return ResponseEntity.ok(usersResponse);
	}

	private List<User2App> findUserApps(User user) {
		return userAppRepository.findAll().stream().filter(item -> item.getUser().getId().equals(user.getId()))
				.collect(Collectors.toList());
	}

	private User findByUsername(String username) {
		return userRepository.findAll().stream().filter(item -> item.getUsername().equals(username)).findFirst().get();
	}

	private User findByUsernameAndPassword(String username, String password) {
		return userRepository.findAll().stream()
				.filter(item -> item.getUsername().equals(username) && checkPass(password, item.getPassword())).findFirst()
				.get();
	}

	private String hashPassword(String plainTextPassword) {
		return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
	}

	private boolean checkPass(String plainPassword, String hashedPassword) {
		return BCrypt.checkpw(plainPassword, hashedPassword);
	}

	private void initializeLogInfo(String requestId, String sourceIP, String userId) {
		MDC.put("SYSTEM_LOG_LEVEL", loglevel);
		MDC.put("REQUEST_ID", requestId);
		MDC.put("SOURCE_IP", sourceIP);
		MDC.put("USER_ID", userId);
	}

	private User saveNewUser(LoginModel loginModel, long nextWeek) {
		User entity;
		checkUserPassword(loginModel.getPassword());
		logger.info("Try to add new user");
		entity = userRepository
				.save(new User(loginModel.getUsername(), hashPassword(loginModel.getPassword()), new Timestamp(nextWeek), // nextVerifications
						false, // alreadyLoggedIn
						UUID.randomUUID().toString().replace("-", "")));
		return entity;
	}

	private void checkUserPassword(String password) {
		List<String> validationMessages = new ArrayList<>();

		logger.info("Check that User Password is more than 10 characters");

		if(password.length() < 9){
			validationMessages.add("Password is too short");
			logger.warn("Password is too short");
		}

		if (!validationMessages.isEmpty()){

			throw new BusinessValidationException(String.join(", ", validationMessages));
		}
	}

}

