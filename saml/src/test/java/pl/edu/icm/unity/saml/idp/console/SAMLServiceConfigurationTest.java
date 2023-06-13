/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.console;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.P;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.defaults;

public class SAMLServiceConfigurationTest
{
	private PKIManagement pkiMan = mock(PKIManagement.class);
	private MessageSource msg = mock(MessageSource.class);
	private URIAccessService uriAccessSrv = mock(URIAccessService.class);
	private ImageAccessService imageAccessSrv = mock(ImageAccessService.class);
	private FileStorageService fileStorageSrv = mock(FileStorageService.class);
	
	@Test
	public void serializationIsIdempotentForMinimalConfig() throws Exception
	{
		when(pkiMan.getCredentialNames()).thenReturn(Sets.newHashSet("foo"));
		NamedCertificate nc = new NamedCertificate("foo", mock(X509Certificate.class));
		when(pkiMan.getCertificate(any())).thenReturn(nc);
		TranslationProfile tp = new TranslationProfile("name", "description", ProfileType.OUTPUT, Collections.emptyList());
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithoutDefaults(P, defaults)
				.update("embeddedTranslationProfile", tp.toJsonObject().toString())
				.remove("acceptedSP.1.encryptAssertion")
				.update("defaultGroup", "/foo1")
				.update("groupMapping.1.mappingGroup", "/foo2")
				.get();
		SAMLServiceConfiguration processor = new SAMLServiceConfiguration(msg, Collections.emptyList());
		
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, uriAccessSrv, imageAccessSrv, pkiMan, Lists.newArrayList());
		String converted = processor.toProperties(pkiMan, msg, fileStorageSrv, "name");
		
		Properties result = ConfigurationComparator.fromString(converted, P).get();
		
		createComparator(P, defaults)
				.ignoringMissing("translationProfile", "")
				.ignoringSuperflous("acceptedSPMetadataSource.1.refreshInterval", 
						"acceptedSPMetadataSource.1.signaturVerification")
				.checkMatching(result, sourceCfg);
	}
	
	@Test
	public void serializationIsIdempotentForMinimalExplicitDefaultsConfig() throws Exception
	{
		when(pkiMan.getCredentialNames()).thenReturn(Sets.newHashSet("foo"));
		NamedCertificate nc = new NamedCertificate("foo", mock(X509Certificate.class));
		when(pkiMan.getCertificate(any())).thenReturn(nc);
		TranslationProfile tp = new TranslationProfile("name", "description", ProfileType.OUTPUT, Collections.emptyList());
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithDefaults(P, defaults)
				.update("embeddedTranslationProfile", tp.toJsonObject().toString())
				.remove("acceptedSP.1.encryptAssertion")
				.update("defaultGroup", "/foo1")
				.update("groupMapping.1.mappingGroup", "/foo2")
				.get();
		SAMLServiceConfiguration processor = new SAMLServiceConfiguration(msg, Collections.emptyList());
		
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, uriAccessSrv, 
				imageAccessSrv, pkiMan, Lists.newArrayList());
		String converted = processor.toProperties(pkiMan, msg, fileStorageSrv, "name");
		
		Properties result = ConfigurationComparator.fromString(converted, P).get();
		
		createComparator(P, defaults)
				.ignoringMissing("translationProfile", "")
				.checkMatching(result, sourceCfg);
	}
	
	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfig() throws Exception
	{
		when(pkiMan.getCredentialNames()).thenReturn(Sets.newHashSet("foo"));
		NamedCertificate nc = new NamedCertificate("foo", mock(X509Certificate.class));
		when(pkiMan.getCertificate(any())).thenReturn(nc);
		when(imageAccessSrv.getEditableImageResourceFromUri(eq("foo"), any())).thenReturn(Optional.of(new LocalOrRemoteResource(null, "foo")));
		TranslationProfile tp = new TranslationProfile("name", "description", ProfileType.OUTPUT, Collections.emptyList());
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(P, defaults)
				.update("embeddedTranslationProfile", tp.toJsonObject().toString())
				.update("acceptedSP.1.returnURLs.1", "[0]https://foo")
				.remove("acceptedSP.1.dn")
				.update("defaultGroup", "/foo1")
				.update("groupMapping.1.mappingGroup", "/foo2")
				.update("metadataSource", "src/test/resources/metadata.switchaai.xml")
				.update("policyAgreements.1.policyDocuments", "1")
				.update("policyAgreements.1.policyAgreementPresentationType", PolicyAgreementPresentationType.CHECKBOX_NOTSELECTED.toString())
				.get();
		SAMLServiceConfiguration processor = new SAMLServiceConfiguration(msg, Collections.emptyList());
		
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, uriAccessSrv, 
				imageAccessSrv, pkiMan, Lists.newArrayList());
		String converted = processor.toProperties(pkiMan, msg, fileStorageSrv, "name");
		
		Properties result = ConfigurationComparator.fromString(converted, P).get();
		
		createComparator(P, defaults)
				.ignoringMissing("translationProfile", "metadataSource")
				.withAlias("unity.saml.acceptedSP.1.certificates.2", "unity.saml.acceptedSP.1.certificates.1")
				.withAlias("unity.saml.acceptedSP.1.certificates.1", "unity.saml.acceptedSP.1.certificate")
				.withAlias("unity.saml.acceptedSP.1.returnURLs.2", "unity.saml.acceptedSP.1.returnURLs.1")
				.checkMatching(result, sourceCfg);
	}
}
