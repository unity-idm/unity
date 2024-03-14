/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;
import pl.edu.icm.unity.engine.api.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResponseProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.ldap.client.config.LdapClientConfiguration;
import pl.edu.icm.unity.ldap.client.config.LdapProperties;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

/**
 * Base for LDAP verificators, responsible for handling configuration (common for both pass and cert verificators).
 */
public abstract class LdapBaseVerificator extends AbstractRemoteVerificator 
{
	protected final LdapClient client;
	private final PKIManagement pkiManagement;
	protected final RemoteAuthnResponseProcessor remoteAuthnProcessor;

	private LdapProperties ldapProperties;
	protected LdapClientConfiguration clientConfiguration;
	protected TranslationProfile translationProfile;

	protected LdapBaseVerificator(String name, String description, 
			RemoteAuthnResultTranslator processor,
			PKIManagement pkiManagement, String exchangeId,
			RemoteAuthnResponseProcessor remoteAuthnProcessor)
	{
		super(name, description, exchangeId, processor);
		this.remoteAuthnProcessor = remoteAuthnProcessor;
		this.client = new LdapClient(name);
		this.pkiManagement = pkiManagement;
	}

	@Override
	public String getSerializedConfiguration()
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
	public void setSerializedConfiguration(String source)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			ldapProperties = new LdapProperties(properties);
			translationProfile = getTranslationProfile(ldapProperties, CommonWebAuthnProperties.TRANSLATION_PROFILE,
					CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE);
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
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}
	
	@Override
	public List<IdPInfo> getIdPs()
	{
		return List.of(IdPInfo.builder().withId(getName()).withDisplayedName(new I18nString(getName())).build());
	}
}
