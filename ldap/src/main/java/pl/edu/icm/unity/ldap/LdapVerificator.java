/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import eu.unicore.security.AuthenticationException;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;

/**
 * Supports {@link PasswordExchange} and verifies the password and username against a configured LDAP 
 * server. Access to remote attributes and groups is also provided.
 * 
 * @author K. Benedyczak
 */
public class LdapVerificator extends AbstractRemoteVerificator implements PasswordExchange
{
	private LdapProperties ldapProperties;
	private LdapClient client;
	private LdapClientConfiguration clientConfiguration;
	private PKIManagement pkiManagement;
	private String translationProfile;
	
	public LdapVerificator(String name, String description, 
			TranslationProfileManagement profileManagement, AttributesManagement attrMan,
			PKIManagement pkiManagement)
	{
		super(name, description, PasswordExchange.ID, profileManagement, attrMan);
		this.client = new LdapClient(name);
		this.pkiManagement = pkiManagement;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		StringWriter sbw = new StringWriter();
		try
		{
			ldapProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize LDAP verificator configuration", e);
		}
		return sbw.toString();
	}

	@Override
	public void setSerializedConfiguration(String source) throws InternalException
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			ldapProperties = new LdapProperties(properties);
			translationProfile = ldapProperties.getValue(LdapProperties.TRANSLATION_PROFILE);
			clientConfiguration = new LdapClientConfiguration(ldapProperties, pkiManagement);
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the LDAP verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the LDAP verificator(?)", e);
		}
	}

	@Override
	public AuthenticationResult checkPassword(String username, String password)
			throws EngineException
	{
		RemotelyAuthenticatedInput input;
		try
		{
			input = client.bindAndSearch(username, password, clientConfiguration);
		} catch (LdapAuthenticationException e)
		{
			return new AuthenticationResult(Status.deny, null, null);
		} catch (Exception e)
		{
			throw new AuthenticationException("Problem when authenticating against the LDAP server", e);
		}

		return getResult(input, translationProfile);
	}

	@Override
	public CredentialReset getCredentialResetBackend()
	{
		return new NoCredentialResetImpl();
	}
}
