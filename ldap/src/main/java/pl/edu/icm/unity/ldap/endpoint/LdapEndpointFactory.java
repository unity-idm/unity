/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.credential.PasswordVerificator;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Creates instances of {@link LdapEndpoint}s.
 */
@Component
public class LdapEndpointFactory implements EndpointFactory
{
    private static final Logger LOG = Log.getLogger(Log.U_SERVER_LDAP_ENDPOINT, LdapServerProperties.class);
	public static final String NAME = "LDAPServer";

	private EndpointTypeDescription endpointDescription;

	private NetworkServer server;

	private PasswordVerificator credentialVerificator;

	private SessionManagement sessionMan;

	private AttributesManagement attributesMan;

	private IdentitiesManagement identitiesMan;

	private UnityServerConfiguration mainConfig;

	private UserMapper userMapper;

        private LdapEndpoint _instance;
        
	@Autowired
	public LdapEndpointFactory(NetworkServer server, IdentityResolver identityResolver,
			PasswordVerificatorFactory pwf, SessionManagement sessionMan,
			AttributesManagement attributesMan, IdentitiesManagement identitiesMan,
			UnityServerConfiguration mainConfig, UserMapper userMapper)
	{
		this.server = server;
		this.mainConfig = mainConfig;
		this.userMapper = userMapper;

		// now now, this is not very nice
		this.credentialVerificator = (PasswordVerificator) pwf.newInstance();
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
            LOG.info("LdapEndpointFactory.newInstance()");
            //if (_instance == null) {
                _instance = new LdapEndpoint(server, sessionMan, attributesMan,
				identitiesMan, mainConfig, userMapper);
            //}
            //TODO: Do we need to handle update?
            return _instance;
	}
}
