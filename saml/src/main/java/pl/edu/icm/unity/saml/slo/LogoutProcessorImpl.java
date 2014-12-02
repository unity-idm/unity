/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.slo.SAMLInternalLogoutContext.AsyncLogoutFinishCallback;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.LogoutProcessor;
import pl.edu.icm.unity.server.registries.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Performs a logout of additional session participants, in case of logout initiated directly in Unity.
 * 
 * @author K. Benedyczak
 */
public class LogoutProcessorImpl implements LogoutProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, LogoutProcessorImpl.class);
	
	private LogoutContextsStore contextsStore;
	private InternalLogoutProcessor internalProcessor;
	private SessionParticipantTypesRegistry registry;
	
	public LogoutProcessorImpl(LogoutContextsStore contextsStore,
			InternalLogoutProcessor internalProcessor, SessionParticipantTypesRegistry registry)
	{
		this.contextsStore = contextsStore;
		this.internalProcessor = internalProcessor;
		this.registry = registry;
	}

	@Override
	public void handleAsyncLogout(LoginSession session, String requestersRelayState, String returnUrl, 
			HttpServletResponse response) throws IOException
	{
		PlainExternalLogoutContext externalContext = new PlainExternalLogoutContext(requestersRelayState, 
				returnUrl, session);
		String relayState = contextsStore.addPlainExternalContext(externalContext);
		
		AsyncLogoutFinishCallback finishCallback = new AsyncLogoutFinishCallback()
		{
			@Override
			public void finished(HttpServletResponse response,
					SAMLInternalLogoutContext finalInternalContext)
			{
				internalLogoutFinished(response, finalInternalContext);
			}
		};
		
		SAMLInternalLogoutContext internalCtx = new SAMLInternalLogoutContext(session, null, finishCallback,
				registry);
		contextsStore.addInternalContext(relayState, internalCtx);
		
		try
		{
			internalProcessor.continueAsyncLogout(internalCtx, response);
		} catch (EopException e)
		{
			//ok
		}
	}

	@Override
	public boolean handleSynchronousLogout(LoginSession session)
	{
		SAMLInternalLogoutContext internalCtx = new SAMLInternalLogoutContext(session, null, null, registry);
		internalProcessor.logoutSynchronousParticipants(internalCtx);
		boolean allLoggedOut = internalCtx.getFailed().isEmpty();
		return allLoggedOut;
	}
	
	private void internalLogoutFinished(HttpServletResponse response,
			SAMLInternalLogoutContext finalInternalContext)
	{
		String relayState = finalInternalContext.getRelayState();
		PlainExternalLogoutContext plainCtx = contextsStore.getPlainExternalContext(
				relayState);
		if (plainCtx != null)
		{
			try
			{
				finishAsyncLogoutNoSAML(plainCtx, response, relayState);
			} catch (IOException e)
			{
				log.error("Finalization of logout failed", e);
			} catch (EopException e)
			{
				//ok
			}
		} else
			log.error("Logout of SAML session participants is finished but it seems "
					+ "there is no associated external context. This means there is a bug");
	}
	

	/**
	 * Performs a final redirect to a requested address after the SAML logout part is completed. 
	 * @throws EopException 
	 * @throws IOException 
	 */
	private void finishAsyncLogoutNoSAML(PlainExternalLogoutContext ctx, HttpServletResponse response, 
			String externalContextKey) throws IOException, EopException
	{
		contextsStore.removeExternalContext(externalContextKey);
		
		StringBuilder ret = new StringBuilder(ctx.getReturnUrl());
		if (ctx.getRequestersRelayState() != null)
			ret.append("?").append("RelayState=").append(ctx.getRequestersRelayState());
		response.sendRedirect(ret.toString());
	}
}
