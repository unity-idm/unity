/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.endpoint;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * RESTful endpoint for managing simple JWT authn: issuing, refreshing and invalidation of tokens.
 * 
 * @author K. Benedyczak
 */
public class JWTManagementEndpoint extends RESTEndpoint
{
	private TokensManagement tokensMan;
	private PKIManagement pkiManagement;
	private NetworkServer networkServer;
	private IdentitiesManagement identitiesMan;
	private JWTAuthenticationProperties config;
	
	public JWTManagementEndpoint(UnityMessageSource msg, SessionManagement sessionMan,
			EndpointTypeDescription type, String servletPath, TokensManagement tokensMan,
			PKIManagement pkiManagement, NetworkServer networkServer, IdentitiesManagement identitiesMan)
	{
		super(msg, sessionMan, type, servletPath);
		this.tokensMan = tokensMan;
		this.pkiManagement = pkiManagement;
		this.networkServer = networkServer;
		this.identitiesMan = identitiesMan;
	}

	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		super.setSerializedConfiguration(serializedState);
		try
		{
			config = new JWTAuthenticationProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the JWT management"
					+ " endpoint's configuration", e);
		}		
	}
	
	@Override
	protected Application getApplication()
	{
		String addr = networkServer.getAdvertisedAddress().toString();
		String realm = description.getRealm().getName();
		JWTManagement jwtMan = new JWTManagement(tokensMan, pkiManagement, identitiesMan,
				realm, addr, config);
		return new JWTManagementJAXRSApp(jwtMan);
	}

	@ApplicationPath("/")
	public static class JWTManagementJAXRSApp extends Application
	{
		private JWTManagement engine;
		
		public JWTManagementJAXRSApp(JWTManagement engine)
		{
			this.engine = engine;
		}

		@Override 
		public Set<Object> getSingletons() 
		{
			HashSet<Object> ret = new HashSet<>();
			ret.add(engine);
			return ret;
		}
	}
}
