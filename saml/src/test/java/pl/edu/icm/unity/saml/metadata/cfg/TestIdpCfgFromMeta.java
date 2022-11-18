/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.messages.XMLExpandedMessage;
import eu.unicore.util.configuration.ConfigurationException;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.SamlEntityId;
import pl.edu.icm.unity.saml.idp.TrustedServiceProvider;
import pl.edu.icm.unity.saml.idp.TrustedServiceProviders;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.*;

public class TestIdpCfgFromMeta extends DBIntegrationTestBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, TestIdpCfgFromMeta.class);
	
	@Autowired
	private RemoteMetadataService metadataService;
	
	@Autowired
	@Qualifier("insecure")
	private PKIManagement pkiManagement;
	
	@Autowired
	private MessageSource msg;

	@Before
	public void reset()
	{
		metadataService.reset();
	}

	@Test
	public void testConfigureIdpFromMetadata() throws Exception
	{
		X509Certificate cert = CertificateUtils.loadCertificate(new ByteArrayInputStream(CERT.getBytes()),
				Encoding.PEM);
		pkiManagement.addVolatileCertificate("issuerCert2", cert);

		SAMLIdPConfiguration configuration = SAMLIdPConfiguration.builder()
				.withCredentialName("MAIN")
				.withPublishMetadata(false)
				.withIssuerURI("me")
				.withGroupChooser(Map.of("group", "group"),"group")
				.withTrustedMetadataSources(List.of(
						BaseSamlConfiguration.RemoteMetadataSource.builder()
								.withUrl("file:src/test/resources/metadata.switchaai.xml")
								.withSignatureValidation(SAMLSPProperties.MetadataSignatureValidation.require)
								.withIssuerCertificate("issuerCert2")
								.withTranslationProfile(new TranslationProfile("name", "description", ProfileType.INPUT, List.of()))
								.withRefreshInterval(Duration.of(2, ChronoUnit.SECONDS))
								.build()
				))
				.withTrustedServiceProviders(new TrustedServiceProviders(List.of(
						TrustedServiceProvider.builder()
								.withEntityId("https://support.hes-so.ch/shibboleth")
								.withReturnUrl("URL")
								.withName(new I18nString("Name"))
								.withLogoUri(new I18nString("http://example.com"))
								.withEncrypt(true)
								.withCertificateName("MAIN")
								.withCertificate(cert)
								.build()
				)))
				.build();
		
		RemoteMetaManager manager = new RemoteMetaManager(configuration,
				pkiManagement, metadataService,
				new MetaToIDPConfigConverter(pkiManagement, msg));

		Awaitility.await()
				.timeout(Durations.ONE_MINUTE)
				.pollDelay(Durations.TEN_SECONDS)
				.untilAsserted(() -> assertRemoteMetadataLoaded(manager));
	}
	
	private void assertRemoteMetadataLoaded(RemoteMetaManager manager) throws Exception
	{
		try
		{
			SAMLIdPConfiguration configuration = manager.getSAMLIdPConfiguration();
			TrustedServiceProvider spConfig = configuration.trustedServiceProviders.getSPConfig(new SamlEntityId("https://support.hes-so.ch/shibboleth", null));
			assertEquals("URL", spConfig.returnUrl);
			assertEquals("Name", spConfig.name.getDefaultLocaleValue(msg));
			assertEquals("MAIN", spConfig.certificateName);
			assertEquals("http://example.com", spConfig.logoUri.getDefaultLocaleValue(msg));
			assertTrue(spConfig.encrypt);

			TrustedServiceProvider spConfig2 = configuration.trustedServiceProviders.getSPConfig(new SamlEntityId("https://attribute-viewer.aai.switch.ch/interfederation-test/shibboleth", null));
			assertEquals("https://aai-viewer.switch.ch/interfederation-test/Shibboleth.sso/SAML2/POST",
					spConfig2.returnUrl);
			String certName = spConfig2.certificateNames.iterator().next();
			assertNotNull(pkiManagement.getCertificate(certName));

			assertEquals(LOGO, spConfig2.logoUri.getDefaultLocaleValue(msg));
			assertEquals("AAI Viewer Interfederation Test", spConfig2.name.getValue("en"));
		} catch (Throwable e)
		{
			log.info("Condition not met, {}", e);
			throw e;
		}
	}

	@Test
	public void testConfigureSLOFromSPsMetadata() throws Exception
	{
		SAMLIdPConfiguration configuration = getDFNMetadataBasedConfig();
		
		RemoteMetaManager manager = new RemoteMetaManager(configuration,
				pkiManagement, metadataService,
				new MetaToIDPConfigConverter(pkiManagement, msg));

		Awaitility.await()
				.timeout(Durations.ONE_MINUTE)
				.pollDelay(Durations.TEN_SECONDS)
				.untilAsserted(() -> assertSLOCfgLoaded(manager));
	}

	private void assertSLOCfgLoaded(RemoteMetaManager manager)
	{
		SAMLIdPConfiguration configuration = manager.getSAMLIdPConfiguration();

		TrustedServiceProvider spConfig = configuration.trustedServiceProviders.getSPConfig(new SamlEntityId("http://shibboleth.metapress.com/shibboleth-sp", null));
		assertEquals("https://shibboleth.metapress.com/Shibboleth.sso/SLO/POST",
				spConfig.postLogoutUrl);
		assertEquals("https://shibboleth.metapress.com/Shibboleth.sso/SLO/Redirect",
				spConfig.redirectLogoutUrl);
		assertEquals("https://shibboleth.metapress.com/Shibboleth.sso/SLO/Redirect-RESP",
				spConfig.redirectLogoutRetUrl);
		assertEquals("https://shibboleth.metapress.com/Shibboleth.sso/SLO/SOAP",
				spConfig.soapLogoutUrl);

		TrustedServiceProvider spConfig1 = configuration.trustedServiceProviders.getSPConfig(new SamlEntityId("http://shibboleth.metapress.com/shibboleth-sp-hidden", null));
		if (spConfig1 != null)
			fail("Hidden service is available");
	}
	
	@Test
	public void shouldConfigureMultipleTrustedAssertionConsumers() throws Exception
	{
		SAMLIdPConfiguration configuration = getDFNMetadataBasedConfig();

		RemoteMetaManager manager = new RemoteMetaManager(configuration,
				pkiManagement, metadataService,
				new MetaToIDPConfigConverter(pkiManagement, msg));

		Awaitility.await()
			.atMost(Durations.TEN_SECONDS)
			.untilAsserted(() -> assertEndpointsCfgLoaded(manager));
	}
	
	private void assertEndpointsCfgLoaded(RemoteMetaManager manager)
	{
		assertEndpointCfgLoaded(manager, 8, "https:POST8");
		assertEndpointCfgLoaded(manager, 7, "https:POST7");
		assertEndpointCfgLoaded(manager, 1, "https://shibboleth.metapress.com/Shibboleth.sso/SAML2/POST");

		SAMLIdPConfiguration idpCfg = manager.getSAMLIdPConfiguration();
		AuthnRequestType reqDef = AuthnRequestType.Factory.newInstance();
		reqDef.setIssuer(NameIDType.Factory.newInstance());
		reqDef.getIssuer().setStringValue("http://shibboleth.metapress.com/shibboleth-sp");
		assertThat(idpCfg.getReturnAddressForRequester(reqDef)).isEqualTo("https:POST7");
	}
	
	private void assertEndpointCfgLoaded(RemoteMetaManager manager, Integer index, String expected)
	{
		SAMLIdPConfiguration idpCfg = manager.getSAMLIdPConfiguration();
		AuthnRequestDocument reqDoc = AuthnRequestDocument.Factory.newInstance();
		AuthnRequestType req = reqDoc.addNewAuthnRequest();
		req.setAssertionConsumerServiceIndex(index);
		req.setIssuer(NameIDType.Factory.newInstance());
		req.getIssuer().setStringValue("http://shibboleth.metapress.com/shibboleth-sp");
		assertThat(idpCfg.getReturnAddressForRequester(req)).isEqualTo(expected);
		try
		{
			idpCfg.getAuthnTrustChecker().checkTrust(new XMLExpandedMessage(reqDoc, req), req);
		} catch (SAMLValidationException e)
		{
			fail("Endpoint is not accepted: " + expected);
		}
	}
	
	private SAMLIdPConfiguration getDFNMetadataBasedConfig() throws ConfigurationException, IOException
	{
		return SAMLIdPConfiguration.builder()
				.withCredentialName("MAIN")
				.withPublishMetadata(false)
				.withIssuerURI("me")
				.withGroupChooser(Map.of("group", "group"),"group")
				.withTrustedMetadataSources(List.of(
						BaseSamlConfiguration.RemoteMetadataSource.builder()
								.withTranslationProfile(new TranslationProfile("name", "description", ProfileType.INPUT, List.of()))
								.withRefreshInterval(Duration.of(2, ChronoUnit.SECONDS))
								.withUrl("file:src/test/resources/DFN-AAI-metadata-part.xml")
								.withSignatureValidation(SAMLSPProperties.MetadataSignatureValidation.ignore)
								.build()
				)).build();
	}

	
	
	private static final String LOGO = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAAA8CAIAAAB+RarbAAAC0GlDQ1BJQ0NQcm9maWxlAAB4nI2Uz0sUYRjHv7ONGChBYGZ7iKFDSKhMFmVE5a6/2LRtWX+UEsTs7Lu7k7Oz08zsmiIRXjpm0T0qDx76Azx46JSXwsAsAuluUUSCl5LteWfG3RHtxwsz83mfH9/ned/hfYEaWTFNPSQBecOxkn1R6fromFT7ESEcQR3CqFNU24wkEgOgwWOxa2y+h8C/K617+/866tK2mgeE/UDoR5rZKrDvF9kLWWoEELlew4RjOsT3OFue/THnlMfzrn0o2UW8SHxANS0e/5q4Q80paaBGJG7JBmJSAc7rRdXv5yA99cwYHqTvcerpLrN7fBZm0kp3P3Eb8ec06+7hmsTzGa03RtxMz1rG6h32WDihObEhj0Mjhh4f8LnJSMWv+pqi6UST2/p2abBn235LuZwgDhMnxwv9PKaRcjunckPXPBb0qVxX3Od3VjHJ6x6jmDlTd/8X9RZ6hVHoYNBg0NuAhCT6EEUrTFgoIEMejSI0sjI3xiK2Mb5npI5EgCXyr1POuptzG0XK5lkjiMYx01JRkOQP8ld5VX4qz8lfZsPF5qpnxrqpqcsPvpMur7yt63v9njx9lepGyKsjS9Z8ZU12oNNAdxljNlxV4jXY/fhmYJUsUKkVKVdp3K1Ucn02vSOBan/aPYpdml5sqtZaFRdurNQvTe/Yq8KuVbHKqnbOq3HBfCYeFU+KMbFDPAdJvCR2ihfFbpqdFwcqGcOkomHCVbKhUJaBSfKaO/6ZFwvvrLmjoY8ZzNJUiZ//hFXIaDoLHNF/uP9z8HvFo7Ei8MIGDp+u2jaS7h0iNC5Xbc4V4MI3ug/eVm3NdB4OPQEWzqhFq+RLC8IbimZ3HD7pKpiTlpbNOVK7LJ+VInQlMSlmqG0tkqLrkuuyJYvZzCqxdBvszKl2T6WedqXmU7m8Qeev9hGw9bBc/vmsXN56Tj2sAS/138C8/UXN/ALEAAAJI2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNC40LjAiPgogICA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPgogICAgICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIi8+CiAgIDwvcmRmOlJERj4KPC94OnhtcG1ldGE+CiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAKPD94cGFja2V0IGVuZD0idyI/Pqfd9JIAAAAhdEVYdFNvZnR3YXJlAEdyYXBoaWNDb252ZXJ0ZXIgKEludGVsKXeH+hkAAAaRSURBVHic7JZpUFNXFMf92A+dsR8ECrQ6Fq2tlVVELVQEZLNaoSwV1FEsy6CMuyJhFRAQQiAJEBCIiCigaAUFFARZlE0EAReoiiCgLGUnJIQktyc8GpKXSPnUzjzefzKZ9+4979z7u/fcc+4StMi05P+ewH8tEpjoIoGJLhKY6CKBiS4SmOgigYkuEpjoIoGJLhKY6CKBia5FDMyZnLpT/Nzl+FXbgykObmw719TopLKe3hHoEolE2bkN9NTy8XGe9Me37zUHUQu7P4pt0EgHqo5EnRUy7qd56CkLteXBo0AovJRZc7f4BdbTNzAenfgwKqEkGn6sUkZKGS3pYRSrlMoqjaAX5z94Ie3mz/Z+WkrZbs/LtgdT7VzZ9m7snPxnXN60xIA/LUjOqCoqb5UnnOTykzKrwLMM8NgEz9krfc2WsF0uKY4elxzg535J35q63opa19gJBtl5jUqavuysGmlfj2rffrb8hBclR/xSehrRl6G+JpnRXuUg2lL0Oh8ep/gCw12xHt7XsZ62t31We1iWzizrPYnmTqyVm4MNdtC270uycmaZOcZFJpTMrphAeJ5e9K1RqJFNrL27GNXBnf2LS7LGjyFup7N4U9MSKj3LqFMhufLAg8McGHfpGm8ZYFjdlZuCi8tbBQKhxLS98y/tbRfcz2RjA8MCw+uHvlFpdyfP3f58tU99eSliqaOSkzJD8UZR2kZ0yw4JhdgmGNsxvHxzsE6hSARbxOXxebzpzu4hLbMLjNRyWBRogR8YY2aFZa/U1wcw2RVDIxyJY5hkes4TNV3/ipo3EmCDn6O9z+cpBDa2Y6rrBcgAw/Js+ZU5OsbFWZ8Nu+PpcwN7rn/2/iv9QL/IfGkD4FfXjzhmswOlrUGjnTIf1zMR7QvUXYW94YBxTtaZRsRcfCjfFRxzD7rGJ3i49o6uQY3NwUlXZp1zuPwN8wEz8MBGtnSIXtzugSBmpI/KmdC8FRuDnrd+lLahJtd4WpqipxHSjSLOAErUQAVukpZ5gCFTiIGTFAAHUAuM7ZmSDZcItjS/5Pnr9n7J64bt0ZSIO/IexiemTOzjVHX9ZYAhXcHuuRy/llfUUv30XW1jR/PLnvc9w9NSEQ7q6hleaxJ+8ESmSCiSNA6M8K/QqIjbK20pqAwRMVTRYNscMF8MfJiieIe1tl1QuMOB1AITxzh5YJy4XP6mnTG/n8x80tQJk5f8IAGVVLZt3hmjrOkrAwzKvd8CWcHQhg5faptH6phH/mASbr47obahQ9p1fNojFR2/4oq5fMgXoJrCXNHU+JwRcDJVURlF+sP5Qrp3RNMsgqYI2D+qwHQBwLDDP9nSVxmG6lqIZ65jESl+mHkGllWGIV/rB+KB0Uz5gS+HRyY/9o3CIamsfQvwkEVhcyQ2E5wpM8d4C6cEKGNYy+PG3qD99ujV5TlHxUdErFVorGuhwDM7HK0opAOphaaO8QsB3riDdjTg1sDgRO/AWG//GPbfNzDW+qYP1gK/w4+ftDe2dPOnhThHUCchMw8OT0g33n3wUlnbDytRsEZ7j988tssCXd2AuEPi7v5mxFAV1DFwruY/w5/a4aDoQghI4MG1Q6KGip2V24C9cmbOsE+4gjM8PDq51V4uSytr+ULuhj6cNTO1HIBh2XCDOR1K17WIgliorn+nph+SwWCgBGVUFwMrgHKdEVsP8YYWDoyFtMKkFRp7/3vjMMi0uHZILlBZvPxmvc2WpTAFwFhZUsMlLbhvrDYKrW2QqSucSf5vnmmmDnHSIY2pvun9CoMgmM2ew+lwbRBn8gJXxNJAzWmIoYKa2PID/2tI0xQB3y9rVdMLyLhZh2uvqn+33CAoPq1SFnjBZamza8h6byKcctdTWYd8bnh4Z3ucybZwZsEqZN1ukPeCZkr0lzr+yw3OQYYXvw+9RqxvEF0FXdsmvnLICS4VRjb0Q/9UdWlBSK/dGhadWCrfBWsNdxuYBlwEYVZwC4KJ7TuSsXZrOOSRrg/DmBmEtJ5V1OlP3LRg3GXrKDLA4gmPcJipFbqWkUpaFGUtP0jFTofTG5q75F1ggqJ14NhVmM1cRqlnirKtUXuRQnswOxH0R2xyuXwXHJn9RzOu5yleWaFQlHuv5TvjMCVNioq2n9I6CgQ5hMOQVJzDfcHz7PWLGdXyn4+N847637Q5kIwHXiQigYkuEpjoIoGJLhKY6CKBiS4SmOgigYkuEpjoIoGJLhKY6CKBia5FB/w3AAAA//8DABFh2N/+esWhAAAAAElFTkSuQmCC";
				
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
