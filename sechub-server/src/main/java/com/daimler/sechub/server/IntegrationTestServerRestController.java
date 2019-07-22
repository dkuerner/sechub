// SPDX-License-Identifier: MIT
package com.daimler.sechub.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.daimler.sechub.sharedkernel.APIConstants;
import com.daimler.sechub.sharedkernel.Profiles;
import com.daimler.sechub.sharedkernel.RoleConstants;
import com.daimler.sechub.sharedkernel.UserContextService;
import com.daimler.sechub.sharedkernel.error.NotFoundException;
import com.daimler.sechub.sharedkernel.storage.JobStorage;
import com.daimler.sechub.sharedkernel.storage.StorageService;
import com.daimler.sechub.sharedkernel.validation.ProjectIdValidation;
import com.daimler.sechub.sharedkernel.validation.ValidationResult;

/**
 * Contains additional rest call functionality for integration test server
 *
 * @author Albert Tregnaghi
 *
 */
@RestController
@Profile(Profiles.INTEGRATIONTEST)
public class IntegrationTestServerRestController {

	private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestServerRestController.class);

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private StorageService storageService;

	@Autowired
	private UserContextService userContextService;

	@Autowired
	private ProjectIdValidation projectIdValidation;

	@RequestMapping(path = APIConstants.API_ANONYMOUS + "integrationtest/alive", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_UTF8_VALUE,
			MediaType.APPLICATION_JSON_VALUE })
	public void isAlive() {
		LOG.info("Integration test server check for alive called...");
	}

	@RequestMapping(path = APIConstants.API_ANONYMOUS + "integrationtest/shutdown", method = RequestMethod.GET, produces = {
			MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public void shutdownServer() {
		LOG.info("Integration test server shutdown initiated by closing context...");
		context.close();
	}

	@RolesAllowed(RoleConstants.ROLE_USER)
	@RequestMapping(path = APIConstants.API_USER + "integrationtest/check/role/user", method = RequestMethod.GET, produces = {
			MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public void checkRoleUser() {
		LOG.info("Integration test server says user '{}' has allowed role '{}' - all authorities: '{}'",userContextService.getUserId(),RoleConstants.ROLE_USER,userContextService.getAuthories());
	}

	@RolesAllowed(RoleConstants.ROLE_OWNER)
	@RequestMapping(path = APIConstants.API_USER + "integrationtest/check/role/owner", method = RequestMethod.GET, produces = {
			MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public void checkRoleOwner() {
		LOG.info("Integration test server says user '{}' has allowed role '{}' - all authorities: '{}'",userContextService.getUserId(),RoleConstants.ROLE_OWNER,userContextService.getAuthories());
	}


	@RequestMapping(path = APIConstants.API_ANONYMOUS + "integrationtest/{projectId}/{jobUUID}/uploaded/{fileName}", method = RequestMethod.GET)
	public ResponseEntity<Resource> getUploadedFile(@PathVariable("projectId") String projectId, @PathVariable("jobUUID") UUID jobUUID,
			@PathVariable("fileName") String fileName) throws IOException {

		ValidationResult projectIdValidationResult = projectIdValidation.validate(projectId);
		if(! projectIdValidationResult.isValid()) {
			LOG.warn("Called with illegal projectId '{}',projectId");
			return ResponseEntity.notFound().build();
		}

		JobStorage storage = storageService.getJobStorage(projectId, jobUUID);
		if (!storage.isExisting(fileName)) {
			throw new NotFoundException("file not uploaded:" + fileName);
		}
		String absolutePath = storage.getAbsolutePath(fileName);
		File file = new File(absolutePath);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		/* @formatter:off */
		 Path path = Paths.get(file.getAbsolutePath());
	    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
	    return ResponseEntity.ok()
	            .headers(headers)
	            .contentLength(file.length())
	            .contentType(MediaType.parseMediaType("application/octet-stream"))
	            .body(resource);
		    /* @formatter:on */
	}

}