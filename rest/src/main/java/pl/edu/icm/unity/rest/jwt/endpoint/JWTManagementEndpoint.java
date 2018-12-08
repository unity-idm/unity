/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.endpoint;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * RESTful endpoint for managing simple JWT authn: issuing, refreshing and invalidation of tokens.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class JWTManagementEndpoint extends RESTEndpoint
{
	public static final String NAME = "JWTMan";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "A RESTful endpoint allowing for management of tokens (issuing, refreshing) "
					+ "which are subsequently used to authenticate to Unity by "
					+ "non-browser clients in a simple way.", 
			JAXRSAuthentication.NAME,
			Collections.singletonMap("", "The REST management base path"));

	private TokensManagement tokensMan;
	private PKIManagement pkiManagement;
	private EntityManagement identitiesMan;
	private JWTAuthenticationProperties config;
	
	@Autowired
	public JWTManagementEndpoint(UnityMessageSource msg, SessionManagement sessionMan,
			AuthenticationProcessor authenticationProcessor,
			TokensManagement tokensMan,
			PKIManagement pkiManagement, NetworkServer networkServer, EntityManagement identitiesMan)
	{
		super(msg, sessionMan, authenticationProcessor, networkServer, "");
		this.tokensMan = tokensMan;
		this.pkiManagement = pkiManagement;
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
		String addr = httpServer.getAdvertisedAddress().toString();
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
	
	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<JWTManagementEndpoint> factory;
		
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
}
