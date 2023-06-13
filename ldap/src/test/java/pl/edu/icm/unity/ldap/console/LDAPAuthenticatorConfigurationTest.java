/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;
import static pl.edu.icm.unity.ldap.client.config.LdapProperties.META;
import static pl.edu.icm.unity.ldap.client.config.LdapProperties.PREFIX;

import java.security.cert.X509Certificate;
import java.util.Properties;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.engine.translation.in.action.IncludeInputProfileActionFactory;
import pl.edu.icm.unity.ldap.client.LdapCertVerificator;
import pl.edu.icm.unity.ldap.client.LdapPasswordVerificator;
import pl.edu.icm.unity.ldap.client.config.LdapConfiguration;
import pl.edu.icm.unity.webui.authn.extensions.PasswordRetrievalProperties;

public class LDAPAuthenticatorConfigurationTest
{
	private PKIManagement pkiMan = mock(PKIManagement.class);
	private MessageSource msg = mock(MessageSource.class);

	private static final TranslationProfile DEF_PROFILE = new TranslationProfile("Embedded", "", ProfileType.INPUT,
			Lists.newArrayList(new TranslationRule("true",
					new IncludeInputProfileActionFactory().getInstance("sys:ldap"))));

	@Test
	public void serializationIsIdempotentForMinimalConfigUsedTemplateBasedDNResolving() throws EngineException
	{
		NamedCertificate nc = new NamedCertificate("foo", mock(X509Certificate.class));
		when(pkiMan.getCertificate(any())).thenReturn(nc);
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithoutDefaults(PREFIX, META)
				.update("userDNTemplate", "cn={USERNAME}")
				.get();
		sourceCfg.put("retrieval.password.enableAssociation", "false");
		
		Properties result = parsePropertiesAndBack(sourceCfg, LdapPasswordVerificator.NAME);

		createComparator(PREFIX, META)
				.ignoringSuperflous("embeddedTranslationProfile", "additionalSearch.1.scope", "validUsersFilter")
				.checkMatching(result, sourceCfg);
		String defaultProfileJson = DEF_PROFILE.toJsonObject().toString();
		assertThat(result.get(PREFIX + "embeddedTranslationProfile")).isEqualTo(defaultProfileJson);
	}
	
	@Test
	public void serializationIsIdempotentForMinimalCertConfigUsedTemplateBasedDNResolving() throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithoutDefaults(PREFIX, META)
				.update("userDNTemplate", "cn={USERNAME}").get();
		sourceCfg.put("retrieval.tls.enableAssociation", "false");
		
		Properties result = parsePropertiesAndBack(sourceCfg, LdapCertVerificator.NAME);

		createComparator(PREFIX, META)
				.ignoringSuperflous("embeddedTranslationProfile", "additionalSearch.1.scope")
				.checkMatching(result, sourceCfg);
		String defaultProfileJson = DEF_PROFILE.toJsonObject().toString();
		assertThat(result.get(PREFIX + "embeddedTranslationProfile")).isEqualTo(defaultProfileJson);
	}

	@Test
	public void serializationIsIdempotentForMinimalExplicitDefaultsConfigUsedTemplateBasedDNResolving() throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithDefaults(PREFIX, META)
				.update("userDNTemplate", "cn={USERNAME}").get();
		sourceCfg.put("retrieval.password.enableAssociation", "false");
		
		Properties result = parsePropertiesAndBack(sourceCfg, LdapPasswordVerificator.NAME);

		createComparator(PREFIX, META).ignoringMissing("translationProfile")
				.ignoringSuperflous("embeddedTranslationProfile", "additionalSearch.1.scope")
				.checkMatching(result, sourceCfg);
		String defaultProfileJson = DEF_PROFILE.toJsonObject().toString();
		assertThat(result.get(PREFIX + "embeddedTranslationProfile")).isEqualTo(defaultProfileJson);
	}

	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfigUsedTemplateBasedDNResolving() throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, META)
				.update("authenticateOnly", "false")
				.update("embeddedTranslationProfile", DEF_PROFILE.toJsonObject().toString())
				.update("userDNTemplate", "cn={USERNAME}")
				.update("bindAs", "system")
				.remove("userDNSearchKey")
				.update("ports.1", "999").get();
		Properties sourceCfgRet = ConfigurationGenerator.generateCompleteWithNonDefaults(
				PasswordRetrievalProperties.P, PasswordRetrievalProperties.defaults).get();
		sourceCfg.putAll(sourceCfgRet);
		
		Properties result = parsePropertiesAndBack(sourceCfg, LdapPasswordVerificator.NAME);

		createComparator(PREFIX, META)
				.ignoringMissing("translationProfile").checkMatching(result, sourceCfg);
	}
	
	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfigUsedLdapSearchDNResolving() throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, META)
				.update("authenticateOnly", "false")
				.update("embeddedTranslationProfile", DEF_PROFILE.toJsonObject().toString())
				.update("bindAs", "system")
				.update("ports.1", "999")
				.update("userDNSearchKey", "1")
				.remove("userDNTemplate")
				.get();
		Properties sourceCfgRet = ConfigurationGenerator.generateCompleteWithNonDefaults(
				PasswordRetrievalProperties.P, PasswordRetrievalProperties.defaults).get();
		sourceCfg.putAll(sourceCfgRet);
		
		Properties result = parsePropertiesAndBack(sourceCfg, LdapPasswordVerificator.NAME);
		
		createComparator(PREFIX, META)
				.withAlias(PREFIX + "additionalSearch.searchUserDN.filter", PREFIX + "additionalSearch.1.filter")
				.withAlias(PREFIX + "additionalSearch.searchUserDN.baseName", PREFIX + "additionalSearch.1.baseName")
				.withAlias(PREFIX + "additionalSearch.searchUserDN.scope", PREFIX + "additionalSearch.1.scope")
				.ignoringMissing("translationProfile", "additionalSearch.1.selectedAttributes")
				.withExpectedChange("userDNSearchKey", "searchUserDN")
				.checkMatching(result, sourceCfg);
	}
	
	private Properties parsePropertiesAndBack(Properties sourceCfg, String verificatorType)
	{
		LdapConfiguration processor = new LdapConfiguration();
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), verificatorType,
				msg);
		String converted = processor.toProperties(verificatorType, msg);

		return ConfigurationComparator.fromString(converted, PREFIX).get();
	}
}
