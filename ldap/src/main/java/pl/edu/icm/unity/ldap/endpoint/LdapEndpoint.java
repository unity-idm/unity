/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.endpoint.AbstractEndpoint;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * LDAP endpoint exposes a stripped LDAP protocol interface to Unity's database.
 * 
 * @author K. Benedyczak
 */
public class LdapEndpoint extends AbstractEndpoint
{
	private LdapServerProperties configuration;
	
	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(serializedState));
			configuration = new LdapServerProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the LDAP"
					+ " endpoint's configuration", e);
		}
	}
	
	@Override
	public void start() throws EngineException
	{
		// TODO runtime initialization and startup. The state (authenticators, description and configuration)
		// is all set up here.
		
	}
	
	@Override
	public void destroy() throws EngineException
	{
		//TODO runtime should be stopped here (e.g. server shutdown)
	}


	@Override
	public void updateAuthenticationOptions(List<AuthenticationOption> authenticationOptions)
			throws UnsupportedOperationException
	{
		// TODO if possible the list of deployed authenticators should be updated 
	}
}
