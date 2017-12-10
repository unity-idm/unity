/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPI;
import pl.edu.icm.unity.ldap.client.LdapProperties.BindAs;


/**
 * Imports user from LDAP. Shares similar code and configuration with the LDAP verificator, however
 * does not verify user's password, and bindAs=user is not allowed.
 * @author K. Benedyczak
 */
public class LdapImporter implements UserImportSPI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, LdapImporter.class);
	
	private LdapClientConfiguration clientConfiguration;

	private LdapClient client;

	public LdapImporter(PKIManagement pkiManagement, Properties properties, String idpName)
	{
		LdapProperties ldapProperties = new LdapProperties(properties);
		clientConfiguration = new LdapClientConfiguration(ldapProperties, pkiManagement);
		client = new LdapClient(idpName);
		if (clientConfiguration.getBindAs() == BindAs.user)
		{
			throw new ConfigurationException("LDAP import can be only performed "
					+ "when the LDAP subsystem is configured to bind with a system credential");
		}
	}

	@Override
	public RemotelyAuthenticatedInput importUser(String identity, String type)
	{
		try 
		{
			return client.search(identity, clientConfiguration);
		} catch (Exception e) 
		{
			log.warn("LDAP import failed, skipping", e);
			return null;
		}
	}
}
