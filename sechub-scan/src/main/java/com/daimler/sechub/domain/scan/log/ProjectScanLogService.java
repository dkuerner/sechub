// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import com.daimler.sechub.sharedkernel.RoleConstants;
import com.daimler.sechub.sharedkernel.execution.SecHubExecutionContext;

@Service
public class ProjectScanLogService {

	@Autowired
	ProjectScanLogRepository repository;

	private static final Logger LOG = LoggerFactory.getLogger(ProjectScanLogService.class);

	public UUID logScanStarted(SecHubExecutionContext context) {
		String projectId = context.getConfiguration().getProjectId();
		UUID secHubJobUUID = context.getSechubJobUUID();
		String config = context.getConfiguration().toJSON();
		String executedBy = context.getExecutedBy();

		ProjectScanLog log = new ProjectScanLog(projectId, secHubJobUUID, executedBy, config);
		log.setStatus(ProjectScanLog.STATUS_STARTED);
		ProjectScanLog persistedLog = repository.save(log);
		return persistedLog.getUUID();


	}

	public void logScanEnded(UUID logScanUUID) {
		logEndedWithStatus(logScanUUID, ProjectScanLog.STATUS_OK);
	}

	public void logScanFailed(UUID logUUID) {
		logEndedWithStatus(logUUID, ProjectScanLog.STATUS_FAILED);
	}

	private void logEndedWithStatus(UUID logScanUUID, String status) {
		Optional<ProjectScanLog> optLog = repository.findById(logScanUUID);
		if (!optLog.isPresent()) {
			LOG.error("Cannot update log entry {} because not existing!", logScanUUID);
			return;
		}
		ProjectScanLog log = optLog.get();
		log.setEnded(LocalDateTime.now());
		log.setStatus(status);
		repository.save(log);
	}

//	public List<UUID> findAllJobUUIDsForProject(String projectId) {
//		return repository.findJobUUIDSForProject(projectId);
//	}

	@RolesAllowed(RoleConstants.ROLE_SUPERADMIN)
	public List<ProjectScanLogSummary> fetchSummaryLogsFor(String projectId) {
		return repository.findSummaryLogsFor(projectId);
	}

	@RolesAllowed(RoleConstants.ROLE_SUPERADMIN)
	public List<ProjectScanLog> fetchLogsForJob(UUID sechubJobUUID) {
		ProjectScanLog log = new ProjectScanLog();
		log.sechubJobUUID=sechubJobUUID;

		return repository.findAll(Example.of(log));
	}



}
