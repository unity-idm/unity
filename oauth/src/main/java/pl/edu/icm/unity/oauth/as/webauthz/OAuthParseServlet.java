/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.CommonIdPProperties;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.RoutingServlet;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.collect.Sets.SetView;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;

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
	protected OAuthRequestValidator requestValidator;
	private boolean assumeForce;
	
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
		this.assumeForce = oauthConfig.getBooleanValue(CommonIdPProperties.ASSUME_FORCE);
		this.requestValidator = new OAuthRequestValidator(oauthConfig, identitiesMan, attributesMan);
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
			//We can have the old session expired or order to forcefully close it.
			String forceStr = request.getParameter(REQ_FORCE);
			boolean force = assumeForce || (forceStr != null && !forceStr.equals("false"));
			
			if (!force && !context.isExpired())
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
			context = new OAuthAuthzContext(authzRequest, oauthConfig);
			validate(context);
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
		try
		{
			Entity clientResolvedEntity = identitiesMan.getEntity(clientEntity);
			context.setClientEntityId(clientResolvedEntity.getId());
		} catch (IllegalIdentityValueException e)
		{
			throw new OAuthValidationException("The client '" + client + "' is unknown");
		} catch (EngineException e)
		{
			log.error("Problem retrieving identity of the OAuth client", e);
			throw new OAuthValidationException("Internal error, can not retrieve OAuth client's data");
		}

		context.setClientUsername(client);
		
		requestValidator.validateGroupMembership(clientEntity, client);
		Map<String, AttributeExt<?>> attributes = requestValidator.getAttributes(clientEntity);
		
		AttributeExt<?> allowedUrisA = attributes.get(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI);
		AttributeExt<?> nameA = attributes.get(OAuthSystemAttributesProvider.CLIENT_NAME);
		AttributeExt<?> logoA = attributes.get(OAuthSystemAttributesProvider.CLIENT_LOGO);
		AttributeExt<?> groupA = attributes.get(OAuthSystemAttributesProvider.PER_CLIENT_GROUP);

		if (allowedUrisA == null || allowedUrisA.getValues().isEmpty())
			throw new OAuthValidationException("The '" + client + 
					"' has no authorized redirect URI(s) defined");
		Set<String> allowedUris = new LinkedHashSet<>();
		for (Object val: allowedUrisA.getValues())
			allowedUris.add(val.toString());
		
		Set<GrantFlow> allowedFlows = requestValidator.getAllowedFlows(attributes);
		
		validateFlowAndMode(authzRequest, allowedFlows, client, context);
		
		URI redirectionURI = authzRequest.getRedirectionURI();
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
		
		context.setTranslationProfile(oauthConfig.getValue(CommonIdPProperties.TRANSLATION_PROFILE));
		
		Scope requestedScopes = authzRequest.getScope();
		List<ScopeInfo> validRequestedScopes = requestValidator.getValidRequestedScopes(requestedScopes);
		validRequestedScopes.forEach(si -> context.addScopeInfo(si));
	}

	/**
	 * Checks if the response type(s) are understood and in proper combination. Also openid connect mode
	 * is set and checked for consistency with the flow. It is checked if the client can use the requested flow. 
	 * @param authzRequest
	 * @param allowedFlows
	 * @param client
	 * @param context
	 * @throws OAuthValidationException
	 */
	private void validateFlowAndMode(AuthorizationRequest authzRequest, Set<GrantFlow> allowedFlows, 
			String client, OAuthAuthzContext context) throws OAuthValidationException
	{
		ResponseType responseType = authzRequest.getResponseType();
		Scope requestedScopes = authzRequest.getScope();

		context.setOpenIdMode(requestedScopes != null && requestedScopes.contains(OIDCScopeValue.OPENID));

		if (context.isOpenIdMode())
		{
			if (responseType.contains(ResponseType.Value.TOKEN) && responseType.size() == 1)
				throw new OAuthValidationException("The OpenID Connect mode implied by "
						+ "the requested 'openid' scope can not be used with the "
						+ "'token' response type - it makes no sense");
			if (!(authzRequest instanceof AuthenticationRequest))
				throw new OAuthValidationException("The OpenID Connect mode implied by "
						+ "the requested 'openid' scope was used with non OIDC compliant, "
						+ "plain OOAuth request");
		} else
		{
			if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN))
				throw new OAuthValidationException("The 'openid' scope was not requested and the "
						+ "'id_token' response type was what is an invalid combination");
		}

		
		SetView<ResponseType.Value> diff = Sets.difference(responseType, KNOWN_RESPONSE_TYPES);
		if (!diff.isEmpty())
			throw new OAuthValidationException("The following response type(s) is(are) not supported: " 
					+ diff);
		
		if (responseType.contains(ResponseType.Value.CODE))
		{
			if (responseType.size() == 1)
			{
				context.setFlow(GrantFlow.authorizationCode);
			} else
			{
				context.setFlow(GrantFlow.openidHybrid);
				if (!context.isOpenIdMode())
					throw new OAuthValidationException("The OpenID Connect mode "
							+ "implied by the requested hybrid flow "
							+ "must use the 'openid' scope");
			}
		} else
		{
			context.setFlow(GrantFlow.implicit);
		}
		
		if (!allowedFlows.contains(context.getFlow()))
			throw new OAuthValidationException("The '" + client + 
					"' is not authorized to use the '" + context.getFlow() + "' grant flow.");
	}
}




