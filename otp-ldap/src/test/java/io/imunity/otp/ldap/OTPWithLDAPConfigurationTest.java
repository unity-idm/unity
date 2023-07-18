/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.ldap;

import static io.imunity.otp.ldap.OTPWithLDAPProperties.PREFIX;
import static io.imunity.otp.ldap.OTPWithLDAPProperties.defaults;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;

import java.security.cert.X509Certificate;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.imunity.otp.OTPRetrievalProperties;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;

public class OTPWithLDAPConfigurationTest
{
	private PKIManagement pkiMan = mock(PKIManagement.class);
	private MessageSource msg = mock(MessageSource.class);

	@Test
	public void serializationIsIdempotentForMinimalConfigUsedTemplateBasedDNResolving() throws EngineException
	{
		NamedCertificate nc = new NamedCertificate("foo", mock(X509Certificate.class));
		when(pkiMan.getCertificate(any())).thenReturn(nc);
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithoutDefaults(PREFIX, defaults)
				.update("userDNTemplate", "cn={USERNAME}").get();

		Properties result = parsePropertiesAndBack(sourceCfg);

		createComparator(PREFIX, defaults).ignoringSuperflous("validUsersFilter").checkMatching(result, sourceCfg);
	}

	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfigUsedTemplateBasedDNResolving()
			throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, defaults)
				.update("userDNTemplate", "cn={USERNAME}").update("otpHashFunction", "SHA1").update("ports.1", "999")
				.get();
		Properties sourceCfgRet = ConfigurationGenerator
				.generateCompleteWithNonDefaults(OTPRetrievalProperties.P, OTPRetrievalProperties.defaults).get();
		sourceCfg.putAll(sourceCfgRet);

		Properties result = parsePropertiesAndBack(sourceCfg);

		createComparator(PREFIX, defaults).ignoringMissing("searchFilter", "searchBaseName").checkMatching(result,
				sourceCfg);
	}

	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfigUsedLdapSearchDNResolving() throws EngineException
	{
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, defaults)
				.remove("userDNTemplate").update("otpHashFunction", "SHA1").update("ports.1", "999").get();
		Properties sourceCfgRet = ConfigurationGenerator
				.generateCompleteWithNonDefaults(OTPRetrievalProperties.P, OTPRetrievalProperties.defaults).get();
		sourceCfg.putAll(sourceCfgRet);

		Properties result = parsePropertiesAndBack(sourceCfg);

		createComparator(PREFIX, defaults).checkMatching(result, sourceCfg);
	}

	private Properties parsePropertiesAndBack(Properties sourceCfg)
	{
		OTPWithLDAPConfiguration processor = new OTPWithLDAPConfiguration();
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg);
		String converted = processor.toProperties(msg);

		return ConfigurationComparator.fromString(converted, PREFIX).get();
	}
}
