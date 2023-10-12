/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Tag("div")
public class FidoBrowserCallableComponent extends Component
{
	private final Logger log;
	private final Function<String, String> msg;
	private final NotificationPresenter notificationPresenter;
	private final BiConsumer<String, String> finalizeRegistration;
	private final BiConsumer<String, String> finalizeAuthentication;

	public FidoBrowserCallableComponent(Function<String, String> msg,
	                                    Logger log,
	                                    NotificationPresenter notificationPresenter,
	                                    BiConsumer<String, String> finalizeRegistration,
	                                    BiConsumer<String, String> finalizeAuthentication)
	{
		this.msg = msg;
		this.log = log;
		this.notificationPresenter = notificationPresenter;
		this.finalizeRegistration = finalizeRegistration;
		this.finalizeAuthentication = finalizeAuthentication;
		UI.getCurrent().getPage().addJavaScript("../unitygw/fido.js");
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
		showErrorNotification(msg.apply("Fido.internalError"), msg.apply("FidoExc.internalErrorMsg"));
	}

	public void showErrorNotification(final String title, final String errorMsg)
	{
		notificationPresenter.showError(title, errorMsg);
	}
}
