/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Creates instances of {@link LdapEndpoint}s.
 */
@Component
public class LdapEndpointFactory implements EndpointFactory
{
	public static final String NAME = "LDAPServer";

	private EndpointTypeDescription endpointDescription;

	private NetworkServer server;

	private PasswordVerificator credentialVerificator;

	private SessionManagement sessionMan;

	private AttributesManagement attributesMan;

	private EntityManagement identitiesMan;

	private UnityServerConfiguration mainConfig;

	private UserMapper userMapper;

	@Autowired
	public LdapEndpointFactory(NetworkServer server, IdentityResolver identityResolver,
			ObjectFactory<PasswordVerificator> pwf, SessionManagement sessionMan,
			AttributesManagement attributesMan, EntityManagement identitiesMan,
			UnityServerConfiguration mainConfig, UserMapper userMapper)
	{
		this.server = server;
		this.mainConfig = mainConfig;
		this.userMapper = userMapper;

		// now now, this is not very nice
		this.credentialVerificator = pwf.getObject();
		this.credentialVerificator.setIdentityResolver(identityResolver);
		this.credentialVerificator.setCredentialName("Password credential");
		this.sessionMan = sessionMan;
		this.attributesMan = attributesMan;
		this.identitiesMan = identitiesMan;

		Map<String, String> paths = new HashMap<>();
		endpointDescription = new EndpointTypeDescription(NAME,
				"Limited LDAP server interface",
				Collections.singleton(LdapServerAuthentication.NAME), paths);

	}

	@Override
	public EndpointTypeDescription getDescription()
	{
		return endpointDescription;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new LdapEndpoint(server, sessionMan, attributesMan,
				identitiesMan, mainConfig, userMapper);
	}
}
