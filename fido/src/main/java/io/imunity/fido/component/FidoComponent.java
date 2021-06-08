/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.component;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;
import io.imunity.fido.FidoExchange;
import io.imunity.fido.FidoRegistration;
import io.imunity.fido.credential.FidoCredentialInfo;
import io.imunity.fido.service.FidoException;
import io.imunity.fido.service.NoEntityException;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import java.util.AbstractMap;
import java.util.function.Consumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * BE part of FidoComponent. Realize communication between BE and Javascript Client.
 *
 * @author R. Ledzinski
 */
@JavaScript({"fido.js"})
public class FidoComponent extends AbstractJavaScriptComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FIDO, FidoComponent.class);

	private final FidoRegistration fidoRegistration;
	private final FidoExchange fidoExchange;
	private final MessageSource msg;

	private final Long entityId;
	private final String credentialConfiguration;
	private final String credentialName;
	private final boolean showSuccessNotification;
	private final Consumer<FidoCredentialInfo> newCredentialListener;
	private Consumer<AuthenticationResult> authenticationResultListener;

	private FidoComponent(final FidoRegistration fidoRegistration,
						  final FidoExchange fidoExchange,
						  final MessageSource msg,
						  final Long entityId,
						  final String credentialConfiguration,
						  final String credentialName,
						  final boolean showSuccessNotification,
						  final Consumer<FidoCredentialInfo> newCredentialListener,
						  final Consumer<AuthenticationResult> authenticationResultListener,
						  final boolean allowAuthenticatorReUsage)
	{
		this.fidoRegistration = fidoRegistration;
		this.fidoExchange = fidoExchange;
		this.msg = msg;
		this.entityId = entityId;
		this.credentialConfiguration = credentialConfiguration;
		this.credentialName = credentialName;
		this.showSuccessNotification = showSuccessNotification;
		this.newCredentialListener = newCredentialListener;
		this.authenticationResultListener = authenticationResultListener;

		addFinalizeRegistrationJSFunction();
		addFinalizeAuthenticationJSFunction();
		addShowErrorJSFunctions();
		if (allowAuthenticatorReUsage)
			addFunction("clearExcludedCredentials", arguments -> {});
	}

	public Long getEntityId()
	{
		return entityId;
	}

	private void addFinalizeRegistrationJSFunction()
	{
		addFunction("finalizeRegistration", arguments ->
			{
				log.info("Invoke finalize registration for reqId={}", arguments.getString(0));
				try
				{
					FidoCredentialInfo newCred = fidoRegistration.createFidoCredentials(
							credentialName, credentialConfiguration,
							arguments.getString(0), arguments.getString(1));
					if (newCredentialListener != null)
					{
						newCredentialListener.accept(newCred);
					}
					if (showSuccessNotification)
					{
						NotificationPopup.showSuccess(msg.getMessage("Fido.registration"), 
								msg.getMessage("Fido.newCredential"));
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
					AuthenticationResult result = fidoExchange.verifyAuthentication(arguments.getString(0), 
							arguments.getString(1));

					if (nonNull(authenticationResultListener))
						authenticationResultListener.accept(result);

					if (showSuccessNotification)
					{
						NotificationPopup.showSuccess(msg.getMessage("Fido.authentication"), 
								msg.getMessage("Fido.successfulAuth"));
					}
				} catch (FidoException e)
				{
					if (nonNull(authenticationResultListener))
						authenticationResultListener.accept(LocalAuthenticationResult.failed());
					else
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
					log.error("Showing internal error caused by {}: {}", 
							arguments.getString(0), arguments.getString(1));
					showError(msg.getMessage("Fido.internalError"), msg.getMessage("FidoExc.internalErrorMsg"));
				}
			});
	}

	public void showError(final String title, final String errorMsg)
	{
		NotificationPopup.showError(title, errorMsg);
	}

	public void invokeRegistration(final String username, final boolean useResidentKey)
	{
		try
		{
			AbstractMap.SimpleEntry<String, String> options = fidoRegistration.getRegistrationOptions(
					credentialName, credentialConfiguration, entityId, username, useResidentKey);
			log.debug("reqId={}", options.getKey());
			callFunction("createCredentials", options.getKey(), options.getValue());
		} catch (FidoException e)
		{
			showError(msg.getMessage("Fido.registration"), e.getLocalizedMessage());
		} catch (Exception e)
		{
			log.error("Showing internal error caused by ", e);
			showError(msg.getMessage("Fido.internalError"), msg.getMessage("FidoExc.internalErrorMsg"));
		}
	}

	public void invokeAuthentication(final Long entityId, final String username)
	{
		try
		{
			AbstractMap.SimpleEntry<String, String> options = fidoExchange.getAuthenticationOptions(
					nonNull(entityId) ? entityId : this.entityId, username);
			log.debug("reqId={}", options.getKey());
			callFunction("getCredentials", options.getKey(), options.getValue());
		} catch (NoEntityException e)
		{
			if (nonNull(authenticationResultListener))
				authenticationResultListener.accept(LocalAuthenticationResult.notApplicable());
			else
				showError(msg.getMessage("Fido.authentication"), e.getLocalizedMessage());
		} catch (FidoException e)
		{
			if (nonNull(authenticationResultListener))
				authenticationResultListener.accept(LocalAuthenticationResult.failed());
			else
				showError(msg.getMessage("Fido.authentication"), e.getLocalizedMessage());
		} catch (Exception e)
		{
			log.error("Showing internal error caused by ", e);
			showError(msg.getMessage("Fido.internalError"), msg.getMessage("FidoExc.internalErrorMsg"));
		}
	}

	public static FidoComponentBuilder builder(final MessageSource msg)
	{
		return new FidoComponentBuilder(msg);
	}

	public static final class FidoComponentBuilder
	{
		private final MessageSource msg;
		private FidoRegistration fidoRegistration;
		private FidoExchange fidoExchange;
		private boolean showSuccessNotification = true;
		private Long entityId;
		private String credentialConfiguration;
		private String credentialName;
		private Consumer<FidoCredentialInfo> newCredentialListener;
		private Consumer<AuthenticationResult> authenticationResultListener;
		private boolean allowAuthenticatorReUsage = false;

		private FidoComponentBuilder(final MessageSource msg)
		{
			this.msg = msg;
		}

		public FidoComponentBuilder fidoRegistration(FidoRegistration fidoRegistration)
		{
			this.fidoRegistration = fidoRegistration;
			return this;
		}

		public FidoComponentBuilder fidoExchange(FidoExchange fidoExchange)
		{
			this.fidoExchange = fidoExchange;
			return this;
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

		public FidoComponentBuilder credentialConfiguration(String credentialConfiguration)
		{
			this.credentialConfiguration = credentialConfiguration;
			return this;
		}

		public FidoComponentBuilder credentialName(String credentialName)
		{
			this.credentialName = credentialName;
			return this;
		}

		public FidoComponentBuilder newCredentialListener(Consumer<FidoCredentialInfo> newCredentialListener)
		{
			this.newCredentialListener = newCredentialListener;
			return this;
		}

		public FidoComponentBuilder allowAuthenticatorReUsage(boolean allowAuthenticatorReUsage)
		{
			this.allowAuthenticatorReUsage = allowAuthenticatorReUsage;
			return this;
		}

		public FidoComponentBuilder authenticationResultListener(Consumer<AuthenticationResult> authenticationResultListener)
		{
			this.authenticationResultListener = authenticationResultListener;
			return this;
		}

		public FidoComponent build()
		{
			if (isNull(fidoExchange) && isNull(fidoRegistration))
				throw new IllegalArgumentException("Cannot create FidoComponent. "
						+ "At least one FidoRegistration or FidoExchange has to be provided");

			return new FidoComponent(fidoRegistration,
					fidoExchange,
					msg,
					entityId,
					credentialConfiguration,
					credentialName,
					showSuccessNotification,
					newCredentialListener,
					authenticationResultListener,
					allowAuthenticatorReUsage);
		}
	}
}
