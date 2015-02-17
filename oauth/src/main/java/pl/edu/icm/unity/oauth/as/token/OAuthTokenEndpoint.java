/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;


/**
 * Unity's RESTful endpoint, exposing OAuth token, userInfo and validation endpoints (in OAuth sense).  
 * @author K. Benedyczak
 */
public class OAuthTokenEndpoint extends RESTEndpoint
{
	public static final String TOKEN_PATH = "/token";
	public static final String USER_INFO_PATH = "/userinfo";
	public static final String JWK_PATH = "/jwk";
	public static final String TOKEN_INFO_PATH = "/tokeninfo";
	
	private TokensManagement tokensManagement;
	private PKIManagement pkiManagement;
	private OAuthASProperties config;
	private OAuthEndpointsCoordinator coordinator;
	
	public OAuthTokenEndpoint(UnityMessageSource msg, SessionManagement sessionMan,
			EndpointTypeDescription type, String servletPath, TokensManagement tokensMan,
			PKIManagement pkiManagement, OAuthEndpointsCoordinator coordinator, 
			AuthenticationProcessor authnProcessor)
	{
		super(msg, sessionMan, authnProcessor, type, servletPath);
		this.tokensManagement = tokensMan;
		this.pkiManagement = pkiManagement;
		this.coordinator = coordinator;
	}
	
	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		super.setSerializedConfiguration(serializedState);
		config = new OAuthASProperties(properties, pkiManagement, 
				getServletUrl(OAuthTokenEndpointFactory.PATH));
		coordinator.registerTokenEndpoint(config.getValue(OAuthASProperties.ISSUER_URI), 
				getServletUrl(""));
		addNotProtectedPaths(JWK_PATH, "/.well-known/openid-configuration", TOKEN_INFO_PATH, USER_INFO_PATH);
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
			ret.add(new AccessTokenResource(tokensManagement, config));
			ret.add(new DiscoveryResource(config, coordinator));
			ret.add(new KeysResource(config));
			ret.add(new TokenInfoResource(tokensManagement));
			ret.add(new UserInfoResource(tokensManagement));
			installExceptionHandlers(ret);
			return ret;
		}
	}

}
