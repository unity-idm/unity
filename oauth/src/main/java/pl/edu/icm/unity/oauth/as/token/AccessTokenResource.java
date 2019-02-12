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
import java.util.Optional;
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
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.EntityInGroup;
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
	private ClientCredentialsProcessor clientGrantProcessor;
	private OAuthIdPEngine notAuthorizedOauthIdpEngine;
	private OAuthRequestValidator requestValidator;
	private EntityManagement idMan;
	private AuthzCodeHandler authzCodeHandler;

	public AccessTokenResource(TokensManagement tokensManagement, OAuthASProperties config,
			OAuthRequestValidator requestValidator, 
			IdPEngine idpEngineInsecure,
			EntityManagement idMan, TransactionalRunner tx)
	{
		this.tokensManagement = tokensManagement;
		this.config = config;
		this.clientGrantProcessor = new ClientCredentialsProcessor(requestValidator,
				idpEngineInsecure, config);
		this.notAuthorizedOauthIdpEngine = new OAuthIdPEngine(idpEngineInsecure);
		this.requestValidator = requestValidator;
		this.idMan = idMan;
		this.authzCodeHandler = new AuthzCodeHandler(tokensManagement, config, tx);
	}

	@Path("/")
	@POST
	public Response getToken(@FormParam("grant_type") String grantType,
			@FormParam("code") String code, 
			@FormParam("scope") String scope,
			@FormParam("redirect_uri") String redirectUri,
			@FormParam("refresh_token") String refreshToken,
			@FormParam("audience") String audience,
			@FormParam("requested_token_type") String requestedTokenType,
			@FormParam("subject_token") String subjectToken,
			@FormParam("subject_token_type") String subjectTokenType,
			@FormParam("code_verifier") String codeVerifier)
			throws EngineException, JsonProcessingException
	{
		if (grantType == null)
			return makeError(OAuth2Error.INVALID_REQUEST, "grant_type is required");

		log.trace("Handle new token request with " + grantType + " grant");
		
		if (grantType.equals(GrantType.AUTHORIZATION_CODE.getValue()))
		{
			if (code == null)
				return makeError(OAuth2Error.INVALID_REQUEST, "code is required");
			return authzCodeHandler.handleAuthzCodeFlow(code, redirectUri, codeVerifier);
		} else if (grantType.equals(GrantType.CLIENT_CREDENTIALS.getValue()))
		{
			return handleClientCredentialFlow(scope);
		} else if (grantType.equals(EXCHANGE_GRANT))
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
		} else if (grantType.equals(GrantType.REFRESH_TOKEN.getValue()))
		{
			if (refreshToken == null)
				return makeError(OAuth2Error.INVALID_REQUEST,
						"refresh_token is required");
			return handleRefreshToken(refreshToken, scope);
		} else
		{
			return makeError(OAuth2Error.INVALID_GRANT,
					"wrong or not supported grant_type value");
		}
	}

	
	private void validateExchangeRequest(String subjectToken, String subjectTokenType,
			String requestedTokenType, String audience, long callerEntityId,
			EntityParam audienceEntity, List<String> oldRequestedScopesList)
			throws OAuthErrorException
	{
		if (!subjectTokenType.equals(ACCESS_TOKEN_TYPE_ID))
		{
			throw new OAuthErrorException(makeError(OAuth2Error.INVALID_REQUEST,
					"unsupported subject_token_type"));
		}

		if (requestedTokenType != null)
		{
			if (!(requestedTokenType.equals(ACCESS_TOKEN_TYPE_ID)
					|| requestedTokenType.equals(ID_TOKEN_TYPE_ID)))
			{
				throw new OAuthErrorException(makeError(OAuth2Error.INVALID_REQUEST,
						"unsupported requested_token_type"));
			}
		}

		Entity audienceResolvedEntity = null;
		try
		{
			audienceResolvedEntity = idMan.getEntity(audienceEntity);
			requestValidator.validateGroupMembership(audienceEntity, audience);

		} catch (IllegalIdentityValueException | OAuthValidationException oe)
		{
			throw new OAuthErrorException(
					makeError(OAuth2Error.INVALID_REQUEST, "wrong audience"));
		} catch (EngineException e)
		{
			throw new OAuthErrorException(makeError(OAuth2Error.SERVER_ERROR,
					"Internal error, can not retrieve OAuth client's data"));
		}

		if (!audienceResolvedEntity.getId().equals(callerEntityId))
			throw new OAuthErrorException(
					makeError(OAuth2Error.INVALID_REQUEST, "wrong audience"));

		if (!oldRequestedScopesList.contains(EXCHANGE_SCOPE))
		{
			throw new OAuthErrorException(makeError(OAuth2Error.INVALID_SCOPE,
					"Orginal token must have  " + EXCHANGE_SCOPE + " scope"));
		}
	}

	private Response handleExchangeToken(String subjectToken, String subjectTokenType,
			String requestedTokenType, String audience, String scope)
			throws EngineException, JsonProcessingException
	{

		long callerEntityId = InvocationContext.getCurrent().getLoginSession()
				.getEntityId();
		EntityParam audienceEntity = new EntityParam(
				new IdentityTaV(UsernameIdentity.ID, audience));
		
		
		Token subToken = null;
		OAuthToken parsedSubjectToken = null;

		try
		{
			subToken = tokensManagement.getTokenById(
					OAuthProcessor.INTERNAL_ACCESS_TOKEN, subjectToken);
			parsedSubjectToken = parseInternalToken(subToken);
		} catch (Exception e)
		{
			return makeError(OAuth2Error.INVALID_REQUEST, "wrong subject_token");
		}
		
		List<String> oldRequestedScopesList = Arrays
				.asList(parsedSubjectToken.getRequestedScope());
			
		try
		{
			validateExchangeRequest(subjectToken, subjectTokenType, requestedTokenType,
					audience, callerEntityId, audienceEntity, oldRequestedScopesList);
		} catch (OAuthErrorException e)
		{
			return e.response;
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
		newToken.setClientType(parsedSubjectToken.getClientType());

		try
		{
			newToken.setClientName(getClientName(audienceEntity));
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		Date now = new Date();
		AccessToken accessToken = OAuthProcessor.createAccessToken(newToken);
		newToken.setAccessToken(accessToken.getValue());
		
		RefreshToken refreshToken = TokenUtils.addRefreshToken(config, tokensManagement, now, 
				newToken, subToken.getOwner());
		Date accessExpiration = TokenUtils.getAccessTokenExpiration(config, now);

		Map<String, Object> additionalParams = new HashMap<>();
		additionalParams.put("issued_token_type", ACCESS_TOKEN_TYPE_ID);

		AccessTokenResponse oauthResponse = TokenUtils.getAccessTokenResponse(newToken, accessToken,
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
			return makeError(OAuth2Error.INVALID_REQUEST, "wrong refresh token");
		}

		long callerEntityId = InvocationContext.getCurrent().getLoginSession()
				.getEntityId();
		if (parsedRefreshToken.getClientId() != callerEntityId)
		{
			log.warn("Client with id " + callerEntityId
					+ " presented use refresh code issued " + "for client "
					+ parsedRefreshToken.getClientId());
			// intended - we mask the reason
			return makeError(OAuth2Error.INVALID_GRANT, "wrong refresh token");
		}

		List<String> oldRequestedScopesList = Arrays
				.asList(parsedRefreshToken.getRequestedScope());
		OAuthToken newToken = null;
		
		//When no scopes are requested RFC mandates to assign all originally assigned
		if (scope == null)
			scope = String.join(" ", oldRequestedScopesList);

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
		Date accessExpiration = TokenUtils.getAccessTokenExpiration(config, now);

		AccessToken accessToken = OAuthProcessor.createAccessToken(newToken);
		newToken.setAccessToken(accessToken.getValue());

		AccessTokenResponse oauthResponse = TokenUtils.getAccessTokenResponse(newToken, accessToken,
				null, null);
		log.debug("Refreshed access token {} of entity {}, valid until {}", 
				tokenToLog(accessToken.getValue()), refreshToken.getOwner(), accessExpiration);
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN,
				accessToken.getValue(), new EntityParam(refreshToken.getOwner()),
				newToken.getSerialized(), now, accessExpiration);

		return toResponse(Response.ok(getResponseContent(oauthResponse)));

	}

	private Response handleClientCredentialFlow(String scope)
			throws EngineException, JsonProcessingException
	{
		Date now = new Date();
		OAuthToken internalToken;
		try
		{
			internalToken = clientGrantProcessor.processClientFlowRequest(scope);
		} catch (OAuthValidationException e)
		{
			return makeError(OAuth2Error.INVALID_REQUEST, e.getMessage());
		}

		AccessToken accessToken = OAuthProcessor.createAccessToken(internalToken);
		internalToken.setAccessToken(accessToken.getValue());
		
		Date expiration = TokenUtils.getAccessTokenExpiration(config, now);
		log.debug("Client cred grant: issuing new access token {}, valid until {}", 
				tokenToLog(accessToken.getValue()), expiration);
		AccessTokenResponse oauthResponse = new AccessTokenResponse(
				new Tokens(accessToken, null));
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN,
				accessToken.getValue(),
				new EntityParam(internalToken.getClientId()),
				internalToken.getSerialized(), now, expiration);

		return toResponse(Response.ok(getResponseContent(oauthResponse)));
	}

	private String getClientName(EntityParam entity) throws OAuthErrorException
	{
		try
		{
			Map<String, AttributeExt> attributes = requestValidator
					.getAttributesNoAuthZ(entity);
			AttributeExt nameA = attributes
					.get(OAuthSystemAttributesProvider.CLIENT_NAME);
			if (nameA != null)
				return ((String) nameA.getValues().get(0));
			else
				return null;
		} catch (Exception e)
		{
			throw new OAuthErrorException(makeError(OAuth2Error.SERVER_ERROR, e.getMessage()));
		}
		
	}
	
	private OAuthToken prepareNewToken(OAuthToken oldToken, String scope,
			List<String> oldRequestedScopesList, long ownerId, long clientId,
			String clientUserName, boolean createIdToken, String grant)
			throws OAuthErrorException
	{
		OAuthToken newToken = new OAuthToken(oldToken);

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
		// if openid mode build new id_token using new userinfo.
		if (newRequestedScopeList.contains(OIDCScopeValue.OPENID.getValue())
				&& createIdToken)
		{
			try
			{
				newToken.setOpenidToken(createIdToken(now, newToken,
						Arrays.asList(new Audience(clientUserName)),
						userInfoClaimSet));
			} catch (Exception e)
			{
				log.error("Cannot create new id token", e);
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
		newToken.setIssuerUri(config.getIssuerName());;
		//responseType in newToken is the same as in oldToken
		//subject in newToken is the same as in oldToken
		
		return newToken;
	}

	private TranslationResult getAttributes(long clientId, long ownerId, String grant)
			throws OAuthErrorException
	{
		EntityInGroup client = new EntityInGroup(
				config.getValue(OAuthASProperties.CLIENTS_GROUP), 
				new EntityParam(clientId));
		TranslationResult userInfoRes = null;
		try
		{
			userInfoRes = notAuthorizedOauthIdpEngine.getUserInfoUnsafe(ownerId,
					String.valueOf(clientId), Optional.of(client),
					config.getValue(OAuthASProperties.USERS_GROUP),
					config.getValue(CommonIdPProperties.TRANSLATION_PROFILE),
					grant, config);
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
		
		if (signedJWT == null)
			return null;
		
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
				audience, TokenUtils.getAccessTokenExpiration(config, now), now);
		newClaims.setNonce(oldClaims.getNonce());

		ResponseType responseType = null;
		if (token.getResponseType() != null && !token.getResponseType().isEmpty())
		{
			responseType = ResponseType.parse(token.getResponseType());
			if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN)
					&& responseType.size() == 1)
				newClaims.putAll(userInfoClaimSet);
		}

		return config.getTokenSigner().sign(newClaims).serialize();
	}

	public static class OAuthErrorException extends EngineException
	{
		Response response;

		public OAuthErrorException(Response response)
		{
			this.response = response;
		}
	}
}
