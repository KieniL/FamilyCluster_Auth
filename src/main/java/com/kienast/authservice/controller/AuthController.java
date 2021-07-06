package com.kienast.authservice.controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.kienast.authservice.dto.TokenAdapter;
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
import com.kienast.authservice.rest.api.model.LoginModel;
import com.kienast.authservice.rest.api.model.PasswordModel;
import com.kienast.authservice.rest.api.model.ResettedModel;
import com.kienast.authservice.rest.api.model.TokenModel;
import com.kienast.authservice.rest.api.model.TokenVerifiyResponseModel;
import com.kienast.authservice.rest.api.model.TokenVerifiyResponseModel.MfaActionEnum;
import com.kienast.authservice.rest.api.model.UserModel;
import com.kienast.authservice.service.TokenService;

import io.swagger.v3.oas.annotations.Operation;


@RestController
public class AuthController implements AuthApi{

	
	@Autowired
    private UserRepository userRepository;
	
	@Autowired
	private User2AppRepository userAppRepository;

	
	@Autowired
	private TokenService tokenService;
	
	
	@Override
	@Operation(description = "Authenticate a customer")
	public ResponseEntity<AuthenticationModel> authenticate(@Valid LoginModel loginModel) {
		
		AuthenticationModel response = new AuthenticationModel();
		User user = null;
		try {
			user = findByUsernameAndPassword(loginModel.getUsername(), loginModel.getPassword());
			List<User2App> userApps = findUserApps(user);
			
			List<AllowedApplicationModel> allowedApplicatiions = new ArrayList<>();
			
			for(User2App ua: userApps) {
				App app = ua.getApp();
				AllowedApplicationModel allowedAppliation = new AllowedApplicationModel();
				allowedAppliation.setId(app.getId().toString());
				allowedAppliation.setAppname(app.getAppname());
				allowedAppliation.setUrl(app.getUrl());
				if(app.getCssClasses() == null) {
					allowedAppliation.setCssClasses("");
				}else {
					allowedAppliation.setCssClasses(app.getCssClasses());
				}
				allowedApplicatiions.add(allowedAppliation);
			}
			
			response.setAllowedApplicationList(allowedApplicatiions);

			
		}catch(java.util.NoSuchElementException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw(new NotAuthorizedException(loginModel.getUsername()));
		}
		 
		
		String userCred = "";
		
		try{
			userCred = tokenService.generateToken(user.getUsername());
		}catch(WrongCredentialsException e) {
			e.printStackTrace();
			throw(new NotAuthorizedException(user.getUsername()));
		}
		
		TokenModel tokenModel = new TokenAdapter(userCred, user.getUsername()).createJson();
		
		response.setToken(tokenModel.getToken());
		
		
		
		
		return ResponseEntity.ok(response);
	}

	

	@Override
	@Operation(description = "Register a customer")
	public ResponseEntity<TokenModel> register(@Valid LoginModel loginModel) throws NotAuthorizedException {
		
		User user = null;
		try {
			user = findByUsernameAndPassword(loginModel.getUsername(), loginModel.getPassword());
		}catch(java.util.NoSuchElementException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
		User entity = null;
		
		
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.WEEK_OF_YEAR, +1);
		long nextWeek = calendar.getTime().getTime();
		
		if(user == null) {
			if(loginModel.getUsername() != null && loginModel.getPassword() != null){
				entity = userRepository.save(new User(
					loginModel.getUsername(),
					hashPassword(loginModel.getPassword()),
					new Timestamp(nextWeek), //nextVerifications
					false //alreadyLoggedIn
					));
			}else{
				System.out.println("Data missing");
				throw(new NotAuthorizedException("Data missing"));
			}
			
		}else {
			System.out.println("Already exists");
			throw(new NotAuthorizedException("already exists"));
		}
		
		
		
		
		String userCred = "";
		
		try{
			userCred = tokenService.generateToken(entity.getUsername());
		}catch(WrongCredentialsException e) {
			e.printStackTrace();
			throw(new NotAuthorizedException(entity.getUsername()));
		}
		
		TokenModel response = new TokenAdapter(userCred, entity.getUsername()).createJson();
		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "Verify JWT")
	public ResponseEntity<TokenVerifiyResponseModel> verifyToken(@Valid TokenModel tokenModel) {
		
		TokenVerifiyResponseModel response = new TokenVerifiyResponseModel();

		if (!tokenService.validateToken(tokenModel.getToken())) {
			throw(new NotAuthorizedException(tokenModel.getToken()));
		}
		

		User user = findByUsername(tokenModel.getUsername());
		
		//MFA needed if not logged in until now
		if(!user.hasAlreadyLoggedIn()){
			response.setMfaNeeded(true);
			response.setMfaAction(MfaActionEnum.SETUP);
			user.setAlreadyLoggedIn(true);
		//No secret already --> setup needed
		}else if(user.getSecret() == null) {
			response.setMfaNeeded(true);
			response.setMfaAction(MfaActionEnum.SETUP);
		// Verification Time is there
		}else if(user.getNextVerification().before(new Timestamp(Calendar.getInstance().getTime().getTime()))) {
			response.setMfaNeeded(true);
			response.setMfaAction(MfaActionEnum.VERIFIY);
		//Check if the user is already logged ind
		}
		else {
			response.setMfaNeeded(false);
		}
		
		user.setLoggedIn(true);
		userRepository.save(user);
		
		
		
		return ResponseEntity.ok(response);
	}
	
	
	@Override
	@Operation(description = "Reset Mfa")
	public ResponseEntity<ResettedModel> resetMfa(String username, @Valid TokenModel tokenModel) {
		
		ResettedModel response = new ResettedModel();
		User user = null;
		
		if (!tokenService.validateToken(tokenModel.getToken())) {
			throw(new NotAuthorizedException(tokenModel.getToken()));
		}
		
		
		try {
			user = findByUsername(username);
		}catch(java.util.NoSuchElementException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw(new NotAuthorizedException(username)); 
		}
		
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.WEEK_OF_YEAR, -1);
			long lastWeek = calendar.getTime().getTime();
			
			user.setNextVerification(new Timestamp(lastWeek));
			user.setLoggedIn(false);
			userRepository.save(user);
			
			response.setResetted(true);
		}catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			response.setResetted(false);
		}

		
		
		
		
		return ResponseEntity.ok(response);
		
	}
	
	@Override
	@Operation(description = "Change Password")
	public ResponseEntity<ChangedModel> changePassword(String username, @Valid PasswordModel passwordModel) {
		
		if (!tokenService.validateToken(passwordModel.getJwt())) {
			throw(new NotAuthorizedException(passwordModel.getJwt()));
		}
		
		ChangedModel response = new ChangedModel();
		
		User user = null;
		try {
			user = findByUsername(username);
		}catch(java.util.NoSuchElementException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw(new NotAuthorizedException(username)); 
		}
		
		try {
			if(!passwordModel.getPassword().isEmpty()){
				user.setPassword(hashPassword(passwordModel.getPassword()));
				userRepository.save(user);
				response.setChanged(true);
			}else{
				System.out.println("password is empty");
				throw(new NotAuthorizedException("password is empty")); 
			}
			
		}catch (Exception e) {
			System.out.println(e.getMessage());	
			e.printStackTrace();
			response.setChanged(false);
		}
		
		
		return ResponseEntity.ok(response);
	}
	
	@Override
	@Operation(description = "Get all users")
	public ResponseEntity<List<UserModel>> getUsers() {
		
		List<UserModel> usersResponse = new ArrayList<>();
		List<User> users = userRepository.findAll();
		
		for(User u: users ) {
			UserModel model = new UserModel();
			model.setId(u.getId().toString());
			model.setUsername(u.getUsername());
			usersResponse.add(model);
		}
		
		
		return ResponseEntity.ok(usersResponse);
	}
	
	private List<User2App> findUserApps(User user) {
		return userAppRepository.findAll().stream().filter(item -> item.getUser().getId().equals(user.getId())).collect(Collectors.toList());
	}
	
	private User findByUsername(String username) {
		return userRepository.findAll().stream().filter(
				item -> item.getUsername().equals(username)
				).findFirst().get();
	}

	
	
	private User findByUsernameAndPassword(String username, String password) {
		return userRepository.findAll().stream().filter(
				item -> item.getUsername().equals(username) && checkPass(password, item.getPassword())
				).findFirst().get();
	}
	
	private String hashPassword(String plainTextPassword){
		return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
	}
	
	private boolean checkPass(String plainPassword, String hashedPassword) {
		return BCrypt.checkpw(plainPassword, hashedPassword);
	}















	
	



}
