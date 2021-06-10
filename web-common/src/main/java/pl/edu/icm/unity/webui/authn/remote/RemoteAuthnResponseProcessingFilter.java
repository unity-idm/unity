/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.remote;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.SharedRemoteAuthenticationContextStore;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

/**
 * Common filter to be installed on endpoints which can receive response of remote authentication.
 * It should be installed on the target endpoint, before authentication filter, as it may set up session and cookies 
 * should be set on the proper path.
 */
@PrototypeComponent
public class RemoteAuthnResponseProcessingFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, RemoteAuthnResponseProcessingFilter.class);
	public static final String CONTEXT_ID_HTTP_PARAMETER = "__remote_authn_context_id";
	public static final String RESULT_SESSION_ATTRIBUTE = "__remote_authn_result";
	public static final String AUTHN_CONTEXT_SESSION_ATTRIBUTE = "__remote_authn_step_context";
	private final SharedRemoteAuthenticationContextStore remoteAuthnContextStore;
	private final RemoteAuthnResponseProcessor remoteAuthnResponseProcessor;
	
	public RemoteAuthnResponseProcessingFilter(SharedRemoteAuthenticationContextStore remoteAuthnContextStore,
			RemoteAuthnResponseProcessor remoteAuthnResponseProcessor)
	{
		this.remoteAuthnContextStore = remoteAuthnContextStore;
		this.remoteAuthnResponseProcessor = remoteAuthnResponseProcessor;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		String requestId = request.getParameter(CONTEXT_ID_HTTP_PARAMETER);
		if (requestId == null)
		{
			chain.doFilter(request, response);
			return;
		}
		log.debug("Processing remote authentication with context id {}", requestId);

		RemoteAuthnState authnContext;
		try
		{
			authnContext = remoteAuthnContextStore.getAuthnContext(requestId);
			log.debug("Got remote context associated with id {}", requestId);
			remoteAuthnContextStore.removeAuthnContext(requestId);
		} catch (WrongArgumentException e)
		{
			log.debug("Request with invalid remote authn context {}, ignoring it", requestId);
			chain.doFilter(request, response);
			return;
		}
		
		AuthenticationResult result = remoteAuthnResponseProcessor.processResponse(authnContext);
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		httpRequest.getSession().setAttribute(RESULT_SESSION_ATTRIBUTE, result);
		httpRequest.getSession().setAttribute(AUTHN_CONTEXT_SESSION_ATTRIBUTE, authnContext.getAuthenticationStepContext());
		log.debug("Authentication result was set in session");
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		//TODO KB refactor: use original return URI from authnContext. It needs to be put there, creating sth like RedirectRemoteAuthnContext
		httpResponse.sendRedirect(httpRequest.getRequestURI());
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void destroy()
	{
	}
}
