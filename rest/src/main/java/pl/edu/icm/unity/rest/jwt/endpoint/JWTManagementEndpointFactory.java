/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.endpoint;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Factory for {@link JWTManagementEndpoint}.
 * @author K. Benedyczak
 */
@Component
public class JWTManagementEndpointFactory implements EndpointFactory
{
	public static final String NAME = "JWTMan";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "A RESTful endpoint allowing for management of tokens (issuing, refreshing) "
					+ "which are subsequently used to authenticate to Unity by "
					+ "non-browser clients in a simple way.", 
			Collections.singleton(JAXRSAuthentication.NAME),
			Collections.singletonMap("", "The REST management base path"));
	
	@Autowired
	private UnityMessageSource msg;
	@Autowired
	private SessionManagement sessionMan;
	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	private PKIManagement pkiManagement;
	@Autowired
	private NetworkServer server;
	@Autowired
	private IdentitiesManagement identitiesMan;
	@Autowired
	private AuthenticationProcessor authenticationProcessor;
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new JWTManagementEndpoint(msg, sessionMan, authenticationProcessor, 
				TYPE, "", tokensMan, pkiManagement, server, identitiesMan);
	}

}
