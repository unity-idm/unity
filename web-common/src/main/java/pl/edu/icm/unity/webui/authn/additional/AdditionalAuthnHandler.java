/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.additional;

import java.util.List;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * Performs additional authentication in effect of exception signaling such need.
 * 
 * @author K. Benedyczak
 */
@Component
public class AdditionalAuthnHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AdditionalAuthnHandler.class);
	private final UnityMessageSource msg;
	private final ExecutorsService execService;
	private final SessionManagement sessionMan;
	private final EntityManagement entityMan;

	public enum AuthnResult 
	{
		ERROR, CANCEL, SUCCESS
	}
	
	@Autowired
	public AdditionalAuthnHandler(SessionManagement sessionMan, EntityManagement entityMan,
			ExecutorsService execService, UnityMessageSource msg)
	{
		this.sessionMan = sessionMan;
		this.entityMan = entityMan;
		this.execService = execService;
		this.msg = msg;
	}
	
	public void handleAdditionalAuthenticationException(AdditionalAuthenticationRequiredException exception,
			String header, String info,
			Consumer<AuthnResult> resultCallback)
	{
		String authenticator = exception.authenticationOption;
		VaadinAuthentication authn = getRetrieval(authenticator);
		VaadinAuthenticationUI authenticationUI = authn.createUIInstance(Context.LOGIN).iterator().next();
		Entity entity = getCurrentEntity();
		authenticationUI.presetEntity(entity);

		log.debug("Triggering additional authentication for {} using authenticator {}",
				entity.getId(),
				exception.authenticationOption);

		AuthNPanel authnPanel = new AuthNPanel(msg, execService, authenticationUI);
		AdditionalAuthnDialog dialog = new AdditionalAuthnDialog(msg, header, info, authnPanel,
				() -> onDialogClose(resultCallback));
		authenticationUI.setAuthenticationCallback(
				new AdditionalAuthNResultCallback(sessionMan, authenticator, 
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
		log.debug("Additional authentication completed, result: {}", result);
		resultCallback.accept(result);
	}
	
	private void onDialogClose(Consumer<AuthnResult> resultCallback)
	{
		log.debug("Additional authentication was cancelled");
		resultCallback.accept(AuthnResult.CANCEL);
	}
	
	private VaadinAuthentication getRetrieval(String authenticator)
	{
		List<AuthenticationFlow> endpointFlows = InvocationContext.getCurrent().getEndpointFlows();
		for (AuthenticationFlow flow: endpointFlows)
			for (AuthenticatorInstance authn: flow.getAllAuthenticators())
				if (authenticator.equals(authn.getMetadata().getId()))
					return (VaadinAuthentication) authn.getRetrieval();
		throw new IllegalStateException("Got request for additional authentication with " + authenticator + 
				" which is not available on the endpoint");
	}
}
