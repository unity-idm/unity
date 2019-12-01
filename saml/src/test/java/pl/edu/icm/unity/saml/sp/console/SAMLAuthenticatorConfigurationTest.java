/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.console;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.META;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.P;

import java.security.cert.X509Certificate;
import java.util.Properties;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.engine.translation.in.action.IncludeInputProfileActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class SAMLAuthenticatorConfigurationTest
{
	private PKIManagement pkiMan = mock(PKIManagement.class);
	private UnityMessageSource msg = mock(UnityMessageSource.class);
	private URIAccessService uriAccessSrv = mock(URIAccessService.class);
	private FileStorageService fileStorageSrv = mock(FileStorageService.class);
	private static final TranslationProfile DEF_PROFILE = new TranslationProfile("Embedded", "", ProfileType.INPUT, 
			Lists.newArrayList(new TranslationRule("true", 
					new IncludeInputProfileActionFactory().getInstance("fooo"))));	

	
	@Test
	public void serializationIsIdempotentForMinimalConfig() throws EngineException
	{
		NamedCertificate nc = new NamedCertificate("foo", mock(X509Certificate.class));
		when(pkiMan.getCertificate(any())).thenReturn(nc);
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithoutDefaults(P, META)
				.update("metadataPath", "foo")
				.get();
		
		SAMLAuthneticatorConfiguration processor = new SAMLAuthneticatorConfiguration();
		
		processor.fromProperties(pkiMan, uriAccessSrv, msg, ConfigurationComparator.getAsString(sourceCfg));
		String converted = processor.toProperties(pkiMan, fileStorageSrv, msg, "authName");
		
		Properties result = ConfigurationComparator.fromString(converted, P).get();
		
		createComparator(P, META)
			.ignoringSuperflous("metadataSource.1.refreshInterval", 
				"metadataSource.1.signaturVerification",
				"remoteIdp.1.binding",
				"remoteIdp.1.signRequest")
			.checkMatching(result, sourceCfg);
	}
	
	@Test
	public void serializationIsIdempotentForMinimalExplicitDefaultsConfig() throws EngineException
	{
		NamedCertificate nc = new NamedCertificate("foo", mock(X509Certificate.class));
		when(pkiMan.getCertificate(any())).thenReturn(nc);
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithDefaults(P, META)
				.update("metadataPath", "foo")
				.get();
		
		SAMLAuthneticatorConfiguration processor = new SAMLAuthneticatorConfiguration();
		
		processor.fromProperties(pkiMan, uriAccessSrv, msg, ConfigurationComparator.getAsString(sourceCfg));
		String converted = processor.toProperties(pkiMan, fileStorageSrv, msg, "authName");
		
		Properties result = ConfigurationComparator.fromString(converted, P).get();
		
		createComparator(P, META)
			.ignoringMissing("remoteIdp.1.translationProfile")
			.checkMatching(result, sourceCfg);
	}
	
	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfig() throws EngineException
	{
		NamedCertificate nc = new NamedCertificate("foo", mock(X509Certificate.class));
		when(pkiMan.getCertificate(any())).thenReturn(nc);
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(P, META)
				.remove("jwt.")
				.update("remoteIdp.1.signRequest", "false")
				.update("metadataSource.1.perMetadataEmbeddedTranslationProfile", DEF_PROFILE.toJsonObject().toString())
				.update("remoteIdp.1.embeddedTranslationProfile", DEF_PROFILE.toJsonObject().toString())
				.get();
		
		SAMLAuthneticatorConfiguration processor = new SAMLAuthneticatorConfiguration();
		
		processor.fromProperties(pkiMan, uriAccessSrv, msg, ConfigurationComparator.getAsString(sourceCfg));
		String converted = processor.toProperties(pkiMan, fileStorageSrv, msg, "authName");
		
		Properties result = ConfigurationComparator.fromString(converted, P).get();
		
		createComparator(P, META)
			.withAlias("unity.saml.requester.remoteIdp.1.certificates.2", "unity.saml.requester.remoteIdp.1.certificates.1")
			.withAlias("unity.saml.requester.remoteIdp.1.certificates.1", "unity.saml.requester.remoteIdp.1.certificate")
			.ignoringMissing("metadataSource.1.perMetadataTranslationProfile",
					"remoteIdp.1.translationProfile")
			.checkMatching(result, sourceCfg);
	}
}