/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.component;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;
import io.imunity.fido.FidoManagement;
import io.imunity.fido.credential.FidoCredentialInfo;
import io.imunity.fido.service.FidoException;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import java.util.AbstractMap;
import java.util.function.Consumer;

import static java.util.Objects.isNull;

/**
 * BE part of FidoComponent. Realize communication between BE and Javascript Client.
 *
 * @author R. Ledzinski
 */
@JavaScript({"fido.js"})
public class FidoComponent extends AbstractJavaScriptComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FIDO, FidoComponent.class);

	private final FidoManagement fidoManagement;
	private final MessageSource msg;

	private final Long entityId;
	private final String userName;
	private final boolean showSuccessNotification;
	private final Consumer<FidoCredentialInfo> newCredentialListener;

	private FidoComponent(final FidoManagement fidoManagement,
						  final MessageSource msg,
						  final Long entityId,
						  final String userName,
						  final boolean showSuccessNotification,
						  final Consumer<FidoCredentialInfo> newCredentialListener)
	{
		this.fidoManagement = fidoManagement;
		this.msg = msg;
		this.entityId = entityId;
		this.userName = userName;
		this.showSuccessNotification = showSuccessNotification;
		this.newCredentialListener = newCredentialListener;

		addFinalizeRegistrationJSFunction();
		addFinalizeAuthenticationJSFunction();
		addShowErrorJSFunctions();
	}

	private void addFinalizeRegistrationJSFunction()
	{
		addFunction("finalizeRegistration", arguments ->
			{
				log.info("Invoke finalize registration for reqId={}", arguments.getString(0));
				try
				{
					FidoCredentialInfo newCred = fidoManagement.createFidoCredentials(arguments.getString(0), arguments.getString(1));
					if (newCredentialListener != null)
					{
						newCredentialListener.accept(newCred);
					}
					if (showSuccessNotification)
					{
						NotificationPopup.showSuccess(msg.getMessage("Fido.registration"), msg.getMessage("Fido.newCredential"));
					}
				} catch (FidoException e)
				{
					NotificationPopup.showError(msg.getMessage("Fido.registrationFail"), e.getLocalizedMessage());
				}
			});
	}

	private void addFinalizeAuthenticationJSFunction()
	{
		addFunction("finalizeAuthentication", arguments ->
			{
				log.info("Invoke finalize authentication for reqId={}", arguments.getString(0));
				try
				{
					fidoManagement.verifyAuthentication(arguments.getString(0), arguments.getString(1));
					if (showSuccessNotification)
					{
						NotificationPopup.showSuccess(msg.getMessage("Fido.authentication"), msg.getMessage("Fido.successfulAuth"));
					}
				} catch (FidoException e)
				{
					showError(msg.getMessage("Fido.authenticationFail"), e.getLocalizedMessage());
				}
			});
	}

	private void addShowErrorJSFunctions()
	{
		// Show error notification function
		addFunction("showError", arguments ->
			{
				{
					log.debug("Showing error {}: {}", arguments.getString(0), arguments.getString(1));
					showError(arguments.getString(0), arguments.getString(1));
				}
			});

		// Show internal error notification
		addFunction("showInternalError", arguments ->
			{
				{
					log.error("Showing internal error caused by {}: {}", arguments.getString(0), arguments.getString(1));
					showError(msg.getMessage("Fido.internalError"), msg.getMessage("FidoExc.internalErrorMsg"));
				}
			});
	}

	public void showError(final String title, final String errorMsg)
	{
		NotificationPopup.showError(title, errorMsg);
	}

	public void invokeRegistration()
	{
		if (isNull(entityId) && isNull(userName))
			throw new IllegalArgumentException("entityId has to be set before using invokeRegistration() method");

		invokeRegistration(userName);
	}

	public void invokeRegistration(final String username)
	{
		try
		{
			AbstractMap.SimpleEntry<String, String> options = fidoManagement.getRegistrationOptions(entityId, username);
			log.debug("reqId={}", options.getKey());
			callFunction("createCredentials", options.getKey(), options.getValue());
		} catch (FidoException e)
		{
			showError(msg.getMessage("Fido.registration"), e.getLocalizedMessage());
		}
	}

	public void invokeAuthentication(final String username)
	{
		try
		{
			AbstractMap.SimpleEntry<String, String> options = fidoManagement.getAuthenticationOptions(entityId, username);
			log.debug("reqId={}", options.getKey());
			callFunction("getCredentials", options.getKey(), options.getValue());
		} catch (FidoException e)
		{
			showError(msg.getMessage("Fido.authentication"), e.getLocalizedMessage());
		}
	}

	public static FidoComponentBuilder builder(final FidoManagement fidoManagement, final MessageSource msg)
	{
		return new FidoComponentBuilder(fidoManagement, msg);
	}

	public static final class FidoComponentBuilder
	{

		private final FidoManagement fidoService;
		private final MessageSource msg;
		private boolean showSuccessNotification = true;
		private Long entityId;
		private String userName;
		private Consumer<FidoCredentialInfo> newCredentialListener;

		private FidoComponentBuilder(final FidoManagement fidoManagement, final MessageSource msg)
		{
			this.fidoService = fidoManagement;
			this.msg = msg;
		}

		public FidoComponentBuilder showSuccessNotification(boolean showSuccessNotification)
		{
			this.showSuccessNotification = showSuccessNotification;
			return this;
		}

		public FidoComponentBuilder entityId(Long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public FidoComponentBuilder userName(String userName)
		{
			this.userName = userName;
			return this;
		}

		public FidoComponentBuilder newCredentialListener(Consumer<FidoCredentialInfo> newCredentialListener)
		{
			this.newCredentialListener = newCredentialListener;
			return this;
		}

		public FidoComponent build()
		{
			return new FidoComponent(fidoService,
					msg,
					entityId,
					userName,
					showSuccessNotification,
					newCredentialListener);
		}
	}
}
