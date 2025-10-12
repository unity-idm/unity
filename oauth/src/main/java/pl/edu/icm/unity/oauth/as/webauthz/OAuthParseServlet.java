/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;
import com.nimbusds.langtag.LangTag;
import com.nimbusds.langtag.LangTagException;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.util.MultivaluedMapUtils;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import com.nimbusds.oauth2.sdk.util.URLUtils;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.op.ACRRequest;

import io.imunity.vaadin.auth.SigInInProgressContextService;
import io.imunity.vaadin.endpoint.common.EopException;
import io.imunity.vaadin.endpoint.common.LanguageCookie;
import io.imunity.vaadin.endpoint.common.QueryBuilder;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationPolicy;
import pl.edu.icm.unity.engine.api.authn.RequestedAuthenticationContextClassReference;
import pl.edu.icm.unity.engine.api.authn.SigInInProgressContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.Prompt;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;

/**
 * Low level servlet performing the initial OAuth handling.
 * <p>
 * The servlet retrieves the request, parses it, validates and if everything is
 * correct stores it in the session and forwards the processing to the Vaadin
 * part. In case of problems an error is returned to the requester or error page
 * is displayed if the requester can not be established.
 * 
 * @author K. Benedyczak
 */
public class OAuthParseServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthParseServlet.class);

	public static final Set<ResponseType.Value> KNOWN_RESPONSE_TYPES = Sets.newHashSet(ResponseType.Value.CODE,
			ResponseType.Value.TOKEN, OIDCResponseTypeValue.ID_TOKEN);
	private static final String UI_LOCALES_PARAM = "ui_locales";
	
	private final OAuthASProperties oauthConfig;
	private final String oauthUiServletPath;
	private final ErrorHandler errorHandler;
	private final OAuthWebRequestValidator validator;
	private final UnityServerConfiguration serverConfig;

	public OAuthParseServlet(OAuthASProperties oauthConfig, String oauthUiServletPath, ErrorHandler errorHandler,
			EntityManagement identitiesMan, AttributesManagement attributesMan, OAuthScopesService scopeService,
			UnityServerConfiguration serverConfig)
	{
		this.oauthConfig = oauthConfig;
		this.oauthUiServletPath = oauthUiServletPath.endsWith("/") ? oauthUiServletPath : oauthUiServletPath + "/";
		this.errorHandler = errorHandler;
		this.validator = new OAuthWebRequestValidator(oauthConfig, identitiesMan, attributesMan, scopeService);
		this.serverConfig = serverConfig;
	}

	OAuthParseServlet(OAuthASProperties oauthConfig, String oauthUiServletPath, ErrorHandler errorHandler,
			OAuthWebRequestValidator validator, UnityServerConfiguration serverConfig)
	{
		this.oauthConfig = oauthConfig;
		this.oauthUiServletPath = oauthUiServletPath;
		this.errorHandler = errorHandler;
		this.validator = validator;
		this.serverConfig = serverConfig;
	}

	/**
	 * GET handling
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		log.trace("Received GET request to the OAuth2 authorization endpoint");
		processRequest(request, response);
	}

	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws IOException
	{
		try
		{
			processRequestInterruptible(request, response);
		} catch (EopException e)
		{
			// OK
		}
	}

	private String getQueryString(HttpServletRequest request)
	{
		String requestFromHoldOn = request.getParameter("oAuthRequest");
		if (requestFromHoldOn != null)
		{
			return new String(Base64.decodeBase64(requestFromHoldOn), StandardCharsets.UTF_8);
		} else
		{
			return request.getQueryString();
		}
	}

	protected void processRequestInterruptible(HttpServletRequest request, HttpServletResponse response)
			throws IOException, EopException
	{
		log.trace("Starting OAuth2 authorization request processing");
		AuthorizationRequest authzRequest;
		ParsedRequestParametersWithUILocales parsedRequestParametersWithUILocales;
		try
		{
			parsedRequestParametersWithUILocales = new ParsedRequestParametersWithUILocales(getQueryString(request));
		} catch (Exception e)
		{
			if (log.isTraceEnabled())
			{
				log.trace("Request to OAuth2 endpoint address, " + "with invalid/missing parameters, error: "
						+ e);
			}
			errorHandler.showErrorPage("Error parsing OAuth request parameters", e.getMessage(), response);
			return;
		}
	
		try
		{
			authzRequest = AuthenticationRequest.parse(null, parsedRequestParametersWithUILocales.parsedRequestParameters);
		} catch (ParseException e)
		{
			if (log.isTraceEnabled())
			{
				log.trace("Request to OAuth2 endpoint address, which is not OIDC request, "
						+ "will try plain OAuth. OIDC parse error: " + e);
			}
			try
			{
				authzRequest = AuthorizationRequest.parse(null, parsedRequestParametersWithUILocales.parsedRequestParameters);
				Scope requestedScopes = authzRequest.getScope();
				if (requestedScopes != null && requestedScopes.contains(OIDCScopeValue.OPENID))
				{
					log.warn("Request to OAuth2 endpoint address, which is not OIDC request, "
							+ "but OIDC profile requested. OIDC parse error: " + e);
					errorHandler.showErrorPage("Error parsing OAuth OIDC request", e.getMessage(), response);
					return;
				}
			} catch (ParseException ee)
			{
				if (log.isTraceEnabled())
				{
					log.trace("Request to OAuth2 endpoint address, " + "with invalid/missing parameters, error: "
							+ e);
				}
				errorHandler.showErrorPage("Error parsing OAuth request", e.getMessage(), response);
				return;
			}
		}

		// ok, we do have a new request.
		OAuthAuthzContext context;
		if (log.isTraceEnabled())
		{
			log.trace("Request to protected address, with OAuth2 input, will be processed: " + request.getRequestURI());
		}

		try
		{
			if (log.isTraceEnabled())
			{
				log.trace("Parsed OAuth request: " + request.getQueryString());
			}
			context = new OAuthAuthzContext(authzRequest, oauthConfig);
			validator.validate(context);
		} catch (OAuthValidationException e)
		{
			log.warn("Processing of OAuth request failed", e);
			errorHandler.showErrorPage(e.getMessage(), null, response);
			return;
		}

		LoginInProgressService.SignInContextKey contextKey = OAuthSessionService.setContext(request.getSession(), context);
		if (log.isTraceEnabled())
		{
			log.trace("Request with OAuth input handled successfully ({})", contextKey);
		}

		AuthenticationPolicy.setPolicy(request.getSession(), mapPromptToAuthenticationPolicy(context.getPrompts()));
		setLanguageCookie(response, parsedRequestParametersWithUILocales.uiLocales);
		setRequestedAuthenticationContextClassReference(context.getRequestedAcr(), request.getSession(), contextKey);
		response.sendRedirect(oauthUiServletPath + getQueryToAppend(authzRequest, contextKey));
	}
	
	private void setRequestedAuthenticationContextClassReference(ACRRequest acrRequest, HttpSession session,
			LoginInProgressService.SignInContextKey key)
	{
		SigInInProgressContextService.setContext(session,
				new SigInInProgressContext(new RequestedAuthenticationContextClassReference(
						getMappedACRs(acrRequest != null ? acrRequest.getEssentialACRs() : null),
						getMappedACRs(acrRequest != null ? acrRequest.getVoluntaryACRs() : null))),
				key);
	}

	private List<String> getMappedACRs(List<ACR> acrs)
	{
		return acrs != null  ? acrs.stream().map(a -> a.getValue()).toList() : List.of();
	}
	
	private void setLanguageCookie(HttpServletResponse response, Optional<List<LangTag>> uiLocales)
	{
		if (uiLocales.isEmpty())
		{
			return;
		}

		List<Locale> requestedLocales = uiLocales.get().stream().map(l -> Locale.forLanguageTag(l.toString()))
				.collect(Collectors.toList());
		Optional<Locale> fullMatch = matchFullLocale(requestedLocales);
		if (fullMatch.isPresent())
		{
			response.addCookie(new LanguageCookie(fullMatch.get().toString()));
			return;
		}

		Optional<Locale> langMatch = matchPrimaryLangFromLocale(requestedLocales);
		if (langMatch.isPresent())
		{
			response.addCookie(new LanguageCookie(langMatch.get().toString()));
		}
	}

	private Optional<Locale> matchFullLocale(List<Locale> requestedLocales)
	{
		for (Locale langTag : requestedLocales)
		{
			if (serverConfig.isLocaleSupported(langTag))
			{
				return Optional.of(langTag);
			}
		}
		return Optional.empty();
	}

	private Optional<Locale> matchPrimaryLangFromLocale(List<Locale> requestedLocales)
	{
		for (Locale langTag : requestedLocales)
		{
			Locale onlyLang = Locale.forLanguageTag(langTag.getLanguage());
			if (serverConfig.isLocaleSupported(onlyLang))
			{
				return Optional.of(onlyLang);
			}
		}
		return Optional.empty();
	}

	private AuthenticationPolicy mapPromptToAuthenticationPolicy(Set<Prompt> prompts)
	{
		if (prompts.contains(Prompt.NONE))
		{
			return AuthenticationPolicy.REQUIRE_EXISTING_SESSION;
		} else if (prompts.contains(Prompt.LOGIN))
		{
			return AuthenticationPolicy.FORCE_LOGIN;
		}

		return AuthenticationPolicy.DEFAULT;
	}
	/**
	* We are passing all unknown to OAuth query parameters to downstream servlet.
	* This may help to build extended UIs, which can interpret those parameters.
	*/
	private String getQueryToAppend(AuthorizationRequest authzRequest, LoginInProgressService.SignInContextKey contextKey)
	{
		return QueryBuilder.buildQuery(authzRequest.getCustomParameters(), contextKey.getKey());
	}

	private class ParsedRequestParametersWithUILocales
	{
		final Map<String, List<String>> parsedRequestParameters;
		final Optional<List<LangTag>> uiLocales;
		
		ParsedRequestParametersWithUILocales(String queryString)
		{
			Map<String, List<String>> internalParsedRequestParameters = URLUtils.parseParameters(queryString);
			Optional<List<LangTag>> internalUiLocales = Optional.empty();
			try
			{
				internalUiLocales = getUILocales(internalParsedRequestParameters);
			} catch (LangTagException e)
			{
				log.warn(
						"Request to OAuth2 endpoint address with invalid ui_locales parameter={}, skipping this parameter in further processing",
						internalParsedRequestParameters.get(UI_LOCALES_PARAM));
				internalParsedRequestParameters.remove("ui_locales");
			}
			this.parsedRequestParameters = Map.copyOf(internalParsedRequestParameters);
			this.uiLocales = internalUiLocales;
		}
		
		
		private Optional<List<LangTag>> getUILocales(Map<String, List<String>> params) throws LangTagException
		{
			String v = MultivaluedMapUtils.getFirstValue(params, UI_LOCALES_PARAM);
			List<LangTag> uiLocales = null;
			if (StringUtils.isNotBlank(v)) {
				uiLocales = new LinkedList<>();
				StringTokenizer st = new StringTokenizer(v, " ");
				while (st.hasMoreTokens()) {
						uiLocales.add(LangTag.parse(st.nextToken()));		
				}
			}
			return Optional.ofNullable(uiLocales);
		}
	}
	
}
