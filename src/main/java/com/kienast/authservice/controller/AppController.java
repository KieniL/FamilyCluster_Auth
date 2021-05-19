package com.kienast.authservice.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.kienast.authservice.exception.BadRequestException;
import com.kienast.authservice.exception.NotAuthorizedException;
import com.kienast.authservice.model.App;
import com.kienast.authservice.model.User;
import com.kienast.authservice.model.User2App;
import com.kienast.authservice.model.User2AppKey;
import com.kienast.authservice.repository.AppRepository;
import com.kienast.authservice.repository.User2AppRepository;
import com.kienast.authservice.repository.UserRepository;
import com.kienast.authservice.rest.api.AppApi;
import com.kienast.authservice.rest.api.AppOfUserApi;
import com.kienast.authservice.rest.api.model.ApplicationModel;
import com.kienast.authservice.rest.api.model.ApplicationResponseModel;
import com.kienast.authservice.rest.api.model.ApplicationWithoutJwtModel;
import com.kienast.authservice.rest.api.model.UpdateApplicationModel;
import com.kienast.authservice.rest.api.model.UpdatedModel;
import com.kienast.authservice.rest.api.model.VerifiedModel;
import com.kienast.authservice.service.TokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class AppController implements AppApi, AppOfUserApi {

	@Autowired
	private AppRepository appRepository;

	@Autowired
	private User2AppRepository user2AppRepository;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private UserRepository userRepository;

	@Override
	@Operation(description = "Add an application")
	public ResponseEntity<ApplicationModel> addApplication(@Valid ApplicationModel applicationModel) {
		
		ApplicationModel response = new ApplicationModel();
		
		if (!tokenService.validateToken(applicationModel.getJwt())) {
			throw(new NotAuthorizedException(applicationModel.getJwt()));
		}
		
		try {
			if(findAppByName(applicationModel.getAppname()) != null) {
				throw new BadRequestException(applicationModel.getAppname());
			}
		}catch(NoSuchElementException e) {
			e.printStackTrace();
		}
		
		
		App app = new App(applicationModel.getAppname(), applicationModel.getUrl(), applicationModel.getCssClasses());
		appRepository.save(app);
		
		
		for (String s: applicationModel.getAllowedUsers()) {
			User user = findByUsername(s);
			
			user2AppRepository.save(new User2App(new User2AppKey(user.getId(), app.getId()), user, app));
		}
		
		
		List<User2App> user2Apps = findUserApps(app);
		List<String> allowedUsers = new ArrayList<>();
		
		for (User2App user2App : user2Apps) {
			allowedUsers.add(user2App.getUser().getUsername());
		}
		
		

		
		
		response.setAppname(app.getAppname());
		response.setUrl(app.getUrl());
		response.setJwt(applicationModel.getJwt());
		response.setAllowedUsers(allowedUsers);
		
		
		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "update an application")
	public ResponseEntity<ApplicationModel> updateApplication(@Valid UpdateApplicationModel updateApplicationModel) {
		ApplicationModel response = new ApplicationModel();

		if (!tokenService.validateToken(updateApplicationModel.getJwt())) {
			throw (new NotAuthorizedException(updateApplicationModel.getJwt()));
		}

		if (findAppByName(updateApplicationModel.getAppname()) == null) {
			throw new BadRequestException(updateApplicationModel.getAppname());
		}

		App app = findAppByName(updateApplicationModel.getAppname());

		List<User> users = findUsersByUsernameList(updateApplicationModel.getAllowedUsers());

		// Add each element of list into the set
		for (User user : users) {
			user2AppRepository.save(new User2App(new User2AppKey(user.getId(), app.getId()), user, app));
		}

		List<User2App> user2Apps = findUserApps(app);

		Set<User2App> user2AppSet = new HashSet<>();

		// Add each element of list into the set
		for (User2App user2App : user2Apps) {
			if (!updateApplicationModel.getAllowedUsers().contains(user2App.getUser().getUsername())) {
				user2AppRepository.delete(user2App);
			} else {
				user2AppSet.add(user2App);
			}
		}

		app.setUserApp(user2AppSet);
		app.setCssClasses(updateApplicationModel.getCssClasses());
		appRepository.save(app);

		user2Apps = findUserApps(app); // do again to get actual status after delete and add
		List<String> allowedUsers = new ArrayList<>();

		for (User2App user2App : user2Apps) {
			allowedUsers.add(user2App.getUser().getUsername());
		}

		response.setAllowedUsers(allowedUsers);
		response.setAppname(app.getAppname());
		response.setUrl(app.getUrl());
		response.setCssClasses(app.getCssClasses());
		response.setJwt(updateApplicationModel.getJwt());

		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "get an application")
	public ResponseEntity<ApplicationWithoutJwtModel> getApp(String appname) {

		App app = findAppByName(appname);
		ApplicationWithoutJwtModel response = new ApplicationWithoutJwtModel();

		if (app == null) {
			return ResponseEntity.ok(null);
		} else {
			List<String> allowedUsers = findAllowedUsersForApp(app);

			response.setAppname(app.getAppname());
			response.setUrl(app.getUrl());
			if(app.getCssClasses() == null) {
				response.setCssClasses("");
			}else {
				response.setCssClasses(app.getCssClasses());
			}
			response.setAllowedUsers(allowedUsers);
		}

		return ResponseEntity.ok(response);

	}

	@Override
	@Operation(description = "verify user for application")
	public ResponseEntity<VerifiedModel> verifyUserForApp(String appname, String username) {
		App app = findAppByName(appname);
		VerifiedModel response = new VerifiedModel();

		if (app == null) {
			response.setVerificationMessage("not_allowed");
		} else {
			if (checkIfUserInList(app, username)) {
				response.setVerificationMessage("allowed");
			} else {
				response.setVerificationMessage("not_allowed");
			}
		}

		return ResponseEntity.ok(response);

	}
	
	@Override
	@Operation(description = "Add User to App")
	public ResponseEntity<UpdatedModel> addUser2App(String appname, String username) {
		App app = findAppByName(appname);
		User user = findByUsername(username);
		UpdatedModel response = new UpdatedModel();
		
		if (app == null || user == null) {
			response.setUpdated(false);
		}else {
			try {
				
				
				user2AppRepository.save(new User2App(new User2AppKey(user.getId(), app.getId()), user, app));
				appRepository.save(app);
				
				response.setUpdated(true);
			}catch(Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				response.setUpdated(false);
			}
		}
		
		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "Get All Applications")
	public ResponseEntity<List<ApplicationResponseModel>> getApplications() {
		List<App> apps = appRepository.findAll();
		List<ApplicationResponseModel> response = new ArrayList<>();
		
		for(App a : apps) {
			ApplicationResponseModel model = new ApplicationResponseModel();
			model.setAppname(a.getAppname());
			model.setId(a.getId().toString());
			if(a.getCssClasses() == null) {
				model.setCssClasses("");
			}else {
				model.setCssClasses(a.getCssClasses());
			}
			response.add(model);
		}
		
		return ResponseEntity.ok(response);
	}
	
	@Override
	@Operation(description = "Get All Applications for User")
	public ResponseEntity<List<ApplicationWithoutJwtModel>> getAppOfUser(String username) {
		List<App> apps = appRepository.findAll();
		List<ApplicationWithoutJwtModel> response = new ArrayList<>();

		if (apps == null) {
			return ResponseEntity.ok(null);
		} else {
			
			for(App a: apps) {
				Set<User2App> user2Apps = a.getUserApp();
				for (Iterator<User2App> it = user2Apps.iterator(); it.hasNext(); ) {
					User2App user2App = it.next();
					if(user2App.getUser().getUsername().equals(username)) {
						ApplicationWithoutJwtModel application = new ApplicationWithoutJwtModel();
						application.setAppname(a.getAppname());
						application.setUrl(a.getUrl());
						if(a.getCssClasses() == null) {
							application.setCssClasses("");
						}else {
							application.setCssClasses(a.getCssClasses());
						}
						response.add(application);
						break;
					}
			    }
			}
		}
		

		return ResponseEntity.ok(response);
	}

	private List<String> findAllowedUsersForApp(App app) {
		List<User2App> userApps = findUserApps(app);

		List<String> allowedUsers = new ArrayList<>();
		for (User2App ua : userApps) {
			allowedUsers.add(ua.getUser().getUsername());
		}

		return allowedUsers;
	}
	
	private User findByUsername(String username) {
		return userRepository.findAll().stream().filter(item -> item.getUsername().equals(username)).findFirst().get();
	}


	private App findAppByName(String appName) {
		try {
			return appRepository.findAll().stream().filter(item -> item.getAppname().equals(appName)).findFirst().get();
		} catch (NoSuchElementException e) {
			e.printStackTrace();
			return null;
		}	
	}

	private List<User> findUsersByUsernameList(List<String> usernames) {
		List<User> users = new ArrayList<>();

		for (String s : usernames) {
			try {
				users.add(findByUsername(s));
			} catch (NoSuchElementException e) {
				throw new BadRequestException(e.getMessage());
			}

		}
		return users;
	}

	private List<User2App> findUserApps(App app) {
		return user2AppRepository.findAll().stream().filter(item -> item.getApp().getId().equals(app.getId()))
				.collect(Collectors.toList());
	}

	private boolean checkIfUserInList(App app, String username) {
		List<String> users = findAllowedUsersForApp(app);

		return users.contains(username);
	}





}
