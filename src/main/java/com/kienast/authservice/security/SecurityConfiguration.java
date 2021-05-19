package com.kienast.authservice.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter  {
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable()
		.cors().and()
		.authorizeRequests()
		.antMatchers(HttpMethod.POST, "/auth").permitAll()
		.antMatchers(HttpMethod.PUT, "/auth").permitAll()
		.antMatchers(HttpMethod.PATCH, "/auth").permitAll()
		.antMatchers(HttpMethod.GET, "/auth").permitAll()
		.antMatchers(HttpMethod.POST, "/auth/{\\d+}").permitAll()
		.antMatchers(HttpMethod.PUT, "/auth/{\\d+}").permitAll()
		.antMatchers(HttpMethod.GET, "/").permitAll()
		.antMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll()
		.antMatchers(HttpMethod.GET, "/app/{\\d+}").permitAll()
		.antMatchers(HttpMethod.GET, "/appOfUser/{\\d+}").permitAll()	
		.antMatchers(HttpMethod.GET, "/app/{\\d+}/{\\\\d+}").permitAll()
		.antMatchers(HttpMethod.POST, "/app/{\\d+}/{\\\\d+}").permitAll()
		.antMatchers(HttpMethod.POST, "/app").permitAll()
		.antMatchers(HttpMethod.PUT, "/app").permitAll()
		.antMatchers(HttpMethod.GET, "/app").permitAll()
		.antMatchers(HttpMethod.POST, "/mfa/verify").permitAll()
		.antMatchers(HttpMethod.POST, "/mfa/setup").permitAll();
		//.antMatchers("/**").authenticated();
	// @formatter:on
	}

}
