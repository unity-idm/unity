/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.translation.ExecutionFailException;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;
import pl.edu.icm.unity.rest.jwt.JWTUtils;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * RESTful implementation of the access token resource.
 * <p>
 * Access to this resource should be limited only to authenticated OAuth clients
 * 
 * @author K. Benedyczak
 * @author P. Piernik
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.TOKEN_PATH)
public class AccessTokenResource extends BaseOAuthResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH,
			AccessTokenResource.class);

	public static final String EXCHANGE_GRANT = "urn:ietf:params:oauth:grant-type:token-exchange";
	public static final String ACCESS_TOKEN_TYPE_ID = "urn:ietf:params:oauth:token-type:access_token";
	public static final String ID_TOKEN_TYPE_ID = "urn:ietf:params:oauth:token-type:id_token";
	public static final String EXCHANGE_SCOPE = "token-exchange";
	

	private TokensManagement tokensManagement;
	private OAuthASProperties config;
	private TransactionalRunner tx;
	private ClientCredentialsProcessor clientGrantProcessor;
	private OAuthIdPEngine oauthIdpEngine;
	private OAuthRequestValidator requestValidator;
	private EntityManagement idMan;

	public AccessTokenResource(TokensManagement tokensManagement, OAuthASProperties config,
			OAuthRequestValidator requestValidator, IdPEngine idpEngine,
			EntityManagement idMan, TransactionalRunner tx)
	{
		this.tokensManagement = tokensManagement;
		this.config = config;
		this.tx = tx;
		this.clientGrantProcessor = new ClientCredentialsProcessor(requestValidator,
				idpEngine, config);
		this.oauthIdpEngine = new OAuthIdPEngine(idpEngine);
		this.requestValidator = requestValidator;
		this.idMan = idMan;
	}

	@Path("/")
	@POST
	public Response getToken(@FormParam("grant_type") String grantType,
			@FormParam("code") String code, @FormParam("scope") String scope,
			@FormParam("redirect_uri") String redirectUri,
			@FormParam("refresh_token") String refreshToken,
			@FormParam("audience") String audience,
			@FormParam("requested_token_type") String requestedTokenType,
			@FormParam("subject_token") String subjectToken,
			@FormParam("subject_token_type") String subjectTokenType)
			throws EngineException, JsonProcessingException
	{
		if (grantType == null)
			return makeError(OAuth2Error.INVALID_REQUEST, "grant_type is required");

		if (grantType.equals(GrantType.REFRESH_TOKEN.getValue()))
		{
			if (refreshToken == null)
				return makeError(OAuth2Error.INVALID_REQUEST,
						"refresh_token is required");
			return handleRefreshToken(refreshToken, scope);
		}

		if (grantType.equals(EXCHANGE_GRANT))
		{
			if (audience == null)
				return makeError(OAuth2Error.INVALID_REQUEST,
						"audience is required");
			if (subjectToken == null)
				return makeError(OAuth2Error.INVALID_REQUEST,
						"subject_token is required");
			if (subjectTokenType == null)
				return makeError(OAuth2Error.INVALID_REQUEST,
						"subject_token_type is required");
			return handleExchangeToken(subjectToken, subjectTokenType,
					requestedTokenType, audience, scope);
		}

		if (grantType.equals(GrantType.AUTHORIZATION_CODE.getValue()))
		{
			if (code == null)
				return makeError(OAuth2Error.INVALID_REQUEST, "code is required");
			return handleAuthzCodeFlow(code, redirectUri);
		} else if (grantType.equals(GrantType.CLIENT_CREDENTIALS.getValue()))
		{
			return handleClientCredentialFlow(scope);
		} else
		{
			return makeError(OAuth2Error.INVALID_GRANT,
					"wrong or not supported grant_type value");
		}
	}

	private Response handleExchangeToken(String subjectToken, String subjectTokenType,
			String requestedTokenType, String audience, String scope)
			throws EngineException, JsonProcessingException
	{

		if (!subjectTokenType.equals(ACCESS_TOKEN_TYPE_ID))
		{
			return makeError(OAuth2Error.INVALID_REQUEST,
					"unsupported subject_token_type");
		}

		// if (requestedTokenType != null &&
		// !requestedTokenType.equals(subjectTokenType))
		// {
		// return makeError(OAuth2Error.INVALID_REQUEST,
		// "subject_token_type and requested_token_type must be equal");
		// }

		EntityParam audienceEntity = new EntityParam(
				new IdentityTaV(UsernameIdentity.ID, audience));
		Entity audienceResolvedEntity = null;
		try
		{
			audienceResolvedEntity = idMan.getEntity(audienceEntity);

		} catch (IllegalIdentityValueException e)
		{
			return makeError(OAuth2Error.INVALID_REQUEST, "wrong audience");
		} catch (EngineException e)
		{
			return makeError(OAuth2Error.SERVER_ERROR,
					"Internal error, can not retrieve OAuth client's data");
		}

		long callerEntityId = InvocationContext.getCurrent().getLoginSession()
				.getEntityId();
		if (!audienceResolvedEntity.getId().equals(callerEntityId))
			return makeError(OAuth2Error.INVALID_REQUEST, "wrong audience");

		Token subToken = null;
		OAuthToken parsedSubjectToken = null;

		try
		{
			subToken = tokensManagement.getTokenById(
					OAuthProcessor.INTERNAL_ACCESS_TOKEN, subjectToken);
			parsedSubjectToken = parseInternalToken(subToken);
		} catch (Exception e)
		{
			return makeError(OAuth2Error.INVALID_REQUEST, "wrong subject token");
		}

		List<String> oldRequestedScopesList = Arrays
				.asList(parsedSubjectToken.getRequestedScope());

		if (!oldRequestedScopesList.contains(EXCHANGE_SCOPE))
		{
			return makeError(OAuth2Error.INVALID_SCOPE,
					"orginal token must have  " + EXCHANGE_SCOPE + " scope");
		}
		OAuthToken newToken = null;
		try
		{
			newToken = prepareNewToken(parsedSubjectToken, scope,
					oldRequestedScopesList, subToken.getOwner(), callerEntityId,
					audience, requestedTokenType != null && requestedTokenType
							.equals(ID_TOKEN_TYPE_ID),
					EXCHANGE_GRANT);
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		newToken.setClientId(callerEntityId);
		newToken.setAudience(audience);
		newToken.setClientUsername(audience);

		try
		{
			requestValidator.validateGroupMembership(audienceEntity, audience);
			Map<String, AttributeExt> attributes = requestValidator
					.getAttributes(audienceEntity);
			AttributeExt nameA = attributes
					.get(OAuthSystemAttributesProvider.CLIENT_NAME);
			if (nameA != null)
				newToken.setClientName((String) nameA.getValues().get(0));
			else
				newToken.setClientName(null);
		} catch (OAuthValidationException e)
		{
			return makeError(OAuth2Error.INVALID_CLIENT, e.getMessage());
		} catch (Exception e) {
			return makeError(OAuth2Error.SERVER_ERROR, e.getMessage());
		}

		Date now = new Date();
		RefreshToken refreshToken = getRefreshToken(now);
		if (refreshToken != null)
		{
			//save refresh token but internalToken.refreshToken is not set
			Date refreshExpiration = getRefreshTokenExpiration(now);
			tokensManagement.addToken(OAuthProcessor.INTERNAL_REFRESH_TOKEN,
					refreshToken.getValue(),
					new EntityParam(subToken.getOwner()),
					newToken.getSerialized(), now, refreshExpiration);
		}
	  	//set refesh token in access token
		newToken.setRefreshToken(refreshToken.getValue()); 
		AccessToken accessToken = new BearerAccessToken();
		newToken.setAccessToken(accessToken.getValue());
		Date accessExpiration = getAccessTokenExpiration(now);

		Map<String, Object> additionalParams = new HashMap<>();
		additionalParams.put("issued_token_type", ACCESS_TOKEN_TYPE_ID);

		AccessTokenResponse oauthResponse = getAccessTokenResponse(newToken, accessToken,
				refreshToken, additionalParams);

		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN,
				accessToken.getValue(), new EntityParam(subToken.getOwner()),
				newToken.getSerialized(), now, accessExpiration);

		return toResponse(Response.ok(getResponseContent(oauthResponse)));
	}

	private Response handleRefreshToken(String refToken, String scope)
			throws EngineException, JsonProcessingException
	{

		Token refreshToken = null;
		OAuthToken parsedRefreshToken = null;
		try
		{
			refreshToken = tokensManagement.getTokenById(
					OAuthProcessor.INTERNAL_REFRESH_TOKEN, refToken);
			parsedRefreshToken = parseInternalToken(refreshToken);
		} catch (Exception e)
		{
			return makeError(OAuth2Error.INVALID_GRANT, "wrong refresh code");
		}

		long callerEntityId = InvocationContext.getCurrent().getLoginSession()
				.getEntityId();
		if (parsedRefreshToken.getClientId() != callerEntityId)
		{
			log.warn("Client with id " + callerEntityId
					+ " presented use refresh code issued " + "for client "
					+ parsedRefreshToken.getClientId());
			return makeError(OAuth2Error.INVALID_GRANT, "wrong refresh code");
		}

		List<String> oldRequestedScopesList = Arrays
				.asList(parsedRefreshToken.getRequestedScope());
		OAuthToken newToken = null;
		try
		{
			newToken = prepareNewToken(parsedRefreshToken, scope,
					oldRequestedScopesList, refreshToken.getOwner(),
					callerEntityId, parsedRefreshToken.getClientUsername(),
					true, GrantType.REFRESH_TOKEN.getValue());
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		Date now = new Date();
		Date accessExpiration = getAccessTokenExpiration(now);

		AccessToken accessToken = new BearerAccessToken();
		newToken.setAccessToken(accessToken.getValue());

		AccessTokenResponse oauthResponse = getAccessTokenResponse(newToken, accessToken,
				null, null);

		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN,
				accessToken.getValue(), new EntityParam(refreshToken.getOwner()),
				newToken.getSerialized(), now, accessExpiration);

		return toResponse(Response.ok(getResponseContent(oauthResponse)));

	}

	private OAuthToken prepareNewToken(OAuthToken token, String scope,
			List<String> oldRequestedScopesList, long ownerId, long clientId,
			String clientUserName, boolean idToken, String grant)
			throws OAuthErrorException
	{
		OAuthToken newToken = new OAuthToken(token);

		List<String> newRequestedScopeList = new ArrayList<>();
		if (scope != null && !scope.isEmpty())
		{
			newRequestedScopeList.addAll(Arrays.asList(scope.split(" ")));
		}

		if (!oldRequestedScopesList.containsAll(newRequestedScopeList))
		{
			throw new OAuthErrorException(
					makeError(OAuth2Error.INVALID_SCOPE, "wrong scope"));
		}
		newToken.setRequestedScope(newRequestedScopeList.stream().toArray(String[]::new));

		// get new attributes for identity
		TranslationResult userInfoRes = getAttributes(clientId, ownerId, grant);

		List<ScopeInfo> newValidRequestedScopes = requestValidator.getValidRequestedScopes(
				Scope.parse(String.join(" ", newRequestedScopeList)));
		newToken.setEffectiveScope(newValidRequestedScopes.stream().map(s -> s.getName())
				.toArray(String[]::new));

		UserInfo userInfoClaimSet = createUserInfo(newValidRequestedScopes,
				newToken.getSubject(), userInfoRes);
		newToken.setUserInfo(userInfoClaimSet.toJSONObject().toJSONString());

		Date now = new Date();
		// if openid mode build new id_token using new userinfo
		if (newRequestedScopeList.contains(OIDCScopeValue.OPENID.getValue()) && idToken)
		{
			try
			{
				newToken.setOpenidToken(createIdToken(now, newToken,
						Arrays.asList(new Audience(clientUserName)),
						userInfoClaimSet));
			} catch (Exception e)
			{
				throw new OAuthErrorException(makeError(OAuth2Error.SERVER_ERROR,
						e.getMessage()));
			}
		} else
		{
			// clear openidToken
			newToken.setOpenidToken(null);
		}

		newToken.setMaxExtendedValidity(config.getMaxExtendedAccessTokenValidity());
		newToken.setTokenValidity(config.getAccessTokenValidity());
		newToken.setAccessToken(null);
		newToken.setRefreshToken(null);
		newToken.setIssuerUri(config.getIssuerName());
		newToken.setRefreshToken(null);

		return newToken;
	}

	private TranslationResult getAttributes(long clientId, long ownerId, String grant)
			throws OAuthErrorException
	{
		TranslationResult userInfoRes = null;
		try
		{
			userInfoRes = oauthIdpEngine.getUserInfoUnsafe(ownerId,
					String.valueOf(clientId),
					config.getValue(OAuthASProperties.USERS_GROUP),
					config.getValue(CommonIdPProperties.TRANSLATION_PROFILE),
					grant, false);
		} catch (ExecutionFailException e)
		{
			log.debug("Authentication failed due to profile's decision, returning error");
			throw new OAuthErrorException(
					makeError(OAuth2Error.ACCESS_DENIED, e.getMessage()));
		} catch (IllegalGroupValueException e)
		{
			log.debug("Entity trying to access OAuth resource is not a member of required group");
			throw new OAuthErrorException(
					makeError(OAuth2Error.ACCESS_DENIED, e.getMessage()));
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			throw new OAuthErrorException(
					makeError(OAuth2Error.SERVER_ERROR, e.getMessage()));
		}
		return userInfoRes;
	}

	private UserInfo createUserInfo(List<ScopeInfo> validScopes, String userIdentity,
			TranslationResult userInfoRes)
	{
		Set<String> requestedAttributes = new HashSet<>();
		for (ScopeInfo si : validScopes)
			requestedAttributes.addAll(si.getAttributes());

		OAuthProcessor processor = new OAuthProcessor();
		Collection<DynamicAttribute> attributes = processor.filterAttributes(userInfoRes,
				requestedAttributes);

		return processor.prepareUserInfoClaimSet(userIdentity, attributes);
	}

	private String createIdToken(Date now, OAuthToken token, List<Audience> audience,
			UserInfo userInfoClaimSet)
			throws ParseException, JOSEException, EngineException
	{
		JWT signedJWT = decodeIDToken(token);
		IDTokenClaimsSet oldClaims;
		try
		{
			oldClaims = new IDTokenClaimsSet(signedJWT.getJWTClaimsSet());
		} catch (Exception e)
		{
			throw new InternalException("Can not parse the internal id token", e);
		}
		IDTokenClaimsSet newClaims = new IDTokenClaimsSet(
				new Issuer(config.getIssuerName()), new Subject(token.getSubject()),
				audience, getAccessTokenExpiration(now), now);
		newClaims.setNonce(oldClaims.getNonce());

		ResponseType responseType = null;
		if (token.getResponseType() != null && !token.getResponseType().isEmpty())
		{
			responseType = ResponseType.parse(token.getResponseType());
			if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN)
					&& responseType.size() == 1)
				newClaims.putAll(userInfoClaimSet);
		}

		return JWTUtils.generate(config.getCredential(), newClaims.toJWTClaimsSet());
	}

	private RefreshToken getRefreshToken(Date now)
	{
		RefreshToken refreshToken = null;
		if (config.getIntValue(OAuthASProperties.REFRESH_TOKEN_VALIDITY) > 0)
		{
			refreshToken = new RefreshToken();
		}
		return refreshToken;
	}

	private Response handleClientCredentialFlow(String scope)
			throws EngineException, JsonProcessingException
	{
		Date now = new Date();
		AccessToken accessToken = new BearerAccessToken();
		OAuthToken internalToken;
		try
		{
			internalToken = clientGrantProcessor
					.processClientFlowRequest(accessToken.getValue(), scope);
		} catch (OAuthValidationException e)
		{
			return makeError(OAuth2Error.INVALID_REQUEST, e.getMessage());
		}

		Date expiration = getAccessTokenExpiration(now);

		AccessTokenResponse oauthResponse = new AccessTokenResponse(
				new Tokens(accessToken, null));
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN,
				accessToken.getValue(),
				new EntityParam(internalToken.getClientId()),
				internalToken.getSerialized(), now, expiration);

		return toResponse(Response.ok(getResponseContent(oauthResponse)));
	}

	private Response handleAuthzCodeFlow(String code, String redirectUri)
			throws EngineException, JsonProcessingException
	{
		TokensPair tokensPair;
		try
		{
			tokensPair = loadAndRemoveAuthzCodeToken(code);
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		Token codeToken = tokensPair.codeToken;
		OAuthToken parsedAuthzCodeToken = tokensPair.parsedAuthzCodeToken;

		if (parsedAuthzCodeToken.getRedirectUri() != null)
		{
			if (redirectUri == null)
				return makeError(OAuth2Error.INVALID_GRANT,
						"redirect_uri is required");
			if (!redirectUri.equals(parsedAuthzCodeToken.getRedirectUri()))
				return makeError(OAuth2Error.INVALID_GRANT,
						"redirect_uri is wrong");
		}

		OAuthToken internalToken = new OAuthToken(parsedAuthzCodeToken);
		AccessToken accessToken = new BearerAccessToken();
		internalToken.setAccessToken(accessToken.getValue());

		Date now = new Date();
		RefreshToken refreshToken = getRefreshToken(now);
		if (refreshToken != null)
		{
			//save refresh token but internalToken.refreshToken is not set
			Date refreshExpiration = getRefreshTokenExpiration(now);
			tokensManagement.addToken(OAuthProcessor.INTERNAL_REFRESH_TOKEN,
					refreshToken.getValue(),
					new EntityParam(codeToken.getOwner()),
					internalToken.getSerialized(), now, refreshExpiration);
		}
		//set refresh token in access token
		internalToken.setRefreshToken(refreshToken.getValue());
		Date accessExpiration = getAccessTokenExpiration(now);

		AccessTokenResponse oauthResponse = getAccessTokenResponse(internalToken,
				accessToken, refreshToken, null);
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN,
				accessToken.getValue(), new EntityParam(codeToken.getOwner()),
				internalToken.getSerialized(), now, accessExpiration);

		return toResponse(Response.ok(getResponseContent(oauthResponse)));
	}

	private AccessTokenResponse getAccessTokenResponse(OAuthToken internalToken,
			AccessToken accessToken, RefreshToken refreshToken,
			Map<String, Object> additionalParams)
	{
		JWT signedJWT = decodeIDToken(internalToken);
		AccessTokenResponse oauthResponse = signedJWT == null
				? new AccessTokenResponse(new Tokens(accessToken, refreshToken),
						additionalParams)
				: new OIDCTokenResponse(new OIDCTokens(signedJWT, accessToken,
						refreshToken), additionalParams);
		return oauthResponse;
	}

	private Date getAccessTokenExpiration(Date now)
	{
		int accessTokenValidity = config
				.getIntValue(OAuthASProperties.ACCESS_TOKEN_VALIDITY);
		return new Date(now.getTime() + accessTokenValidity * 1000);
	}

	private Date getRefreshTokenExpiration(Date now)
	{
		int refreshTokenValidity = config
				.getIntValue(OAuthASProperties.REFRESH_TOKEN_VALIDITY);
		return new Date(now.getTime() + refreshTokenValidity * 1000);
	}

	private TokensPair loadAndRemoveAuthzCodeToken(String code)
			throws OAuthErrorException, EngineException
	{
		return tx.runInTransactionRetThrowing(() -> {
			try
			{
				Token codeToken = tokensManagement.getTokenById(
						OAuthProcessor.INTERNAL_CODE_TOKEN, code);
				OAuthToken parsedAuthzCodeToken = parseInternalToken(codeToken);

				long callerEntityId = InvocationContext.getCurrent()
						.getLoginSession().getEntityId();
				if (parsedAuthzCodeToken.getClientId() != callerEntityId)
				{
					log.warn("Client with id " + callerEntityId
							+ " presented authorization code issued "
							+ "for client "
							+ parsedAuthzCodeToken.getClientId());
					// intended - we mask the reason
					throw new OAuthErrorException(makeError(
							OAuth2Error.INVALID_GRANT, "wrong code"));
				}
				tokensManagement.removeToken(OAuthProcessor.INTERNAL_CODE_TOKEN,
						code);
				return new TokensPair(codeToken, parsedAuthzCodeToken);
			} catch (IllegalArgumentException e)
			{
				throw new OAuthErrorException(
						makeError(OAuth2Error.INVALID_GRANT, "wrong code"));
			}
		});
	}

	public static class OAuthErrorException extends EngineException
	{
		private Response response;

		public OAuthErrorException(Response response)
		{
			this.response = response;
		}
	}

	private static class TokensPair
	{
		Token codeToken;

		OAuthToken parsedAuthzCodeToken;

		public TokensPair(Token codeToken, OAuthToken parsedAuthzCodeToken)
		{
			this.codeToken = codeToken;
			this.parsedAuthzCodeToken = parsedAuthzCodeToken;
		}
	}
}
