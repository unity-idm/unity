/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Factory for {@link RESTAdminEndpoint}.
 * @author K. Benedyczak
 */
@Component
public class RESTAdminEndpointFactory implements EndpointFactory
{
	public static final String NAME = "RESTAdmin";
	public static final String V1_PATH = "/v1";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "A RESTful endpoint exposing Unity management API.", 
			Collections.singleton(JAXRSAuthentication.NAME),
			Collections.singletonMap(V1_PATH, "The REST management base path"));
	
	@Autowired
	private UnityMessageSource msg;
	@Autowired
	private SessionManagement sessionMan;
	@Autowired
	private IdentitiesManagement identitiesMan;
	@Autowired
	private GroupsManagement groupsMan;
	@Autowired
	private AttributesManagement attributesMan;
	@Autowired
	private AuthenticationProcessor authnProcessor;
	@Autowired
	private NetworkServer server;
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new RESTAdminEndpoint(msg, sessionMan, server, "", identitiesMan, groupsMan, attributesMan,
				authnProcessor);
	}

}
