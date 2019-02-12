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

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.rest.authn.ext.HttpBasicRetrievalBase;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
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
	public static final String TOKEN_REVOCATION_PATH = "/revoke";
	
	private TokensManagement tokensManagement;
	private PKIManagement pkiManagement;
	private OAuthASProperties config;
	private OAuthEndpointsCoordinator coordinator;
	private TransactionalRunner tx;
	private IdPEngine insecureIdPEngine;

	//insecure
	private AttributesManagement attributesMan;
	private EntityManagement identitiesMan;
	
	@Autowired
	public OAuthTokenEndpoint(UnityMessageSource msg, SessionManagement sessionMan,
			NetworkServer server, TokensManagement tokensMan,
			PKIManagement pkiManagement, OAuthEndpointsCoordinator coordinator, 
			AuthenticationProcessor authnProcessor, EntityManagement identitiesMan,
			@Qualifier("insecure") AttributesManagement attributesMan, 
			TransactionalRunner tx, @Qualifier("insecure") IdPEngine idPEngine)
	{
		super(msg, sessionMan, authnProcessor, server, PATH);
		this.tokensManagement = tokensMan;
		this.pkiManagement = pkiManagement;
		this.coordinator = coordinator;
		this.identitiesMan = identitiesMan;
		this.attributesMan = attributesMan;
		this.tx = tx;
		this.insecureIdPEngine = idPEngine;
		
	}
	
	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		super.setSerializedConfiguration(serializedState);
		config = new OAuthASProperties(properties, pkiManagement, 
				getServletUrl(PATH));
		coordinator.registerTokenEndpoint(config.getValue(OAuthASProperties.ISSUER_URI), 
				getServletUrl(""));
		addNotProtectedPaths(JWK_PATH, "/.well-known/openid-configuration", TOKEN_INFO_PATH, USER_INFO_PATH,
				TOKEN_REVOCATION_PATH);
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
			ret.add(new AccessTokenResource(tokensManagement, config, 
					new OAuthRequestValidator(config, identitiesMan, attributesMan), 
					insecureIdPEngine, identitiesMan, tx));
			ret.add(new DiscoveryResource(config, coordinator));
			ret.add(new KeysResource(config));
			ret.add(new TokenInfoResource(tokensManagement));
			ret.add(new UserInfoResource(tokensManagement));
			ret.add(new RevocationResource(tokensManagement, sessionMan, getEndpointDescription().getRealm()));
			installExceptionHandlers(ret);
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
