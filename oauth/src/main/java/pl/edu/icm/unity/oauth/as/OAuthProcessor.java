/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue.ID_TOKEN;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.claims.AccessTokenHash;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.CodeHash;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.SerializableRemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthToken.PKCSInfo;
import pl.edu.icm.unity.oauth.as.token.access.AccessTokenFactory;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;

/**
 * Groups OAuth related logic for processing the request and preparing the response.  
 * @author K. Benedyczak
 */
@Component
public class OAuthProcessor
{
	public static final String INTERNAL_CODE_TOKEN = "oauth2Code";

	
	
	private final TokensManagement tokensMan;
	private final OAuthAccessTokenRepository tokenDAO;

	@Autowired
	public OAuthProcessor(TokensManagement tokensMan, OAuthAccessTokenRepository tokenDAO,
			ApplicationEventPublisher eventPublisher, MessageSource msg)
	{
		this.tokensMan = tokensMan;
		this.tokenDAO = tokenDAO;
	}

	/**
	 * Returns only requested attributes for which we have mapping.
	 */
	
	public static Set<DynamicAttribute> filterAttributes(TranslationResult userInfo, 
			Set<String> requestedAttributes)
	{
		return filterAttributes(userInfo, requestedAttributes, null);
	}
	
	
	public static Set<DynamicAttribute> filterAttributes(TranslationResult userInfo, 
			Set<String> requestedAttributes, List<AttributeFilteringSpec> attrsValuesFilter)
	{
		Set<DynamicAttribute> ret = filterNotRequestedAttributes(userInfo, requestedAttributes);
		return attrsValuesFilter == null ? filterUnsupportedAttributes(ret)
				: AttributeValueFilter.filterAttributes(attrsValuesFilter, filterUnsupportedAttributes(ret));
	}

	/**
	 * Returns Authorization response to be returned and records (if needed) 
	 * the internal state token, which is needed to associate further use of the code and/or id tokens with
	 * the authorization that currently takes place.
	 */
	public AuthorizationSuccessResponse prepareAuthzResponseAndRecordInternalState(
			Collection<DynamicAttribute> attributes,
			IdentityParam identity,	OAuthAuthzContext ctx, OAuthIdpStatisticReporter statReporter, Instant authenticationTime,
			List<AttributeFilteringSpec> attributeWhiteList) 
					throws EngineException, JsonProcessingException, ParseException, JOSEException
	{
		OAuthToken internalToken = new OAuthToken();
		OAuthASProperties config = ctx.getConfig();
		internalToken.setEffectiveScope(ctx.getEffectiveRequestedScopes().stream().toList());
		internalToken.setRequestedScope(ctx.getRequestedScopes().stream().toArray(String[]::new));
		internalToken.setClientId(ctx.getClientEntityId());
		internalToken.setRedirectUri(ctx.getReturnURI().toASCIIString());
		internalToken.setClientName(ctx.getClientName());
		internalToken.setClientUsername(ctx.getClientUsername());
		internalToken.setSubject(identity.getValue());
		internalToken.setMaxExtendedValidity(config.getMaxExtendedAccessTokenValidity());
		internalToken.setTokenValidity(config.getAccessTokenValidity()); 
		internalToken.setAudience(Stream.concat(Stream.of(ctx.getClientUsername()), ctx.getAdditionalAudience().stream()).collect(Collectors.toList()));
		internalToken.setIssuerUri(config.getIssuerName());
		internalToken.setClientType(ctx.getClientType());
		internalToken.setClaimsInTokenAttribute(ctx.getClaimsInTokenAttribute());
		internalToken.setAuthenticationTime(authenticationTime);
		internalToken.setAttributeValueFilters(attributeWhiteList);
		internalToken.setRequestedACR(RequestedACRMapper.mapToInternalACRFromNimbusdsACRType(ctx.getRequestedAcr()));
		if (InvocationContext.hasCurrent())
		{
			Optional.ofNullable(InvocationContext.getCurrent())
					.ifPresent(context -> Optional.ofNullable(context.getLoginSession())
							.ifPresent(loginSession -> Optional
									.ofNullable(loginSession.getFirstFactorRemoteIdPAuthnContext())
									.ifPresent(firstFactorRemoteIdPContext -> internalToken
											.setRemoteIdPAuthnContext(SerializableRemoteAuthnMetadata.builder()
													.withClassReferences(firstFactorRemoteIdPContext.classReferences())
													.withProtocol(firstFactorRemoteIdPContext.protocol())
													.withRemoteIdPId(firstFactorRemoteIdPContext.remoteIdPId())
													.build()))));
		}
		String codeChallenge = ctx.getRequest().getCodeChallenge() == null ? 
				null : ctx.getRequest().getCodeChallenge().getValue();
		String codeChallengeMethod = ctx.getRequest().getCodeChallengeMethod() == null ? 
				null : ctx.getRequest().getCodeChallengeMethod().getValue();
		PKCSInfo pkcsInfo = new PKCSInfo(codeChallenge, codeChallengeMethod);
		internalToken.setPkcsInfo(pkcsInfo);
	
		Date now = new Date();
		
		ResponseType responseType = ctx.getRequest().getResponseType();
		internalToken.setResponseType(responseType.toString());
		
		UserInfo userInfo = prepareUserInfoClaimSet(identity.getValue(), attributes);
		internalToken.setUserInfo(userInfo.toJSONObject().toJSONString());
		
		Optional<IDTokenClaimsSet> idToken = generateIdTokenIfRequested(config, ctx, responseType, 
				internalToken, identity, userInfo, now);
		TokenSigner tokenSigner = config.getTokenSigner();
		JWSAlgorithm signingAlgorithm = tokenSigner.isPKIEnabled() ? 
				tokenSigner.getSigningAlgorithm() : null;
		Curve curve = tokenSigner.getCurve();
		
		AuthorizationSuccessResponse oauthResponse = null;
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		if (GrantFlow.authorizationCode == ctx.getFlow())
		{
			AuthorizationCode authzCode = new AuthorizationCode();
			internalToken.setAuthzCode(authzCode.getValue());
			
			signAndRecordIdToken(idToken, tokenSigner, responseType, internalToken);
			
			oauthResponse = new AuthorizationSuccessResponse(ctx.getReturnURI(), authzCode, null,
					ctx.getRequest().getState(), ctx.getRequest().impliedResponseMode());
			Date expiration = new Date(now.getTime() + config.getCodeTokenValidity() * 1000);
			tokensMan.addToken(INTERNAL_CODE_TOKEN, authzCode.getValue(), 
					new EntityParam(identity), internalToken.getSerialized(), now, expiration);
		} else if (GrantFlow.implicit == ctx.getFlow())
		{
			if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN) && responseType.size() == 1)
			{
				Optional<JWT> idTokenSigned = signAndRecordIdToken(idToken, tokenSigner, 
						responseType, internalToken);
				//we return only the id token, no access token so we don't need an internal token.
				return new AuthenticationSuccessResponse(
						ctx.getReturnURI(), null, idTokenSigned.orElse(null), 
						null, ctx.getRequest().getState(), null, 
						ctx.getRequest().impliedResponseMode());
			}

			AccessToken accessToken = accessTokenFactory.create(internalToken, now);
			internalToken.setAccessToken(accessToken.getValue());
			
			addAccessTokenHashIfNeededToIdToken(idToken, accessToken, signingAlgorithm, responseType, curve);
			Optional<JWT> idTokenSigned = signAndRecordIdToken(idToken, tokenSigner, 
					responseType, internalToken);
			
			Date expiration = new Date(now.getTime() + config.getAccessTokenValidity() * 1000);
			oauthResponse = new AuthenticationSuccessResponse(
						ctx.getReturnURI(), null, idTokenSigned.orElse(null), 
						accessToken, ctx.getRequest().getState(), null, 
						ctx.getRequest().impliedResponseMode());
			statReporter.reportStatus(ctx, Status.SUCCESSFUL);
			tokenDAO.storeAccessToken(accessToken, internalToken, new EntityParam(identity), now, expiration);
		} else if (GrantFlow.openidHybrid == ctx.getFlow())
		{
			//in hybrid mode authz code is returned always
			AuthorizationCode authzCode = new AuthorizationCode();
			internalToken.setAuthzCode(authzCode.getValue());
			Date codeExpiration = new Date(now.getTime() + config.getCodeTokenValidity() * 1000);
			addCodeHashIfNeededToIdToken(idToken, authzCode, signingAlgorithm, responseType, curve);

			signAndRecordIdToken(idToken, tokenSigner, responseType, internalToken);
			tokensMan.addToken(INTERNAL_CODE_TOKEN, authzCode.getValue(), 
					new EntityParam(identity), internalToken.getSerialized(), 
					now, codeExpiration);
			
			//access token - sometimes
			AccessToken accessToken = null;
			if (responseType.contains(ResponseType.Value.TOKEN))
			{
				accessToken = accessTokenFactory.create(internalToken, now);
				internalToken.setAccessToken(accessToken.getValue());
				Date accessExpiration = new Date(now.getTime() + config.getAccessTokenValidity() * 1000);
				addAccessTokenHashIfNeededToIdToken(idToken, accessToken, signingAlgorithm, responseType, curve);
				
				signAndRecordIdToken(idToken, tokenSigner, responseType, internalToken);
				statReporter.reportStatus(ctx, Status.SUCCESSFUL);
				tokenDAO.storeAccessToken(accessToken, internalToken, new EntityParam(identity), now, 
						accessExpiration);
			}
			
			Optional<JWT> idTokenSigned = signAndRecordIdToken(idToken, tokenSigner, 
					responseType, internalToken);

			oauthResponse = new AuthenticationSuccessResponse(
					ctx.getReturnURI(), authzCode, idTokenSigned.orElse(null), 
					accessToken, ctx.getRequest().getState(), null, 
					ctx.getRequest().impliedResponseMode());
		}
		
		return oauthResponse;
	}
	
	private Optional<IDTokenClaimsSet> generateIdTokenIfRequested(OAuthASProperties config, OAuthAuthzContext ctx, 
			ResponseType responseType, OAuthToken internalToken, IdentityParam identity, 
			UserInfo userInfo, Date now) throws ParseException, JOSEException
	{
		return Optional.ofNullable(ctx.isOpenIdMode() ? 
				prepareIdInfoClaimSet(identity.getValue(), internalToken.getAudience(), ctx, userInfo, now, internalToken.getAuthenticationTime()) 
				: null);
	}

	private Optional<JWT> signAndRecordIdToken(Optional<IDTokenClaimsSet> idToken, TokenSigner tokenSigner, 
			ResponseType responseType, OAuthToken internalToken) throws ParseException, JOSEException
	{
		if (!idToken.isPresent())
			return Optional.empty();
		JWT idTokenSigned = tokenSigner.sign(idToken.get());	
		internalToken.setOpenidToken(idTokenSigned.serialize());
		//we record OpenID token in internal state always in open id mode. However it may happen
		//that it is not requested immediately now
		if (!responseType.contains(OIDCResponseTypeValue.ID_TOKEN))
			idTokenSigned = null;
		return Optional.ofNullable(idTokenSigned);
	}
	
	/**
	 * Returns a collection of attributes including only those attributes for which there is an OAuth 
	 * representation.
	 */
	private static Set<DynamicAttribute> filterUnsupportedAttributes(Set<DynamicAttribute> src)
	{
		Set<DynamicAttribute> ret = new HashSet<>();
		OAuthAttributeMapper mapper = new DefaultOAuthAttributeMapper();
		
		for (DynamicAttribute a: src)
			if (mapper.isHandled(a.getAttribute()))
				ret.add(a);
		return ret;
	}
	
	
	private static Set<DynamicAttribute> filterNotRequestedAttributes(TranslationResult translationResult, 
			Set<String> requestedAttributes)
	{
		Collection<DynamicAttribute> allAttrs = translationResult.getAttributes();
		Set<DynamicAttribute> filteredAttrs = new HashSet<>();
		
		for (DynamicAttribute attr: allAttrs)
			if (requestedAttributes.contains(attr.getAttribute().getName()))
				filteredAttrs.add(attr);
		return filteredAttrs;
	}
	
	
	/**
	 * Creates an OIDC ID Token. The token includes regular attributes if and only if the access token is 
	 * not issued in the flow. This is the case if the only response type is 'id_token'. Section 5.4 of 
	 * OIDC specification.
	 */
	private IDTokenClaimsSet prepareIdInfoClaimSet(String userIdentity, List<String> audience, OAuthAuthzContext context, 
			ClaimsSet regularAttributes, Date now, Instant authenticationTime)
	{
		AuthenticationRequest request = (AuthenticationRequest) context.getRequest();
		IDTokenClaimsSet idToken = new IDTokenClaimsSet(
				new Issuer(context.getConfig().getIssuerName()), 
				new Subject(userIdentity), 
				audience.stream().filter(a -> a != null).map(Audience::new).collect(Collectors.toList()), 
				new Date(now.getTime() + context.getConfig().getIdTokenValidity()*1000), 
				now);
		idToken.setAuthenticationTime(Date.from(authenticationTime));
		ResponseType responseType = request.getResponseType();
		boolean onlyIdTokenRequested = responseType.contains(ID_TOKEN) && responseType.size() == 1; 

		if (onlyIdTokenRequested || context.requestsAttributesInIdToken())
			idToken.putAll(regularAttributes);
		
		if (request.getNonce() != null)
			idToken.setNonce(request.getNonce());
		return idToken;
	}
	
	private void addAccessTokenHashIfNeededToIdToken(Optional<IDTokenClaimsSet> idTokenOpt, AccessToken accessToken, 
			JWSAlgorithm jwsAlgorithm, ResponseType responseType, Curve curve)
	{
		if (!idTokenOpt.isPresent())
			return;
		IDTokenClaimsSet idToken = idTokenOpt.get();
		boolean onlyIdTokenRequested = responseType.contains(ID_TOKEN) && responseType.size() == 1; 
		if (!onlyIdTokenRequested)
			idToken.setAccessTokenHash(AccessTokenHash.compute(accessToken, jwsAlgorithm, curve));
	}

	private void addCodeHashIfNeededToIdToken(Optional<IDTokenClaimsSet> idTokenOpt, AuthorizationCode code, 
			JWSAlgorithm jwsAlgorithm, ResponseType responseType, Curve curve)
	{
		if (!idTokenOpt.isPresent())
			return;
		IDTokenClaimsSet idToken = idTokenOpt.get();
		if (responseType.contains(ID_TOKEN) && responseType.contains(ResponseType.Value.CODE))
			idToken.setCodeHash(CodeHash.compute(code, jwsAlgorithm, curve));
	}
	
	public static UserInfo prepareUserInfoClaimSet(String userIdentity, Collection<DynamicAttribute> attributes)
	{
		UserInfo userInfo = new UserInfo(new Subject(userIdentity));
		
		OAuthAttributeMapper mapper = new DefaultOAuthAttributeMapper();
		
		for (DynamicAttribute dat: attributes)
		{
			Attribute attr = dat.getAttribute();
			if (mapper.isHandled(attr))
			{
				String name = mapper.getJsonKey(attr);
				Object value = mapper.getJsonValue(attr);
				userInfo.setClaim(name, value);
			}
		}
		
		return userInfo;
	}
}
