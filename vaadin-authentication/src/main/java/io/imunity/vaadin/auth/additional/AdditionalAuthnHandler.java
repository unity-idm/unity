/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.additional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import io.imunity.vaadin.auth.VaadinAuthentication;

import java.util.List;
import java.util.function.Consumer;

/**
 * Performs additional authentication in effect of exception signaling such need.
 */
@Component
public class AdditionalAuthnHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AdditionalAuthnHandler.class);
	private final SessionManagement sessionMan;
	private final EntityManagement entityMan;

	public enum AuthnResult 
	{
		ERROR, CANCEL, SUCCESS
	}
	
	@Autowired
	public AdditionalAuthnHandler(SessionManagement sessionMan, EntityManagement entityMan)
	{
		this.sessionMan = sessionMan;
		this.entityMan = entityMan;
	}
	
	public void handleAdditionalAuthenticationException(AdditionalAuthenticationRequiredException exception,
			String header, String info,
			Consumer<AuthnResult> resultCallback)
	{
		String authenticator = exception.authenticationOption;
		AuthenticatorWithFlow authnPlusFlow = getRetrieval(authenticator);
		
		AuthenticatorStepContext context = new AuthenticatorStepContext(InvocationContext.getCurrent().getRealm(),
				authnPlusFlow.flow, null, FactorOrder.FIRST);
		VaadinAuthentication.VaadinAuthenticationUI authenticationUI = authnPlusFlow.authenticator.createUIInstance(VaadinAuthentication.Context.LOGIN,
				context).iterator().next();
		Entity entity = getCurrentEntity();
		authenticationUI.presetEntity(entity);

		log.info("Triggering additional authentication for {} using authenticator {}",
				entity.getId(),
				exception.authenticationOption);

		AuthNPanel authnPanel = new AuthNPanel(authenticationUI);
		AdditionalAuthnDialog dialog = new AdditionalAuthnDialog(header, info, authnPanel,
				() -> onDialogClose(resultCallback));
		AuthenticationOptionKey additionalAuthnOptionKey = new AuthenticationOptionKey(authenticator, authenticationUI.getId());
		authenticationUI.setAuthenticationCallback(
				new AdditionalAuthNResultCallback(sessionMan, additionalAuthnOptionKey,
						result -> processResult(dialog, result, resultCallback)));
		dialog.show();
	}
	
	private Entity getCurrentEntity()
	{
		long currentEntity = InvocationContext.getCurrent().getLoginSession().getEntityId();
		try
		{
			return entityMan.getEntity(new EntityParam(currentEntity));
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not access information about currently logged user");
		}
	}
	
	private void processResult(AdditionalAuthnDialog dialog, AuthnResult result, Consumer<AuthnResult> resultCallback)
	{
		dialog.diableCancelListener();
		dialog.close();
		log.info("Additional authentication completed, result: {}", result);
		resultCallback.accept(result);
	}
	
	private void onDialogClose(Consumer<AuthnResult> resultCallback)
	{
		log.debug("Additional authentication was cancelled");
		resultCallback.accept(AuthnResult.CANCEL);
	}
	
	private AuthenticatorWithFlow getRetrieval(String authenticator)
	{
		List<AuthenticationFlow> endpointFlows = InvocationContext.getCurrent().getEndpointFlows();
		for (AuthenticationFlow flow: endpointFlows)
			for (AuthenticatorInstance authn: flow.getAllAuthenticators())
				if (authenticator.equals(authn.getMetadata().getId()))
					return new AuthenticatorWithFlow((VaadinAuthentication) authn.getRetrieval(), flow);
		throw new IllegalStateException("Got request for additional authentication with " + authenticator + 
				" which is not available on the endpoint");
	}
	
	private static class AuthenticatorWithFlow
	{
		final VaadinAuthentication authenticator;
		final AuthenticationFlow flow;
		
		AuthenticatorWithFlow(VaadinAuthentication authenticator, AuthenticationFlow flow)
		{
			this.authenticator = authenticator;
			this.flow = flow;
		}
	}
}
