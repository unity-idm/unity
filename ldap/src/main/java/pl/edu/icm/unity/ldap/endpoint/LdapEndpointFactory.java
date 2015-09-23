/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Creates instances of {@link LdapEndpoint}s.
 * @author K. Benedyczak
 */
@Component
public class LdapEndpointFactory implements EndpointFactory
{
	public static final String NAME = "LDAP";
	
	private EndpointTypeDescription endpointDescription;
	
	public LdapEndpointFactory()
	{
		endpointDescription = new EndpointTypeDescription(
				NAME, 
				"Limited LDAP server interface", 
				Sets.newHashSet(LdapServerAuthentication.NAME), 
				new HashMap<>());
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return endpointDescription;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new LdapEndpoint();
	}
}
