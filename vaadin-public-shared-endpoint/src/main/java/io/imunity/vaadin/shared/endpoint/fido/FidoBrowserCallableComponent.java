/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.shared.endpoint.fido;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;

import java.util.function.BiConsumer;

@JsModule("./fido.js")
@Tag("div")
public class FidoBrowserCallableComponent extends Component
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FIDO, FidoBrowserCallableComponent.class);
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final BiConsumer<String, String> finalizeRegistration;
	private final BiConsumer<String, String> finalizeAuthentication;

	public FidoBrowserCallableComponent(MessageSource msg,
	                                    NotificationPresenter notificationPresenter,
	                                    BiConsumer<String, String> finalizeRegistration,
	                                    BiConsumer<String, String> finalizeAuthentication)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		this.finalizeRegistration = finalizeRegistration;
		this.finalizeAuthentication = finalizeAuthentication;
	}

	@ClientCallable
	void clearExcludedCredentials()
	{
	}

	@ClientCallable
	void finalizeRegistration(String reqId, String json)
	{
		finalizeRegistration.accept(reqId, json);
	}

	@ClientCallable
	void finalizeAuthentication(String reqId, String jsonBody)
	{
		finalizeAuthentication.accept(reqId, jsonBody);
	}

	@ClientCallable
	void showError(String caused, String error)
	{
		log.debug("Showing error {}: {}", caused, error);
		showErrorNotification(caused, error);
	}

	@ClientCallable
	void showInternalError(String caused, String error)
	{
		log.error("Showing internal error caused by {}: {}", caused, error);
		showErrorNotification(msg.getMessage("Fido.internalError"), msg.getMessage("FidoExc.internalErrorMsg"));
	}

	public void showErrorNotification(final String title, final String errorMsg)
	{
		notificationPresenter.showError(title, errorMsg);
	}
}
