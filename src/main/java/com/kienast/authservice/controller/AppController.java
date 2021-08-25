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
import com.kienast.authservice.rest.api.model.UpdateApplicationModel;
import com.kienast.authservice.rest.api.model.UpdatedModel;
import com.kienast.authservice.rest.api.model.VerifiedModel;
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
public class AppController implements AppApi, AppOfUserApi {

	@Autowired
	private AppRepository appRepository;

	@Autowired
	private User2AppRepository user2AppRepository;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private UserRepository userRepository;

	private static Logger logger = LogManager.getLogger(AppController.class.getName());

	@Value("${logging.level.com.kienast.authservice}")
	private String loglevel;

	@Override
	@Operation(description = "Add an application")
	public ResponseEntity<ApplicationModel> addApplication(String JWT, String xRequestID, String SOURCE_IP,
			@Valid ApplicationModel applicationModel) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");

		logger.info("Got Request (Add Application) for " + applicationModel.getAppname());

		ApplicationModel response = new ApplicationModel();

		logger.info("Try to validate Token for Request (Add Application) on " + applicationModel.getAppname());
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}

		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		logger.info("Try to check if App " + applicationModel.getAppname() + " not already exists");
		try {
			if (findAppByName(applicationModel.getAppname()) != null) {
				logger.error("App " + applicationModel.getAppname() + " already exists");
				throw new BadRequestException(applicationModel.getAppname());
			}
		} catch (NoSuchElementException e) {
			logger.debug("No Such Element: " + e.getMessage());
		}

		if (applicationModel.getAppname() == null || applicationModel.getUrl() == null
				|| applicationModel.getCssClasses() == null || applicationModel.getAllowedUsers() == null) {

			logger.error("At least one of the provided values was null. Appname: " + applicationModel.getAppname() + " URl: "
					+ applicationModel.getUrl() + " CSS-Classes: " + applicationModel.getCssClasses() + " AllowedUsers: "
					+ applicationModel.getAllowedUsers());

			throw new BadRequestException(applicationModel.getAppname());
		}

		logger.info("Register new Application " + applicationModel.getAppname());
		App app = new App(applicationModel.getAppname(), applicationModel.getUrl(), applicationModel.getCssClasses());
		appRepository.save(app);

		logger.info("Try to add allowedUsers to new Application if there are any");
		for (String s : applicationModel.getAllowedUsers()) {
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
		response.setAllowedUsers(allowedUsers);

		logger.info("Creation was successfull");

		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "update an application")
	public ResponseEntity<ApplicationModel> updateApplication(String JWT, String xRequestID, String SOURCE_IP,
			@Valid UpdateApplicationModel updateApplicationModel) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Update Application) for " + updateApplicationModel.getAppname());

		ApplicationModel response = new ApplicationModel();

		logger.info("Try to validate Token for Request (Update Application) on " + updateApplicationModel.getAppname());
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		logger.info("Try to check if App " + updateApplicationModel.getAppname() + " already exists");
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

		logger.info("Update was successfull");

		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "get an application")
	public ResponseEntity<ApplicationModel> getApp(String appname, String JWT, String xRequestID, String SOURCE_IP) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Get Application) for " + appname);

		logger.info("Try to validate Token for Request (Get Application)");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		App app = findAppByName(appname);

		ApplicationModel response = new ApplicationModel();

		if (app == null) {
			logger.debug("App " + appname + " was not found");
			return ResponseEntity.ok(null);
		} else {
			List<String> allowedUsers = findAllowedUsersForApp(app);

			response.setAppname(app.getAppname());
			response.setUrl(app.getUrl());
			if (app.getCssClasses() == null) {
				response.setCssClasses("");
			} else {
				response.setCssClasses(app.getCssClasses());
			}
			response.setAllowedUsers(allowedUsers);
		}

		logger.info("Retrieval was successfull");

		return ResponseEntity.ok(response);

	}

	@Override
	@Operation(description = "verify user for application")
	public ResponseEntity<VerifiedModel> verifyUserForApp(String appname, String username, String JWT, String xRequestID,
			String SOURCE_IP) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Verify User for Application) for " + appname);

		logger.info("Try to validate Token for Request (Verify User for Application)");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		App app = findAppByName(appname);
		VerifiedModel response = new VerifiedModel();

		if (app == null) {
			logger.info("App " + appname + " does not exist");
			response.setVerificationMessage("not_allowed");
		} else {
			if (checkIfUserInList(app, username)) {
				logger.info("User is allowed to use " + appname);
				response.setVerificationMessage("allowed");
			} else {
				logger.info("User is not allowed to use " + appname);
				response.setVerificationMessage("not_allowed");
			}
		}

		return ResponseEntity.ok(response);

	}

	@Override
	@Operation(description = "Add User to App")
	public ResponseEntity<UpdatedModel> addUser2App(String appname, String username, String JWT, String xRequestID,
			String SOURCE_IP) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Add User to Application) for " + appname);

		logger.info("Try to validate Token for Request (Add User to Application)");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		App app = findAppByName(appname);
		User user = findByUsername(username);
		UpdatedModel response = new UpdatedModel();

		if (app == null || user == null) {
			logger.info("User or app does not exist");
			response.setUpdated(false);
		} else {
			try {

				user2AppRepository.save(new User2App(new User2AppKey(user.getId(), app.getId()), user, app));
				appRepository.save(app);

				logger.info("Adding was successfull");
				response.setUpdated(true);
			} catch (Exception e) {
				logger.info("Error occurred: " + e.getMessage());
				response.setUpdated(false);
			}
		}

		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "Get All Applications")
	public ResponseEntity<List<ApplicationResponseModel>> getApplications(String JWT, String xRequestID,
			String SOURCE_IP) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Get all Applications)");

		logger.info("Try to validate Token for Request (Get Applications)");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		logger.info("Try to search for applications");
		List<App> apps = appRepository.findAll();
		List<ApplicationResponseModel> response = new ArrayList<>();

		for (App a : apps) {
			ApplicationResponseModel model = new ApplicationResponseModel();
			model.setAppname(a.getAppname());
			model.setId(a.getId().toString());
			if (a.getCssClasses() == null) {
				model.setCssClasses("");
			} else {
				model.setCssClasses(a.getCssClasses());
			}
			response.add(model);
		}

		logger.info("Retrieval was succesfull");
		return ResponseEntity.ok(response);
	}

	@Override
	@Operation(description = "Get All Applications for User")
	public ResponseEntity<List<ApplicationModel>> getAppOfUser(String username, String JWT, String xRequestID,
			String SOURCE_IP) {

		initializeLogInfo(xRequestID, SOURCE_IP, "");
		logger.info("Got Request (Get Apps for User)");

		logger.info("Try to validate Token for Request (Get Applications for User)");
		if (!tokenService.validateToken(JWT)) {
			throw (new NotAuthorizedException(JWT));
		}
		String userId = tokenService.getUUIDFromToken(JWT);
		if (StringUtils.isNotBlank(userId)) {
			initializeLogInfo(xRequestID, SOURCE_IP, userId);
			logger.info("Added userId to log");
		}

		List<App> apps = appRepository.findAll();
		List<ApplicationModel> response = new ArrayList<>();

		if (apps == null) {
			logger.debug("There are no apps");
			return ResponseEntity.ok(null);
		} else {

			for (App a : apps) {
				Set<User2App> user2Apps = a.getUserApp();
				for (Iterator<User2App> it = user2Apps.iterator(); it.hasNext();) {
					User2App user2App = it.next();
					if (user2App.getUser().getUsername().equals(username)) {
						ApplicationModel application = new ApplicationModel();
						application.setAppname(a.getAppname());
						application.setUrl(a.getUrl());
						if (a.getCssClasses() == null) {
							application.setCssClasses("");
						} else {
							application.setCssClasses(a.getCssClasses());
						}
						response.add(application);
						break;
					}
				}
			}
		}

		logger.info("Retrieval was successfull");
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
		} catch (Exception e) {
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

	private void initializeLogInfo(String requestId, String sourceIP, String userId) {
		MDC.put("SYSTEM_LOG_LEVEL", loglevel);
		MDC.put("REQUEST_ID", requestId);
		MDC.put("SOURCE_IP", sourceIP);
		MDC.put("USER_ID", userId);
	}

}
