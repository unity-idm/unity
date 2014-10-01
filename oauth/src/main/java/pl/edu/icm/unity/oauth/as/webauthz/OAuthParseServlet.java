/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;

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
	
	
	/**
	 * key used by hold on form to mark that the new authn session should be started even 
	 * when an existing auth is in progress. 
	 */
	public static final String REQ_FORCE = "force";
	protected OAuthASProperties oauthConfig;
	protected String endpointAddress;
	protected String oauthUiServletPath;
	protected ErrorHandler errorHandler;
	protected IdentitiesManagement identitiesMan;
	protected AttributesManagement attributesMan;
	
	public OAuthParseServlet(OAuthASProperties oauthConfig, String endpointAddress,
			String oauthUiServletPath, ErrorHandler errorHandler, IdentitiesManagement identitiesMan,
			AttributesManagement attributesMan)
	{
		super();
		this.oauthConfig = oauthConfig;
		this.endpointAddress = endpointAddress;
		this.oauthUiServletPath = oauthUiServletPath;
		this.errorHandler = errorHandler;
		this.identitiesMan = identitiesMan;
		this.attributesMan = attributesMan;
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
	
	protected void processRequestInterruptible(HttpServletRequest request, HttpServletResponse response) 
			throws IOException, ServletException, EopException
	{
		log.trace("Starting OAuth2 authorization request processing");
		HttpSession session = request.getSession();
		OAuthAuthzContext context = (OAuthAuthzContext) session.getAttribute(SESSION_OAUTH_CONTEXT); 
		AuthorizationRequest authzRequest;
		
		try
		{
			String requestFromHoldOn = request.getParameter("oAuthRequest");
			String queryString;
			if (requestFromHoldOn != null)
			{
				queryString = new String(Base64.decodeBase64(requestFromHoldOn));
			} else
				queryString = request.getQueryString();
			authzRequest = AuthorizationRequest.parse(queryString);
		} catch (ParseException e)
		{
			if (log.isTraceEnabled())
				log.trace("Request to OAuth2 endpoint address, with invalid/missing parameters, error: " + 
						e.toString());
			errorHandler.showErrorPage("Error parsing OAuth request", e.getMessage(), response);
			return;
		}

		//ok, we do have a new request. 

		//is there processing in progress?
		if (context != null)
		{
			//We can have the old session expired or order to forcefully close it.
			String force = request.getParameter(REQ_FORCE);
			if ((force == null || force.equals("false")) && !context.isExpired())
			{
				if (log.isTraceEnabled())
					log.trace("Request to OAuth2 consumer address, with OAuth input and we have " +
							"OAuth login in progress, redirecting to hold on page: " + 
							request.getRequestURI());
				errorHandler.showHoldOnPage(request.getQueryString(), response);
				return;
			} else
			{
				if (log.isTraceEnabled())
					log.trace("Request to OAuth2 authZ address, we are " +
							"forced to break the previous login: " + 
							request.getRequestURI());
				session.removeAttribute(SESSION_OAUTH_CONTEXT);
			}
		}
		
		if (log.isTraceEnabled())
			log.trace("Request to protected address, with OAuth2 input, will be processed: " + 
					request.getRequestURI());
		try
		{
			if (log.isTraceEnabled())
				log.trace("Parsed OAuth request: " + request.getQueryString());
			context = new OAuthAuthzContext(authzRequest);
			validate(context);
		} catch (OAuthValidationException e)
		{
			if (log.isDebugEnabled())
				log.debug("Processing of OAuth request failed", e);
			errorHandler.showErrorPage(e.getMessage(), null, response);
			return;
		}
		
		session.setAttribute(SESSION_OAUTH_CONTEXT, context);
		if (log.isTraceEnabled())
			log.trace("Request with OAuth input handled successfully");
		response.sendRedirect(oauthUiServletPath);
	}
	
	
	/**
	 * Checks if the requested client is defined, if the return URL is valid for the client and 
	 * whether the authorization grant is enabled for the client.
	 * @param context
	 */
	@SuppressWarnings("unchecked")
	protected void validate(OAuthAuthzContext context) 
			throws OAuthValidationException
	{
		AuthorizationRequest authzRequest = context.getRequest();
		String client = authzRequest.getClientID().getValue();
		EntityParam clientEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, client)); 
		Collection<String> groups;
		try
		{
			groups = identitiesMan.getGroups(clientEntity);
		} catch (IllegalIdentityValueException e)
		{
			throw new OAuthValidationException("The '" + client + "' is unknown");
		} catch (EngineException e)
		{
			log.error("Problem retrieving groups of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}
		
		String oauthGroup = oauthConfig.getValue(OAuthASProperties.CLIENTS_GROUP);
		if (!groups.contains(oauthGroup))
			throw new OAuthValidationException("The '" + client + "' is not authorized as OAuth client "
					+ "(not in the clients group)");
		
		Collection<AttributeExt<?>> attrs;
		try
		{
			attrs = attributesMan.getAttributes(clientEntity, oauthGroup, null);
		} catch (EngineException e)
		{
			log.error("Problem retrieving attributes of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}
		AttributeExt<?> allowedUrisA = null;
		AttributeExt<?> allowedFlowsA = null;
		AttributeExt<?> nameA = null;
		AttributeExt<?> logoA = null;
		AttributeExt<?> groupA = null;
		for (AttributeExt<?> attr: attrs)
		{
			if (attr.getName().equals(OAuthSystemAttributesProvider.ALLOWED_FLOWS))
				allowedFlowsA = attr;
			else if (attr.getName().equals(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI))
				allowedUrisA = attr;
			else if (attr.getName().equals(OAuthSystemAttributesProvider.CLIENT_LOGO))
				logoA = attr;
			else if (attr.getName().equals(OAuthSystemAttributesProvider.CLIENT_NAME))
				nameA = attr;
			else if (attr.getName().equals(OAuthSystemAttributesProvider.PER_CLIENT_GROUP))
				groupA = attr;
		}
		if (allowedUrisA == null || allowedUrisA.getValues().isEmpty())
			throw new OAuthValidationException("The '" + client + 
					"' has no authorized redirect URI(s) defined");
		Set<String> allowedUris = new LinkedHashSet<>();
		for (Object val: allowedUrisA.getValues())
			allowedUris.add(val.toString());
		
		Set<GrantFlow> allowedFlows = new HashSet<>();
		if (allowedFlowsA == null)
		{
			allowedFlows.add(GrantFlow.authorizationCode);
		} else
		{
			for (Object val: allowedFlowsA.getValues())
				allowedFlows.add(GrantFlow.valueOf(val.toString()));
		}
		
		ResponseType responseType = authzRequest.getResponseType();
		URI redirectionURI = authzRequest.getRedirectionURI();
		
		if (responseType.impliesCodeFlow() && !allowedFlows.contains(GrantFlow.authorizationCode))
			throw new OAuthValidationException("The '" + client + 
					"' is not authorized to use the authorization code grant flow.");

		if (responseType.impliesImplicitFlow() && !allowedFlows.contains(GrantFlow.implicit))
			throw new OAuthValidationException("The '" + client + 
					"' is not authorized to use the implicit grant flow.");
			
		if (redirectionURI != null && !allowedUris.contains(redirectionURI.toString()))
			throw new OAuthValidationException("The '" + client + 
					"' requested to use a not registered response redirection URI: " + 
					redirectionURI);
		
		if (redirectionURI != null)
			context.setReturnURI(redirectionURI);
		else
		{
			String configuredUri = allowedUris.iterator().next();
			try
			{
				context.setReturnURI(new URI(configuredUri));
			} catch (URISyntaxException e)
			{
				log.error("The URI configured for the client '" + client + 
						"' is invalid: " + configuredUri, e);
				throw new OAuthValidationException("The URI configured for the client '" + client + 
						"' is invalid: " + configuredUri);
			}
		}

		if (logoA != null)
			context.setClientLogo((Attribute<BufferedImage>) logoA);
		
		if (nameA != null)
			context.setClientName((String) nameA.getValues().get(0));
		
		if (groupA != null)
			context.setUsersGroup((String) groupA.getValues().get(0));
		else
			context.setUsersGroup(oauthConfig.getValue(OAuthASProperties.USERS_GROUP));
		
		context.setTranslationProfile(oauthConfig.getValue(OAuthASProperties.TRANSLATION_PROFILE));
	}
}
