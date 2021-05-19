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
import com.kienast.authservice.rest.api.model.MFATokenVerificationModel;
import com.kienast.authservice.rest.api.model.QRCodeModel;
import com.kienast.authservice.rest.api.model.TokenModel;
import com.kienast.authservice.rest.api.model.VerifiedModel;
import com.kienast.authservice.service.TokenService;

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
    
   

    @Value("${companyName}")
    private String companyName;

    @Override
    @Operation(description = "setup MFA")
	public ResponseEntity<QRCodeModel> mfaSetup(@Valid TokenModel tokenModel) {
    	
    	User user = null;
    	
    	
    	if (!tokenService.validateToken(tokenModel.getToken())) {
			throw(new NotAuthorizedException(tokenModel.getToken()));
		}
    	
		
		try {
			user = findByUsername(tokenModel.getUsername());
		}catch(java.util.NoSuchElementException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw(new NotAuthorizedException(tokenModel.getUsername()));
		}
    	
		
		// Generate and store the secret
        String secret = secretGenerator.generate();
        
        user.setSecret(secret);
        userRepository.save(user);

        QrData data = qrDataFactory.newBuilder()
            .label(user.getUsername())
            .secret(secret)
            .issuer(companyName)
            .build();

        // Generate the QR code image data as a base64 string which
        // can be used in an <img>

            
        String qrCodeImage = ""; 
		try {
			qrCodeImage = getDataUriForImage(
			  qrGenerator.generate(data), 
			  qrGenerator.getImageMimeType()
			);
		} catch (QrGenerationException e) {
			e.printStackTrace();
		}
        //return qrCodeImage;
        Map<String, Object> map = new HashMap<>();
        map.put("qrCode", qrCodeImage);
        
        QRCodeModel qrCodeModel = new QRCodeModel();
        qrCodeModel.setQrcode(map.get("qrCode").toString());
        
        
		return ResponseEntity.ok(qrCodeModel);
	}

	@Override
	@Operation(description = "verify MFA")
	public ResponseEntity<VerifiedModel> mfaVerify(@Valid MFATokenVerificationModel mfATokenVerificationModel) {
		
		VerifiedModel verified = new VerifiedModel();
		User user = null;
		
		if (!tokenService.validateToken(mfATokenVerificationModel.getJwtToken())) {
			throw(new NotAuthorizedException(mfATokenVerificationModel.getJwtToken()));
		}
		
		try {
			user = findByUsername(mfATokenVerificationModel.getUsername());
		}catch(java.util.NoSuchElementException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			throw(new NotAuthorizedException(mfATokenVerificationModel.getUsername()));
		}
		
		if (verifier.isValidCode(user.getSecret(), mfATokenVerificationModel.getMfaToken())) {
			verified.setVerificationMessage("CORRECT CODE");
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.WEEK_OF_YEAR, +1);
			long nextWeek = calendar.getTime().getTime();
			
			user.setNextVerification(new Timestamp(nextWeek));
	        userRepository.save(user);
        }else {
        	verified.setVerificationMessage("INCORRECT CODE");	
        }

		return ResponseEntity.ok(verified);
		
	}
	
	private User findByUsername(String username) {
		return userRepository.findAll().stream().filter(
				item -> item.getUsername().equals(username)
				).findFirst().get();
	}


}
