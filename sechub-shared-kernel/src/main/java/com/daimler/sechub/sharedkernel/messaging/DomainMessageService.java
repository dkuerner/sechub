// SPDX-License-Identifier: MIT
package com.daimler.sechub.sharedkernel.messaging;

import static com.daimler.sechub.sharedkernel.util.Assert.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class DomainMessageService {
	/* FIXME Albert Tregnaghi, 2018-01-29:parameter validation */
	/* FIXME Albert Tregnaghi, 2018-01-29:handle web scan not wanted... */
	/* FIXME Albert Tregnaghi, 2018-01-29:null handling ... */
	/*
	 * TODO Albert Tregnaghi, 2018-02-04:check if it is not better to use eureka server for
	 * discovery...
	 */

	private static final Logger LOG = LoggerFactory.getLogger(DomainMessageService.class);

	@Autowired
	protected TaskExecutor taskExecutor;

	Map<MessageID, SynchronMessageHandler> synchronHandlers = new EnumMap<>(MessageID.class);
	Map<MessageID, List<AsynchronMessageHandler>> asynchronHandlers = new EnumMap<>(MessageID.class);
	/*
	 * TODO Albert Tregnaghi, 2018-01-29: in future this could be at least an internal REST
	 * call instead of direct usage of spring boot services... when scan is own
	 * spring boot application .. But this must be also secured...
	 */

	@Autowired // does auto registration of synchron message handlers by spring
	public DomainMessageService(List<SynchronMessageHandler> injectedSynchronousHandlers,
			List<AsynchronMessageHandler> injectedAsynchronousHandlers) {
		notNull(injectedSynchronousHandlers, "Synch.Handlers may not be null!");
		notNull(injectedAsynchronousHandlers, "Async. Handlers may not be null!");

		for (SynchronMessageHandler handler : injectedSynchronousHandlers) {
			Set<MessageID> messageIds = getSupportedMessageIdsFor(handler);
			for (MessageID messageId : messageIds) {
				synchronHandlers.put(messageId, handler);
				LOG.info("Registered synchron message handler:{} for message ID:{}", handler, messageId);
			}
		}

		for (AsynchronMessageHandler handler : injectedAsynchronousHandlers) {
			Set<MessageID> messageIds = getSupportedMessageIdsFor(handler);
			for (MessageID messageId : messageIds) {
				List<AsynchronMessageHandler> foundAsynchronousHandlersForID = this.asynchronHandlers.get(messageId);
				if (foundAsynchronousHandlersForID == null) {
					foundAsynchronousHandlersForID = new ArrayList<>();
					this.asynchronHandlers.put(messageId, foundAsynchronousHandlersForID);
				}
				foundAsynchronousHandlersForID.add(handler);
				LOG.info("Registered asynchronus message handler:{} for message ID:{}", handler, messageId);
			}
		}
	}

	private Map<AsynchronMessageHandler, Set<MessageID>> supportedMessageIdsOfAsyncMap = new HashMap<>();
	private Map<SynchronMessageHandler, Set<MessageID>> supportedMessageIdsOfSyncMap = new HashMap<>();

	private Set<MessageID> getSupportedMessageIdsFor(AsynchronMessageHandler handler) {
		return supportedMessageIdsOfAsyncMap.computeIfAbsent(handler, this::createMessageIdListByAnnotations);
	}

	private Set<MessageID> getSupportedMessageIdsFor(SynchronMessageHandler handler) {
		return supportedMessageIdsOfSyncMap.computeIfAbsent(handler, this::createMessageIdListByAnnotations);
	}

	private Set<MessageID> createMessageIdListByAnnotations(AsynchronMessageHandler handler) {
		List<IsReceivingAsyncMessage> receivings = receiveAnnotationsOfType(handler.getClass(), IsReceivingAsyncMessage.class);
		Set<MessageID> list = new LinkedHashSet<>();
		for (IsReceivingAsyncMessage r : receivings) {
			list.add(r.value());
		}
		return list;
	}

	private <T extends Annotation> List<T> receiveAnnotationsOfType(Class<?> class1, Class<T> annotationClass) {
		List<T> list = new ArrayList<>();
		T[] annotationsClass = class1.getAnnotationsByType(annotationClass);
		for (T annotation: annotationsClass) {
			list.add(annotation);
		}
		Method[] methods = class1.getDeclaredMethods();
		for (Method method: methods) {
			T[] annotations = method.getAnnotationsByType(annotationClass);
			for (T annotation: annotations) {
				list.add(annotation);
			}
		}
		return list;
	}

	private Set<MessageID> createMessageIdListByAnnotations(SynchronMessageHandler handler) {
		List<IsRecevingSyncMessage> receivings = receiveAnnotationsOfType(handler.getClass(), IsRecevingSyncMessage.class);
		Set<MessageID> list = new LinkedHashSet<>();
		for (IsRecevingSyncMessage r : receivings) {
			list.add(r.value());
		}
		List<IsRecevingSyncMessages> receivings2 = receiveAnnotationsOfType(handler.getClass(), IsRecevingSyncMessages.class);
		for (IsRecevingSyncMessages r : receivings2) {
			for (IsRecevingSyncMessage m : r.value()) {
				list.add(m.value());
			}
		}
		return list;
	}

	/**
	 * Triggers request and waits for result. Will be handled only by ONE
	 * {@link SynchronMessageHandler} instance
	 * 
	 * @param request
	 * @return result
	 * @throws UnsupportedOperationException
	 *             if no handler is able to handle the request
	 */
	public DomainMessageSynchronousResult sendSynchron(DomainMessage request) {
		assertRequestNotNull(request);

		MessageID messageID = request.getMessageId();
		SynchronMessageHandler handlersForThisMessageId = synchronHandlers.get(messageID);
		if (handlersForThisMessageId == null) {
			/*
			 * because caller wants to get a response in sync we throw a runtime exception
			 */
			throw new UnsupportedOperationException(
					"Did not found any registered synchronous handler for " + messageID);
		}
		return handlersForThisMessageId.receiveSynchronMessage(request);
	}

	/**
	 * Triggers request but does NOT waits for result. This can be handled by
	 * multiple {@link AsynchronMessageHandler} instances. When no async handler can
	 * handle the request a error log entry will be written
	 * 
	 * @param request
	 * 
	 */
	public void sendAsynchron(DomainMessage request) {
		assertRequestNotNull(request);

		MessageID messageID = request.getMessageId();
		List<AsynchronMessageHandler> handlersForThisMessageId = asynchronHandlers.get(messageID);

		if (handlersForThisMessageId == null || handlersForThisMessageId.isEmpty()) {
			/* handle problem async way by logging */
			LOG.error("Domain request with message id:{}, not handled by any asynchronous handler!", messageID);
			return;
		}

		for (AsynchronMessageHandler handler : handlersForThisMessageId) {
			taskExecutor.execute(new AsynchronMessageHandlerTaskAdapter(handler, request));
		}

	}

	private void assertRequestNotNull(DomainMessage request) {
		if (request == null) {
			throw new IllegalArgumentException("request may not be null!");
		}
	}

	private class AsynchronMessageHandlerTaskAdapter implements Runnable {
		private AsynchronMessageHandler handler;
		private DomainMessage request;

		public AsynchronMessageHandlerTaskAdapter(AsynchronMessageHandler handler, DomainMessage request) {
			this.handler = handler;
			this.request = request;
		}

		public void run() {
			try {
				handler.receiveAsyncMessage(request);
			} catch (RuntimeException e) {
				LOG.error("Was not able to run request:{} with handler {}", request, handler, e);
			}
		}
	}
}
