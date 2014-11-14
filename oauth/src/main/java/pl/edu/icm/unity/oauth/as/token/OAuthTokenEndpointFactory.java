/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Factory for {@link OAuthTokenEndpoint}.
 * @author K. Benedyczak
 */
@Component
public class OAuthTokenEndpointFactory implements EndpointFactory
{
	public static final String NAME = "OAuth2Token";
	public static final String PATH = "";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "A RESTful endpoint exposing OAuth and OIDC related, client-focused endpoints.", 
			Collections.singleton(JAXRSAuthentication.NAME),
			Collections.singletonMap(PATH, "The OAuth base path"));
	
	@Autowired
	private UnityMessageSource msg;
	@Autowired
	private SessionManagement sessionMan;
	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	private PKIManagement pkiMan;
	@Autowired
	private OAuthEndpointsCoordinator coordinator;
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new OAuthTokenEndpoint(msg, sessionMan, TYPE, PATH, tokensMan, pkiMan, coordinator);
	}

}
