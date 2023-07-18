/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.saml.sp.config.TrustedIdPKey.metadataEntity;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.MetadataSignatureValidation;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;

public class TestSpCfgFromMeta extends DBIntegrationTestBase
{
	@Autowired
	private SPRemoteMetaManager.Factory spRemoteMetaManagerFactory;

	@Autowired
	private RemoteMetadataService metadataService;
	
	@Autowired
	@Qualifier("insecure")
	private  PKIManagement pkiManagement;
	
	@BeforeEach
	public void reset()
	{
		metadataService.reset();
	}
	
	@Test
	public void testConfigureSPFromMetadata() throws Exception
	{
		X509Certificate cert = CertificateUtils.loadCertificate(new ByteArrayInputStream(CERT.getBytes()), Encoding.PEM);
		pkiManagement.addVolatileCertificate("issuerCert", cert);

		TranslationProfile translationProfile1 = mock(TranslationProfile.class);
		TranslationProfile translationProfile2 = mock(TranslationProfile.class);
		SAMLSPConfiguration configuration = SAMLSPConfiguration.builder()
				.withRequesterCredential(pkiManagement.getCredential("MAIN"))
				.withRequesterSamlId("me")
				.withPublishMetadata(false)
				.withTrustedMetadataSources(List.of(
						RemoteMetadataSource.builder()
							.withUrl("file:src/test/resources/metadata.switchaai.xml")
							.withRegistrationForm("metaRegForm")
							.withTranslationProfile(translationProfile1)
							.withSignatureValidation(MetadataSignatureValidation.require)
							.withIssuerCertificate("issuerCert")
							.withRefreshInterval(Duration.ofHours(1))
							.build()))
				.withIndividualTrustedIdPs(new TrustedIdPs(List.of(
						TrustedIdPConfiguration.builder()
							.withBinding(Binding.HTTP_POST)
							.withIdpEndpointURL("https://aai.unifr.ch/idp/profile/SAML2/Redirect/SSO")
							.withGroupMembershipAttribute("memberOf")
							.withSamlId("https://aai.unifr.ch/idp/shibboleth")
							.withLogoURI(new I18nString("http://example.com"))
							.withName(new I18nString("Name"))
							.withRegistrationForm("regForm")
							.withRequestedNameFormat("foo")
							.withSignRequest(true)
							.withTranslationProfile(translationProfile2)
							.withCertificateNames(Set.of("MAIN"))
							.withKey(new TrustedIdPKey("idp1"))
							.build()
						)))
				.build();
		
		SPRemoteMetaManager manager = spRemoteMetaManagerFactory.getInstance();
		manager.setBaseConfiguration(configuration);
		
		Awaitility.await()
			.atMost(Durations.FIVE_SECONDS)
			.until(() -> isRemoteMetadataLoaded(manager, metadataEntity("https://aai-login.fh-htwchur.ch/idp/shibboleth", 1)));

		TrustedIdPs ret = manager.getTrustedIdPs();
		TrustedIdPConfiguration trustedIdP1 = ret.get(new TrustedIdPKey("idp1"));
		assertThat(trustedIdP1.idpEndpointURL).isEqualTo("https://aai.unifr.ch/idp/profile/SAML2/Redirect/SSO");
		assertThat(trustedIdP1.binding).isEqualTo(Binding.HTTP_POST);
		assertThat(trustedIdP1.certificateNames).containsExactly("MAIN");
		assertThat(trustedIdP1.groupMembershipAttribute).isEqualTo("memberOf");
		assertThat(trustedIdP1.samlId).isEqualTo("https://aai.unifr.ch/idp/shibboleth");
		assertThat(trustedIdP1.logoURI.getDefaultValue()).isEqualTo("http://example.com");
		assertThat(trustedIdP1.name.getDefaultValue()).isEqualTo("Name");
		assertThat(trustedIdP1.registrationForm).isEqualTo("regForm");
		assertThat(trustedIdP1.requestedNameFormat).isEqualTo("foo");
		assertThat(trustedIdP1.signRequest).isEqualTo(true);
		assertThat(trustedIdP1.translationProfile).isEqualTo(translationProfile2);

		
		TrustedIdPConfiguration trustedIdP2 = ret.get(metadataEntity("https://aai-login.fh-htwchur.ch/idp/shibboleth", 1));
		assertThat(trustedIdP2.idpEndpointURL).isEqualTo("https://aai-login.fh-htwchur.ch/idp/profile/SAML2/Redirect/SSO");
		assertThat(trustedIdP2.binding).isEqualTo(Binding.HTTP_REDIRECT);
		assertThat(trustedIdP2.certificateNames).allMatch(certName -> certName.contains("_SP_METADATA_CERT_"));
		assertThat(pkiManagement.getCertificate(trustedIdP2.certificateNames.iterator().next())).isNotNull();
		assertThat(trustedIdP2.groupMembershipAttribute).isNull();
		assertThat(trustedIdP2.samlId).isEqualTo("https://aai-login.fh-htwchur.ch/idp/shibboleth");
		assertThat(trustedIdP2.logoURI.getDefaultValue()).isEqualTo(LOGO);
		assertThat(trustedIdP2.name.getValue("en")).isEqualTo("HTW Chur - University of Applied Sciences HTW Chur");
		assertThat(trustedIdP2.name.getValue("de")).isEqualTo("HTW Chur - Hochschule für Technik und Wirtschaft");
		assertThat(trustedIdP2.registrationForm).isEqualTo("metaRegForm");
		assertThat(trustedIdP2.requestedNameFormat).isNull();
		assertThat(trustedIdP2.signRequest).isEqualTo(false);
		assertThat(trustedIdP2.translationProfile).isEqualTo(translationProfile1);
	}

	@Test
	public void shouldConfigure2TrustedFederations() throws Exception
	{
		SAMLSPConfiguration configuration = SAMLSPConfiguration.builder()
				.withRequesterCredential(pkiManagement.getCredential("MAIN"))
				.withRequesterSamlId("me")
				.withPublishMetadata(false)
				.withIndividualTrustedIdPs(new TrustedIdPs(List.of()))
				.withTrustedMetadataSources(List.of(
						RemoteMetadataSource.builder()
							.withUrl("file:src/test/resources/metadata.switchaai.xml")
							.withRegistrationForm("metaRegForm")
							.withTranslationProfile(mock(TranslationProfile.class))
							.withRefreshInterval(Duration.ofHours(1))
							.build(),
						RemoteMetadataSource.builder()
							.withUrl("file:src/test/resources/metadata.switchaai-one.xml")
							.withRegistrationForm("metaRegForm")
							.withTranslationProfile(mock(TranslationProfile.class))
							.withRefreshInterval(Duration.ofHours(1))
							.build()))
				.build();
		SPRemoteMetaManager manager = spRemoteMetaManagerFactory.getInstance();
		manager.setBaseConfiguration(configuration);
		
		Awaitility.await()
			.atMost(Durations.FIVE_SECONDS)
			.until(() -> isRemoteMetadataLoaded(manager, metadataEntity("https://aai-login.fh-htwchur.ch/idp/shibboleth", 1)) &&
					isRemoteMetadataLoaded(manager, metadataEntity("https://fake.idp.eu", 1)));

		TrustedIdPs ret = manager.getTrustedIdPs();
		TrustedIdPConfiguration trustedIdP1 = ret.get(metadataEntity("https://aai-login.fh-htwchur.ch/idp/shibboleth", 1));
		assertEquals("HTW Chur - Hochschule für Technik und Wirtschaft", trustedIdP1.name.getValue("de"));
		TrustedIdPConfiguration trustedIdP2 = ret.get(metadataEntity("https://fake.idp.eu", 1));
		assertEquals("Universität Freiburg", trustedIdP2.name.getValue("de"));
	}

	
	@Test
	public void shouldConfigureIdPWith2TrustedCertificates() throws Exception
	{
		SAMLSPConfiguration configuration = SAMLSPConfiguration.builder()
				.withRequesterCredential(pkiManagement.getCredential("MAIN"))
				.withRequesterSamlId("me")
				.withPublishMetadata(false)
				.withTrustedMetadataSources(List.of(
						RemoteMetadataSource.builder()
						.withUrl("file:src/test/resources/DFN-AAI-metadata-2certs.xml")
						.withRegistrationForm("metaRegForm")
						.withTranslationProfile(mock(TranslationProfile.class))
						.withRefreshInterval(Duration.ofHours(1))
						.build()))
				.withIndividualTrustedIdPs(new TrustedIdPs(List.of()))
				.build();
		SPRemoteMetaManager manager = spRemoteMetaManagerFactory.getInstance();
		manager.setBaseConfiguration(configuration);
		
		Awaitility.await()
			.atMost(Durations.FIVE_SECONDS)
			.until(() -> isRemoteMetadataLoaded(manager, metadataEntity("https://idp.scc.kit.edu/idp/shibboleth", 1)));
		TrustedIdPs ret = manager.getTrustedIdPs();
		TrustedIdPConfiguration trustedIdP1 = ret.get(TrustedIdPKey.metadataEntity(
				"https://idp.scc.kit.edu/idp/shibboleth", 1));
		assertThat(trustedIdP1.certificateNames).hasSize(2);
		Iterator<String> certsIt = trustedIdP1.certificateNames.iterator();
		NamedCertificate certificate1 = pkiManagement.getCertificate(certsIt.next());
		assertThat(certificate1).isNotNull();
		NamedCertificate certificate2 = pkiManagement.getCertificate(certsIt.next());
		assertThat(certificate2).isNotNull();
		assertThat(certificate1).isNotEqualTo(certificate2);
	}
	
	@Test
	public void shouldConfigureIdPWith2EndpointsInOneTrustedIdP() throws Exception
	{
		SAMLSPConfiguration configuration = SAMLSPConfiguration.builder()
				.withRequesterCredential(pkiManagement.getCredential("MAIN"))
				.withRequesterSamlId("me")
				.withPublishMetadata(false)
				.withTrustedMetadataSources(List.of(
						RemoteMetadataSource.builder()
						.withUrl("file:src/test/resources/DFN-AAI-metadata-2endpoints.xml")
						.withRegistrationForm("metaRegForm")
						.withTranslationProfile(mock(TranslationProfile.class))
						.withRefreshInterval(Duration.ofHours(1))
						.build()))
				.withIndividualTrustedIdPs(new TrustedIdPs(List.of()))
				.build();
		SPRemoteMetaManager manager = spRemoteMetaManagerFactory.getInstance();
		manager.setBaseConfiguration(configuration);
		
		Awaitility.await()
			.atMost(Durations.FIVE_SECONDS)
			.until(() -> isRemoteMetadataLoaded(manager, metadataEntity("https://idp.scc.kit.edu/idp/shibboleth", 1)));
		TrustedIdPs ret = manager.getTrustedIdPs();
		TrustedIdPConfiguration trustedIdP1 = ret.get(TrustedIdPKey.metadataEntity(
				"https://idp.scc.kit.edu/idp/shibboleth", 1));
		assertThat(trustedIdP1.binding).isEqualTo(Binding.HTTP_REDIRECT);
		TrustedIdPConfiguration trustedIdP2 = ret.get(TrustedIdPKey.metadataEntity(
				"https://idp.scc.kit.edu/idp/shibboleth", 2));
		assertThat(trustedIdP2.binding).isEqualTo(Binding.SOAP);
	}
	
	private boolean isRemoteMetadataLoaded(SPRemoteMetaManager manager, TrustedIdPKey idpKey)
	{
		return manager.getTrustedIdPs().contains(idpKey);
	}
	
	private static final String LOGO = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAAA8CAIAAAB+RarbAAAC0GlDQ1BJQ0NQcm9maWxlAAB4nI2Uz0sUYRjHv7ONGChBYGZ7iKFDSKhMFmVE5a6/2LRtWX+UEsTs7Lu7k7Oz08zsmiIRXjpm0T0qDx76Azx46JSXwsAsAuluUUSCl5LteWfG3RHtxwsz83mfH9/ned/hfYEaWTFNPSQBecOxkn1R6fromFT7ESEcQR3CqFNU24wkEgOgwWOxa2y+h8C/K617+/866tK2mgeE/UDoR5rZKrDvF9kLWWoEELlew4RjOsT3OFue/THnlMfzrn0o2UW8SHxANS0e/5q4Q80paaBGJG7JBmJSAc7rRdXv5yA99cwYHqTvcerpLrN7fBZm0kp3P3Eb8ec06+7hmsTzGa03RtxMz1rG6h32WDihObEhj0Mjhh4f8LnJSMWv+pqi6UST2/p2abBn235LuZwgDhMnxwv9PKaRcjunckPXPBb0qVxX3Od3VjHJ6x6jmDlTd/8X9RZ6hVHoYNBg0NuAhCT6EEUrTFgoIEMejSI0sjI3xiK2Mb5npI5EgCXyr1POuptzG0XK5lkjiMYx01JRkOQP8ld5VX4qz8lfZsPF5qpnxrqpqcsPvpMur7yt63v9njx9lepGyKsjS9Z8ZU12oNNAdxljNlxV4jXY/fhmYJUsUKkVKVdp3K1Ucn02vSOBan/aPYpdml5sqtZaFRdurNQvTe/Yq8KuVbHKqnbOq3HBfCYeFU+KMbFDPAdJvCR2ihfFbpqdFwcqGcOkomHCVbKhUJaBSfKaO/6ZFwvvrLmjoY8ZzNJUiZ//hFXIaDoLHNF/uP9z8HvFo7Ei8MIGDp+u2jaS7h0iNC5Xbc4V4MI3ug/eVm3NdB4OPQEWzqhFq+RLC8IbimZ3HD7pKpiTlpbNOVK7LJ+VInQlMSlmqG0tkqLrkuuyJYvZzCqxdBvszKl2T6WedqXmU7m8Qeev9hGw9bBc/vmsXN56Tj2sAS/138C8/UXN/ALEAAAJI2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNC40LjAiPgogICA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPgogICAgICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIi8+CiAgIDwvcmRmOlJERj4KPC94OnhtcG1ldGE+CiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAKPD94cGFja2V0IGVuZD0idyI/Pqfd9JIAAAAhdEVYdFNvZnR3YXJlAEdyYXBoaWNDb252ZXJ0ZXIgKEludGVsKXeH+hkAAAbvSURBVHic7JdZTFRnFMevwfhQTaPUhgfeWm1kLVBNNFRtDPpgQ2360PIgwYYH1PBCYmIV4vLSRCVYdCxg6bDJrrLJqqCoICiIOyCI7DAMi8CMgDC0v+HD25EhRAtt0+s9ubn57vnOd77z/87ynSv98Z6R9F8b8G+TCljppAJWOqmAlU4qYKWTCljppAJWOqmAlU4qYKWTCljp9P4BLixrTclvSC1o5GGQWdI0OjYhT09MTBiNxr6+vtbW1mfPnvFpraK5ufnp06e8Eaivr9fr9d3d3U+ePGlsbKytrYXPWgQ6Ozvb2tpgog0x+LKGa9euRUREZGRkjI2NITk+Pv4PAnb5LkVyOC05nzE/jpoVG7W6vpdiDtPv3btXVVV1e4ru3LljMBisVQQEBCxdunT//v3btm2ztbW9cOGCr6+vu7s7Yw8Pj3Xr1gUFBS1btuzYsWO7d+92cHDIz8+3s7NjLJZfunSJKQ4oMTExJiZGo9EMDg7Oauvk5KTJZJovYA+fdMktQvoiyvy4R9p5xfX0TwPu7++vrKysek1g7unpsVZRVla2ePFizPX391+9ejVmBQYGpqamAjgqKmrXrl04dtWqVQkJCQcOHECgoaGB4+jq6hLLDx8+3NvbK+946NCh48ePh4eHM+YshH40JCUlEQWzGrBggEdHR+/evWsJ+Pnz59YqysvLbWxsvL29XV1dnZycXr16RWRiIl5NS0tjjMy+ffs2bNiwdu1aYsHHx8fPz0+s5XSOHDkyPDwsawOwTqcDXklJyYkTJ+DEx8cTaEePHm1vb58nWjNgtx/SJNdfgWp+Po/4eEusDBhrHj9+TCSDljfefvToEcwZKvDDokWLsOngwYP29vbCXQ8ePFiyZAk+EfLV1dWSJHl5ee3cuZNBdna2vBx/cpQMWFhcXIxvyWEELl++HBoaCv/s2bM1NTWnTp0aGhpaAMCbf8xc7hm9fJPW/Hz5+2ffJOkHRuRp6lBFRQWAAUCl6ejomJFF4NmzZ4+joyOJumPHDhcXF0IX/t69e52dnbdu3Uqt4hMM27dvJ0WpT56engMDA7IGYiE4OBicISEhN27cSE5ORvjq1aucYEpKysmTJ9GMGaSMZSD8fcBdemNL51BL17D56Rxu1xlMpr98yKGSNhTqWeuzoNE3iZCWmSMjI3JEAEOMhYAlwaGSCzxiI45VCLM7s4znMODdAC+Ilv8RqYDfnUSIimosaEb4WX7KYkQp+ULYi08GJLN8z1uWRnLBUrlMhD1TIokg0kcwLRfKY/YSZki5pc3x2XUJOfU88dn16UWNI687LePIeEpBQ/zUlHk2s7amTm+9MSWN6l1QUCCqGtdsUVERhbelpUX0Z1euXOEupcHiar1+/Trtl26KuLToq5qamrjtKBOUKO4C5GnCuOq4lih4aEYhNYwaDuwXL17QxlFByW1uB5RQ57jzKeksqaurwwzeyPBGAJMwAw3cc0xxHJIzndaa05LTGfPjoFlu0Wm1dRs+WB8Nc3r2k19+Cq+wBsyWGKfVarlLzp07B1pUM05PT+ezsLAQTm5u7sWLF7EAVGFhYRwB4AGMoVyz9+/fRw9nQaMSHR2dmZlZWlqak5ODBhayigtZHBmf58+fz8vLo5ijHCVsTfHn6mJH9mJrtqCkoyErKwslcXFxzDJFh2cGPEfjQcVesTnGfD+LWUdNyJnb1oA5dRThAbyBcx4+fMgbU/AexwwTB9It8saBNFgI4DqCFi/hK8RevjTviAa8jTdYBZOrC2H04EY8xhJikmhCD1NsgTaCVvicdOANn1l0EibcaixBJwN2ZACTyF8AwNiExaLfME3RDAG2wTmWHAwFGwNSwPKWIg8BIH+yCkm6PdFjyQk/H5Lcvn+j01pp0WkB+MONWpjTs2tOB2sqrVXQmXC6BAzxSTfGJyfK582bN2/dukXIccbELXx+lXA1KUBHhTAei4yMRIZgZrZ4iljFEjjkLcFMsSGwYRLSRL5YiwCrWMJZsBH1glnrFnB2wFv8s1Zu1K78Ksb8bNI6fJssd1qdeuOnXyfCFLMfrf/t5+hqaxXAwwhSiLJBqjAgzei3SCFaQpG6ZB1/UcAgPuEjRuwRluQkmUxFISd5M0V6x8bGsgqcqMXhIkspBMQweYty+HyikwFHydlRt97S/xL+7NAZOnqmHp2hq9cod1oTpkkwy7M4fNAw+/VAJSSqxd1ALSUOSSc6JziEOlPcNzhHBDapi3G4V5iIJO4iCYkOsZwowLEI88ZvxDPL+VdFIcVclGuhGQHe8GekzFyA31Ju/rRQveE8Se20lE4qYKWTCljppAJWOqmAlU4qYKWTCljppAJWOqmAlU4qYKXTewf4TwAAAP//AwDTJKa92O1TGAAAAABJRU5ErkJggg==";
	private static final String CERT = "-----BEGIN CERTIFICATE-----\nMIIEJjCCAw6gAwIBAgISSWITCHaaiMetadataSig2014MA0GCSqGSIb3DQEBBQUAMEYxCzAJBgNV"
			+ "BAYTAkNIMQ8wDQYDVQQKEwZTV0lUQ0gxJjAkBgNVBAMTHVNXSVRDSGFhaSBNZXRhZGF0YSBTaWdu"
			+ "aW5nIENBMB4XDTE0MDQwMzA3MDAwMFoXDTE2MDUwMzA2NDQ1OVowQjELMAkGA1UEBhMCQ0gxDzAN"
			+ "BgNVBAoTBlNXSVRDSDEiMCAGA1UEAxMZU1dJVENIYWFpIE1ldGFkYXRhIFNpZ25lcjCCASIwDQYJ"
			+ "KoZIhvcNAQEBBQADggEPADCCAQoCggEBAIJRlIwbBV1lsgsZ+l7N4YRijr2Cm9uy14EXSOBb1KqA"
			+ "fve/20/qFwAsWuWcqkU/v94Q6RN086X6tYdm+jNrqOVUcjVxiyVxieye98Hgyq0d8wYCllVQMdJv"
			+ "Hg7mJz+1mSxCvhFz9pJ3xwjgzTPtNsVmIk3l6ZSAHsN3PDPxxdjDbyqpJbdHAI8S4HW33mDb2BAo"
			+ "/mTrPG2wqY+xo8xf7QXFGyeGvU58fs/jvnD3s+XN4NL3qh4QocK4uiIo2jwsxo5auFIPq8YM0YeL"
			+ "2H2sk5ZO6YQttw6/7+ib/oJquyd1DcqWTUcgZTjqp4PDjJApHC2PnOUmRpD08rzQFPmauwkCAwEA"
			+ "AaOCARAwggEMMA4GA1UdDwEB/wQEAwIHgDAdBgNVHQ4EFgQU2XfrpZMcl7uByGsnJ+L8V8r4Lgsw"
			+ "HwYDVR0jBBgwFoAUkKnCDUaLZTU5RGduPD4q0qEnBbYwTAYDVR0fBEUwQzBBoD+gPYY7aHR0cDov"
			+ "L2NybC5hYWkuc3dpdGNoLmNoL1NXSVRDSGFhaU1ldGFkYXRhU2lnbmluZ0NBMjAxMS5jcmwwVgYI"
			+ "KwYBBQUHAQEESjBIMEYGCCsGAQUFBzAChjpodHRwOi8vY2EuYWFpLnN3aXRjaC5jaC9TV0lUQ0hh"
			+ "YWlNZXRhZGF0YVNpZ25pbmdDQTIwMTEuY3J0MBQGA1UdIAQNMAswCQYHYIV0AQIGBzANBgkqhkiG"
			+ "9w0BAQUFAAOCAQEAjEmjDbK9guQ0uXG3oLy368qCDl4swEZa3uIrmFJ/VJNNPSv2A7kvRpW2Od/p"
			+ "I+VjOJueYfJrSu+zeuB9Epem+2/Pvbjssq4ZJpj+91RUtlmA/MPNqRu0/PxrDBpHZ5HrXqH6NSI1"
			+ "DwZeIW5xEp4m3oYFptb14u9vP9UWUY8OFU2uhJF3FZ9AtzUHWAcIXg0evaw2fId6h+lOYVjl2hG9"
			+ "EP7pnM2z+s8qGPZAz/YjHM7I4/ToPpGvJ/YAnhJZ4MYzflqJxp/Ol1hpfPMeLOBL4G0FPZjrpxHZ"
			+ "56aNfU0OhQHYwveA73avWZ4T2NVo4Jipa5KfQFg5YYxnq6UWMciZAw=="
			+ "\n-----END CERTIFICATE-----";
}
