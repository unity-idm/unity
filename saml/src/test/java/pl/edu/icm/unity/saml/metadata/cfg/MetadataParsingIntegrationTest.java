/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.saml.sp.config.TrustedIdPKey.metadataEntity;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.xmlbeans.XmlException;
import org.junit.jupiter.api.Test;

import eu.unicore.samly2.SAMLUtils;
import eu.unicore.samly2.messages.XMLExpandedMessage;
import eu.unicore.samly2.trust.ResponseTrustCheckResult;
import eu.unicore.samly2.trust.SamlTrustChecker;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.saml.sp.FakeSAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

public class MetadataParsingIntegrationTest
{
	private final TranslationProfile translationProfile1 = mock(TranslationProfile.class);
	
	@Test
	public void shouldTrustResponseSignedByIdpFromParsedMetadata() throws Exception
	{
		PKIManagement pkiManagement = mock(PKIManagement.class);
		when(pkiManagement.getCertificate(any())).thenThrow(IllegalArgumentException.class);
		MetadataToSPConfigConverter converter = new MetadataToSPConfigConverter(pkiManagement , "en");
		EntitiesDescriptorDocument metadata = loadMetadata("src/test/resources/metadata-of-signed-response.xml");
		RemoteMetadataSource metadataSrc = getDummyMetadataSource();
		
		TrustedIdPs trustedIdps = converter.convertToTrustedIdPs(metadata, metadataSrc);
		
		TrustedIdPConfiguration trustedIdP = trustedIdps.get(
				metadataEntity("http://centos6-unity1:8080/simplesaml/saml2/idp/metadata.php", 1));
		SAMLSPConfiguration spConfiguration = createConfig();
		
		SamlTrustChecker checkerForIdP = spConfiguration.getTrustCheckerForIdP(trustedIdP);
		
		ResponseDocument respDoc = ResponseDocument.Factory.parse(
				new File("src/test/resources/responseDocSigned.xml"));
		List<AssertionDocument> authnAssertions = getAuthAssertions(respDoc);
		XMLExpandedMessage response = new XMLExpandedMessage(respDoc, respDoc.getResponse());
		
		ResponseTrustCheckResult responseTrustCheckResult = checkerForIdP.checkTrust(response, respDoc.getResponse());
		Throwable assertionValidationError = catchThrowable(() -> 
			checkerForIdP.checkTrust(authnAssertions.get(0), responseTrustCheckResult));
		
		assertThat(assertionValidationError).isNull();
		assertThat(responseTrustCheckResult.isTrustEstablished()).isTrue();
	}

	private RemoteMetadataSource getDummyMetadataSource()
	{
		return RemoteMetadataSource.builder()
				.withRegistrationForm("regForm")
				.withTranslationProfile(translationProfile1)
				.withUrl("dummy")
				.withRefreshInterval(Duration.ZERO)
				.build();
	}

	private List<AssertionDocument> getAuthAssertions(ResponseDocument respDoc) throws Exception
	{
		return SAMLUtils.extractAllAssertions(
				respDoc.getResponse(), null).stream()
				.filter(a -> a.getAssertion().getAuthnStatementArray().length > 0)
				.collect(Collectors.toList());
	}
	
	private SAMLSPConfiguration createConfig() throws EngineException
	{
		return FakeSAMLSPConfiguration.getFakeBuilder()
				.build();
	}
	
	private EntitiesDescriptorDocument loadMetadata(String path)
	{
		try
		{
			return EntitiesDescriptorDocument.Factory.parse(new File(path));
		} catch (XmlException | IOException e)
		{
			throw new RuntimeException("Can't load test XML", e);
		}
	}
}
