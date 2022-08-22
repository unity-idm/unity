/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.token.AuthzCodeHandler.AuthzCodeHandlerFactory;
import pl.edu.icm.unity.oauth.as.token.CredentialFlowHandler.CredentialFlowHandlerFactory;
import pl.edu.icm.unity.oauth.as.token.ExchangeTokenHandler.ExchangeTokenHandlerFactory;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenStatisticPublisher.OAuthTokenStatisticPublisherFactory;
import pl.edu.icm.unity.oauth.as.token.RefreshTokenHandler.RefreshTokenHandlerFactory;
import pl.edu.icm.unity.oauth.as.token.exception.OAuthExceptionMapper;
import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.rest.authn.ext.HttpBasicRetrievalBase;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;


/**
 * Unity's RESTful endpoint, exposing OAuth token, userInfo and validation endpoints (in OAuth sense).  
 * @author K. Benedyczak
 */
@PrototypeComponent
public class OAuthTokenEndpoint extends RESTEndpoint
{
	public static final String NAME = "OAuth2Token";
	public static final String PATH = "";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "A RESTful endpoint exposing OAuth and OIDC related, client-focused endpoints.", 
			JAXRSAuthentication.NAME,
			Collections.singletonMap(PATH, "The OAuth base path"),
			getEndpointFeatures());
	
	public static final String TOKEN_PATH = "/token";
	public static final String USER_INFO_PATH = "/userinfo";
	public static final String JWK_PATH = "/jwk";
	public static final String TOKEN_INFO_PATH = "/tokeninfo";
	public static final String TOKEN_INTROSPECTION_PATH = "/introspect";
	public static final String TOKEN_REVOCATION_PATH = "/revoke";
	
	private PKIManagement pkiManagement;
	private OAuthASProperties config;
	private OAuthEndpointsCoordinator coordinator;
	private final OAuthScopesService scopeService;
	private final AuthzCodeHandlerFactory authzCodeHandlerFactory;
	private final RefreshTokenHandlerFactory refreshTokenHandlerFactory;
	private final ExchangeTokenHandlerFactory exchangeTokenHandlerFactory;
	private final CredentialFlowHandlerFactory credentialFlowHandlerFactory;
	private final OAuthTokenStatisticPublisherFactory statisticPublisherFactory;
	//insecure
	private final OAuthAccessTokenRepository accessTokenRepository;
	private final OAuthRefreshTokenRepository refreshTokenRepository;
	
	
	@Autowired
	public OAuthTokenEndpoint(MessageSource msg, SessionManagement sessionMan, NetworkServer server,
			PKIManagement pkiManagement, OAuthEndpointsCoordinator coordinator, AuthenticationProcessor authnProcessor,
			EntityManagement identitiesMan, @Qualifier("insecure") AttributesManagement attributesMan,
			@Qualifier("insecure") IdPEngine idPEngine, TokensManagement tokensManagement,
			OAuthAccessTokenRepository accessTokenRepository, OAuthRefreshTokenRepository refreshTokenRepository,
			AdvertisedAddressProvider advertisedAddrProvider, OAuthScopesService scopeService,
			AuthzCodeHandlerFactory authzCodeHandlerFactory, RefreshTokenHandlerFactory refreshTokenHandlerFactory,
			ExchangeTokenHandlerFactory exchangeTokenHandlerFactory,
			CredentialFlowHandlerFactory credentialFlowHandlerFactory,
			OAuthTokenStatisticPublisherFactory statisticPublisherFactory)
	{
		super(msg, sessionMan, authnProcessor, server, advertisedAddrProvider, PATH, identitiesMan);
		this.pkiManagement = pkiManagement;
		this.coordinator = coordinator;
		this.statisticPublisherFactory = statisticPublisherFactory;
		this.accessTokenRepository = accessTokenRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.scopeService = scopeService;
		this.authzCodeHandlerFactory = authzCodeHandlerFactory;
		this.refreshTokenHandlerFactory = refreshTokenHandlerFactory;
		this.exchangeTokenHandlerFactory = exchangeTokenHandlerFactory;
		this.credentialFlowHandlerFactory = credentialFlowHandlerFactory;
	}
	
	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		super.setSerializedConfiguration(serializedState);
		config = new OAuthASProperties(properties, pkiManagement, 
				getServletUrl(PATH));
		coordinator.registerTokenEndpoint(config.getValue(OAuthASProperties.ISSUER_URI), 
				getServletUrl(""));
		addNotProtectedPaths(JWK_PATH, "/.well-known/openid-configuration", TOKEN_INFO_PATH, USER_INFO_PATH);
		addOptionallyAuthenticatedPaths(TOKEN_REVOCATION_PATH, TOKEN_PATH);
	}
	
	@Override
	protected Application getApplication()
	{
		return new OAuthTokenJAXRSApp();
	}

	@ApplicationPath("/")
	public class OAuthTokenJAXRSApp extends Application
	{

		@Override 
		public Set<Object> getSingletons() 
		{
			HashSet<Object> ret = new HashSet<>();
			ret.add(new AccessTokenResource(authzCodeHandlerFactory.getHandler(config, description),
					refreshTokenHandlerFactory.getHandler(config),
					exchangeTokenHandlerFactory.getHandler(config, description),
					credentialFlowHandlerFactory.getHandler(config, description),
					statisticPublisherFactory.getOAuthTokenStatisticPublisher(config, description)));
			ret.add(new DiscoveryResource(config, coordinator, scopeService));
			ret.add(new KeysResource(config));
			ret.add(new TokenInfoResource(accessTokenRepository));
			ret.add(new TokenIntrospectionResource(accessTokenRepository, refreshTokenRepository));
			ret.add(new UserInfoResource(accessTokenRepository));
			ret.add(new RevocationResource(accessTokenRepository, refreshTokenRepository,
					sessionMan, getEndpointDescription().getRealm(),
					config.getBooleanValue(OAuthASProperties.ALLOW_UNAUTHENTICATED_REVOCATION)));
			OAuthExceptionMapper.installExceptionHandlers(ret);
			return ret;
		}
	}

	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<OAuthTokenEndpoint> factory;
		
		@Override
		public EndpointTypeDescription getDescription()
		{
			return TYPE;
		}

		@Override
		public EndpointInstance newInstance()
		{
			return factory.getObject();
		}
	}
	
	private static Properties getEndpointFeatures()
	{
		Properties ret = new Properties();
		ret.setProperty(HttpBasicRetrievalBase.FEATURE_HTTP_BASIC_URLENCODED, "true");
		return ret;
	}
}
