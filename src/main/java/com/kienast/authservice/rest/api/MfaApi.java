/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (3.3.4).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.kienast.authservice.rest.api;

import com.kienast.authservice.rest.api.model.MFATokenVerificationModel;
import com.kienast.authservice.rest.api.model.QRCodeModel;
import com.kienast.authservice.rest.api.model.TokenModel;
import com.kienast.authservice.rest.api.model.VerifiedModel;
import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;
import java.util.Map;

@Validated
@Api(value = "mfa", description = "the mfa API")
public interface MfaApi {

    @ApiOperation(value = "setup mfa and receive the qrcode", nickname = "mfaSetup", notes = "", response = QRCodeModel.class, tags={ "mfa", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Schemas", response = QRCodeModel.class),
        @ApiResponse(code = 403, message = "Forbidden") })
    @RequestMapping(value = "/mfa/setup",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<QRCodeModel> mfaSetup(@ApiParam(value = "" ,required=true )  @Valid @RequestBody TokenModel tokenModel);


    @ApiOperation(value = "verify mfa", nickname = "mfaVerify", notes = "", response = VerifiedModel.class, tags={ "mfa", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Schemas", response = VerifiedModel.class),
        @ApiResponse(code = 403, message = "Forbidden") })
    @RequestMapping(value = "/mfa/verify",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.POST)
    ResponseEntity<VerifiedModel> mfaVerify(@ApiParam(value = "" ,required=true )  @Valid @RequestBody MFATokenVerificationModel mfATokenVerificationModel);

}
