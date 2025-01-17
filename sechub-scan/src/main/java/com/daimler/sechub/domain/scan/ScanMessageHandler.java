// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daimler.sechub.domain.scan.access.ScanGrantUserAccessToProjectService;
import com.daimler.sechub.domain.scan.access.ScanRevokeUserAccessAtAllService;
import com.daimler.sechub.domain.scan.access.ScanRevokeUserAccessFromProjectService;
import com.daimler.sechub.sharedkernel.messaging.AsynchronMessageHandler;
import com.daimler.sechub.sharedkernel.messaging.DomainMessage;
import com.daimler.sechub.sharedkernel.messaging.IsReceivingAsyncMessage;
import com.daimler.sechub.sharedkernel.messaging.MessageDataKeys;
import com.daimler.sechub.sharedkernel.messaging.MessageID;
import com.daimler.sechub.sharedkernel.messaging.UserMessage;

@Component
public class ScanMessageHandler implements AsynchronMessageHandler{


	private static final Logger LOG = LoggerFactory.getLogger(ScanMessageHandler.class);


	@Autowired
	ScanGrantUserAccessToProjectService grantService;

	@Autowired
	ScanRevokeUserAccessFromProjectService revokeUserFromProjectService;

	@Autowired
	ScanRevokeUserAccessAtAllService revokeUserService;

	@Override
	public void receiveAsyncMessage(DomainMessage request) {
		MessageID messageId = request.getMessageId();
		LOG.debug("received domain request: {}", request);

		switch (messageId) {
		case USER_ADDED_TO_PROJECT:
			handleUserAddedToProject(request);
			break;
		case USER_REMOVED_FROM_PROJECT:
			handleUserRemovedFromProject(request);
			break;
		case USER_DELETED:
			handleUserDeleted(request);
			break;
		default:
			throw new IllegalStateException("unhandled message id:"+messageId);
		}
	}

	@IsReceivingAsyncMessage(MessageID.USER_ADDED_TO_PROJECT)
	private void handleUserAddedToProject(DomainMessage request) {
		UserMessage data = request.get(MessageDataKeys.PROJECT_TO_USER_DATA);
		grantService.grantUserAccessToProject(data.getUserId(),data.getProjectId());
	}

	@IsReceivingAsyncMessage(MessageID.USER_REMOVED_FROM_PROJECT)
	private void handleUserRemovedFromProject(DomainMessage request) {
		UserMessage data = request.get(MessageDataKeys.PROJECT_TO_USER_DATA);
		revokeUserFromProjectService.revokeUserAccessFromProject(data.getUserId(), data.getProjectId());
	}

	@IsReceivingAsyncMessage(MessageID.USER_DELETED)
	private void handleUserDeleted(DomainMessage request) {
		UserMessage data = request.get(MessageDataKeys.USER_DELETE_DATA);
		revokeUserService.revokeUserAccess(data.getUserId());
	}

}
