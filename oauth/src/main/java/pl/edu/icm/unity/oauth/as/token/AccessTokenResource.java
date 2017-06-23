/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;
import pl.edu.icm.unity.rest.jwt.JWTUtils;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
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
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.TOKEN_PATH)
public class AccessTokenResource extends BaseOAuthResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH,
			AccessTokenResource.class);

	private TokensManagement tokensManagement;
	private OAuthASProperties config;
	private TransactionalRunner tx;
	private ClientCredentialsProcessor clientGrantProcessor;
	private OAuthIdPEngine oauthIdpEngine;
	private EntityManagement identitiesMan;
	private OAuthRequestValidator requestValidator;

	public AccessTokenResource(TokensManagement tokensManagement,
			EntityManagement identitiesMan, OAuthASProperties config,
			OAuthRequestValidator requestValidator, IdPEngine idpEngine,
			TransactionalRunner tx)
	{
		this.tokensManagement = tokensManagement;
		this.config = config;
		this.tx = tx;
		this.clientGrantProcessor = new ClientCredentialsProcessor(requestValidator,
				idpEngine, config);
		this.oauthIdpEngine = new OAuthIdPEngine(idpEngine);
		this.identitiesMan = identitiesMan;
		this.requestValidator = requestValidator;
	}

	@Path("/")
	@POST
	public Response getToken(@FormParam("grant_type") String grantType,
			@FormParam("code") String code, @FormParam("scope") String scope,
			@FormParam("redirect_uri") String redirectUri,
			@FormParam("refresh_token") String refreshToken)
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

	private Response handleRefreshToken(String refToken, String scope)
			throws EngineException, JsonProcessingException
	{

		Token refreshToken = tokensManagement
				.getTokenById(OAuthProcessor.INTERNAL_REFRESH_TOKEN, refToken);
		OAuthToken parsedRefreshToken = parseInternalToken(refreshToken);

		long callerEntityId = InvocationContext.getCurrent().getLoginSession()
				.getEntityId();
		if (parsedRefreshToken.getClientId() != callerEntityId)
		{
			log.warn("Client with id " + callerEntityId
					+ " presented use refresh code issued " + "for client "
					+ parsedRefreshToken.getClientId());
			return makeError(OAuth2Error.INVALID_GRANT, "wrong refresh code");
		}
		
		String newScopes = new String();
		if (scope != null && !scope.isEmpty())
		{
			newScopes = scope;
		}
		boolean openIdMode = newScopes.contains(OIDCScopeValue.OPENID.getValue()) && parsedRefreshToken.isOpenIdMode();
		List<ScopeInfo> newValidRequestedScopes = requestValidator
				.getValidRequestedScopes(Scope.parse(newScopes));
		
		String oldScopes = new String();
		if (parsedRefreshToken.getScope() != null)
		{
			oldScopes = String.join(" ", parsedRefreshToken.getScope());
		}
		// simply check scope
		List<ScopeInfo> filteredRefreshScopes = filterRefreshScope(newValidRequestedScopes, oldScopes);
		if (!newValidRequestedScopes.isEmpty() && filteredRefreshScopes.size() == 0 && !oldScopes.isEmpty())
		{
			return makeError(OAuth2Error.INVALID_SCOPE, "wrong scope");
		}

		Entity userId = null;
		// get identity associated with refresh key
		try
		{
			userId = identitiesMan.getEntity(new EntityParam(
					new IdentityTaV(parsedRefreshToken.getSubjectType(),
							parsedRefreshToken.getSubject(),
							parsedRefreshToken.getSubjectTarget(),
							parsedRefreshToken.getSubjectRealm())));

		} catch (EngineException e)
		{
			return makeError(OAuth2Error.INVALID_CLIENT,
					"Can not get indentity associated with refresh key");
		}

		// get attributes for identity
		TranslationResult userInfoRes = null;
		try
		{
			userInfoRes = oauthIdpEngine.getUserInfo(userId.getId(),
					String.valueOf(callerEntityId),
					config.getValue(OAuthASProperties.USERS_GROUP),
					config.getValue(CommonIdPProperties.TRANSLATION_PROFILE),
					GrantType.REFRESH_TOKEN.getValue(), false);
		} catch (EngineException e)
		{
			return makeError(OAuth2Error.INVALID_CLIENT,
					"Can not get user info associated with refresh key");
		}

		UserInfo userInfoClaimSet = createUserInfo(filteredRefreshScopes, parsedRefreshToken,
				userInfoRes);
		parsedRefreshToken.setUserInfo(userInfoClaimSet.toJSONObject().toJSONString());
		
		Date now = new Date();
		// if openid mode build new id_token using new userinfo
		if (openIdMode)
		{
			try
			{
				parsedRefreshToken.setOpenidToken(createIdToken(now,
						parsedRefreshToken, userInfoClaimSet));
			} catch (Exception e)
			{
				throw new InternalException("can not genereate new id token", e);
			}
		}else
		{
			//clear openidToken
			parsedRefreshToken.setOpenidToken(null);
		}
		
		AccessToken accessToken = new BearerAccessToken();
		parsedRefreshToken.setAccessToken(accessToken.getValue());
		Date accessExpiration = getAccessTokenExpiration(now);
	
		AccessTokenResponse oauthResponse = getAccessTokenResponse(parsedRefreshToken,
				accessToken, null);
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN,
				accessToken.getValue(), new EntityParam(refreshToken.getOwner()),
				parsedRefreshToken.getSerialized(), now, accessExpiration);

		return toResponse(Response.ok(getResponseContent(oauthResponse)));

	}
	
	private UserInfo createUserInfo(List<ScopeInfo> filteredRefreshScopes, OAuthToken refreshToken,
			TranslationResult userInfoRes)
	{
		Set<String> requestedAttributes = new HashSet<>();
		for (ScopeInfo si : filteredRefreshScopes)
			requestedAttributes.addAll(si.getAttributes());

		OAuthProcessor processor = new OAuthProcessor();
		Collection<DynamicAttribute> attributes = processor.filterAttributes(userInfoRes,
				requestedAttributes);

		return processor.prepareUserInfoClaimSet(refreshToken.getSubject(), attributes);
	}
	
	private List<ScopeInfo> filterRefreshScope(List<ScopeInfo> newScopes, String oldScopes)
	{
		List<ScopeInfo> filteredRequestedScopes = new ArrayList<>();
		for (ScopeInfo validScope : newScopes)
		{
			
			if (oldScopes.contains(validScope.getName()))
			{
				filteredRequestedScopes.add(validScope);
			}
			
		}
		return filteredRequestedScopes;

	}

	private String createIdToken(Date now, OAuthToken refreshToken, UserInfo userInfoClaimSet)
			throws ParseException, JOSEException
	{
		JWT signedJWT = decodeIDToken(refreshToken);
		IDTokenClaimsSet oldClaims;
		try
		{
			oldClaims = new IDTokenClaimsSet(signedJWT.getJWTClaimsSet());
		} catch (Exception e)
		{
			throw new InternalException("Can not parse the internal id token", e);
		}
		IDTokenClaimsSet newClaims = new IDTokenClaimsSet(oldClaims.getIssuer(),
				oldClaims.getSubject(), oldClaims.getAudience(),
				getAccessTokenExpiration(now), now);
		newClaims.setNonce(oldClaims.getNonce());

		ResponseType responseType = null;
		if (refreshToken.getResponseType() != null
				&& !refreshToken.getResponseType().isEmpty())
		{
			responseType = ResponseType.parse(refreshToken.getResponseType());

			if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN)
					&& responseType.size() == 1)
				newClaims.putAll(userInfoClaimSet);
		}
		
		return JWTUtils.generate(config.getCredential(), newClaims.toJWTClaimsSet());
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

		RefreshToken refreshToken = null;
		if (config.getIntValue(OAuthASProperties.REFRESH_TOKEN_VALIDITY) > 0)
		{
			refreshToken = new RefreshToken();
			internalToken.setRefreshToken(refreshToken.getValue());
		}

		Date now = new Date();
		Date accessExpiration = getAccessTokenExpiration(now);

		AccessTokenResponse oauthResponse = getAccessTokenResponse(internalToken,
				accessToken, refreshToken);
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN,
				accessToken.getValue(), new EntityParam(codeToken.getOwner()),
				internalToken.getSerialized(), now, accessExpiration);

		if (refreshToken != null)
		{
			Date refreshExpiration = getRefreshTokenExpiration(now);
			tokensManagement.addToken(OAuthProcessor.INTERNAL_REFRESH_TOKEN,
					refreshToken.getValue(),
					new EntityParam(codeToken.getOwner()),
					internalToken.getSerialized(), now, refreshExpiration);
		}

		return toResponse(Response.ok(getResponseContent(oauthResponse)));
	}

	private AccessTokenResponse getAccessTokenResponse(OAuthToken internalToken,
			AccessToken accessToken, RefreshToken refreshToken)
	{
		JWT signedJWT = decodeIDToken(internalToken);
		AccessTokenResponse oauthResponse = signedJWT == null
				? new AccessTokenResponse(new Tokens(accessToken, refreshToken))
				: new OIDCTokenResponse(new OIDCTokens(signedJWT, accessToken,
						refreshToken));

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
