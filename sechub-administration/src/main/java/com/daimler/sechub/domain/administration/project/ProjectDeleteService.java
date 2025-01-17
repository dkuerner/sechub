// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.administration.project;

import javax.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.daimler.sechub.sharedkernel.RoleConstants;
import com.daimler.sechub.sharedkernel.Step;
import com.daimler.sechub.sharedkernel.UserContextService;
import com.daimler.sechub.sharedkernel.messaging.DomainMessageService;
import com.daimler.sechub.sharedkernel.usecases.admin.user.UseCaseAdministratorDeletesUser;

@Service
@RolesAllowed(RoleConstants.ROLE_SUPERADMIN)
public class ProjectDeleteService {

	private static final Logger LOG = LoggerFactory.getLogger(ProjectDeleteService.class);

	@Autowired
	DomainMessageService eventBusService;

	@Autowired
	ProjectRepository projectRepository;
	
	@Autowired
	UserContextService userContext;

	@Validated
	@UseCaseAdministratorDeletesUser(@Step(number = 2, name = "Service deletes projects.", next = { 3,
			4 }, description = "The service will delete the project with dependencies and triggers asynchronous events"))
	public void deletProject(String projectId) {
		LOG.info("Administrator {} triggers delete of project {}",userContext.getUserId(),projectId);
		
		Project project = projectRepository.findOrFailProject(projectId);
		
		/* FIXME Albert Tregnaghi, 2018-08-04: domain events missing! */
		projectRepository.delete(project);

	}

}
