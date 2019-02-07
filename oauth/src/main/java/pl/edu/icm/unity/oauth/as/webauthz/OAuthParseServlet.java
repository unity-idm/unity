/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.utils.RoutingServlet;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.webui.idpcommon.EopException;


/**
 * Low level servlet performing the initial OAuth handling.
 * <p>
 * The servlet retrieves the request, parses it, validates and if everything is correct 
 * stores it in the session and forwards the processing to the Vaadin part. In case of problems an error is returned
 * to the requester or error page is displayed if the requester can not be established.
 * @author K. Benedyczak
 */
public class OAuthParseServlet extends HttpServlet 
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthParseServlet.class);
	
	/**
	 * Under this key the OAuthContext object is stored in the session.
	 */
	public static final String SESSION_OAUTH_CONTEXT = "oauth2AuthnContextKey";
	
	public static final Set<ResponseType.Value> KNOWN_RESPONSE_TYPES = Sets.newHashSet(
			ResponseType.Value.CODE, ResponseType.Value.TOKEN, OIDCResponseTypeValue.ID_TOKEN);
	
	private OAuthASProperties oauthConfig;
	private String oauthUiServletPath;
	private ErrorHandler errorHandler;
	private OAuthWebRequestValidator validator;
	
	public OAuthParseServlet(OAuthASProperties oauthConfig, 
			String oauthUiServletPath, ErrorHandler errorHandler, EntityManagement identitiesMan,
			AttributesManagement attributesMan)
	{
		this.oauthConfig = oauthConfig;
		this.oauthUiServletPath = oauthUiServletPath;
		this.errorHandler = errorHandler;
		this.validator = new OAuthWebRequestValidator(oauthConfig, identitiesMan, attributesMan);
	}

	/**
	 * GET handling
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		log.trace("Received GET request to the OAuth2 authorization endpoint");
		processRequest(request, response);
	}

	protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
			throws IOException, ServletException
	{
		try
		{
			processRequestInterruptible(request, response);
		} catch (EopException e)
		{
			//OK
		}
	}
	
	private String getQueryString(HttpServletRequest request)
	{
		String requestFromHoldOn = request.getParameter("oAuthRequest");
		if (requestFromHoldOn != null)
			return new String(Base64.decodeBase64(requestFromHoldOn), 
					StandardCharsets.UTF_8);
		else
			return request.getQueryString();
	}
	
	
	
	protected void processRequestInterruptible(HttpServletRequest request, HttpServletResponse response) 
			throws IOException, ServletException, EopException
	{
		log.trace("Starting OAuth2 authorization request processing");
		HttpSession session = request.getSession();
		OAuthAuthzContext context = (OAuthAuthzContext) session.getAttribute(SESSION_OAUTH_CONTEXT); 
		AuthorizationRequest authzRequest;
		
		String queryString = getQueryString(request);

		try
		{
			authzRequest = AuthenticationRequest.parse(queryString);
		} catch (ParseException e)
		{
			if (log.isTraceEnabled())
				log.trace("Request to OAuth2 endpoint address, which is not OIDC request, "
					+ "will try plain OAuth. OIDC parse error: " + e.toString());
			try
			{
				authzRequest = AuthorizationRequest.parse(queryString);
				Scope requestedScopes = authzRequest.getScope();
				if (requestedScopes != null && requestedScopes.contains(OIDCScopeValue.OPENID))
				{
					log.debug("Request to OAuth2 endpoint address, which is not OIDC request, "
							+ "but OIDC profile requested. OIDC parse error: " + 
							e.toString());
					errorHandler.showErrorPage("Error parsing OAuth OIDC request", e.getMessage(), 
							response);
					return;
				}
			}catch (ParseException ee)
			{
				if (log.isTraceEnabled())
					log.trace("Request to OAuth2 endpoint address, "
							+ "with invalid/missing parameters, error: " + e.toString());
				errorHandler.showErrorPage("Error parsing OAuth request", e.getMessage(), response);
				return;
			}
		}

		//ok, we do have a new request. 

		//is there processing in progress?
		if (context != null)
		{
			if (!context.isExpired() && log.isTraceEnabled())
				log.trace("Request to OAuth2 authZ address, we are " +
						"forced to break the previous login: " + 
						request.getRequestURI());
			session.removeAttribute(SESSION_OAUTH_CONTEXT);
		}
		
		if (log.isTraceEnabled())
			log.trace("Request to protected address, with OAuth2 input, will be processed: " + 
					request.getRequestURI());
		try
		{
			if (log.isTraceEnabled())
				log.trace("Parsed OAuth request: " + request.getQueryString());
			context = new OAuthAuthzContext(authzRequest, oauthConfig);
			validator.validate(context);
		} catch (OAuthValidationException e)
		{
			if (log.isDebugEnabled())
				log.debug("Processing of OAuth request failed", e);
			errorHandler.showErrorPage(e.getMessage(), null, response);
			return;
		}
		
		session.setAttribute(SESSION_OAUTH_CONTEXT, context);
		RoutingServlet.clean(request);
		if (log.isTraceEnabled())
			log.trace("Request with OAuth input handled successfully");
		response.sendRedirect(oauthUiServletPath);
	}
}




