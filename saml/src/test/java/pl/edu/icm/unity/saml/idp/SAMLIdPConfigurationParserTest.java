/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import eu.unicore.samly2.SAMLConstants;
import org.junit.Test;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClient;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementsConfiguration;
import pl.edu.icm.unity.engine.api.idp.UserImportConfig;
import pl.edu.icm.unity.engine.api.idp.UserImportConfigs;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.engine.api.idp.CommonIdPProperties.*;
import static pl.edu.icm.unity.saml.SamlProperties.*;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.P;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.*;
import static pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType.CHECKBOX_SELECTED;

public class SAMLIdPConfigurationParserTest
{
	@Test
	public void shouldParseProperties() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P+AUTHENTICATION_TIMEOUT, "10");
		p.setProperty(P+SIGN_RESPONSE, SAMLIdPConfiguration.ResponseSigningPolicy.always.name());
		p.setProperty(P+SIGN_ASSERTION, SAMLIdPConfiguration.AssertionSigningPolicy.always.name());
		p.setProperty(P+CREDENTIAL, "credential");
		p.setProperty(P+TRUSTSTORE, "truststore");
		p.setProperty(P+DEF_ATTR_ASSERTION_VALIDITY, "10");
		p.setProperty(P+SAML_REQUEST_VALIDITY, "12");
		p.setProperty(P+ISSUER_URI, "issuerUri");
		p.setProperty(P+RETURN_SINGLE_ASSERTION, "true");
		p.setProperty(P+SP_ACCEPT_POLICY, SAMLIdPConfiguration.RequestAcceptancePolicy.all.name());
		p.setProperty(P+DEFAULT_GROUP, "/");
		p.setProperty(P+GROUP_PFX+"1."+GROUP, "group");
		p.setProperty(P+GROUP_PFX+"1."+GROUP_TARGET, "group");
		p.setProperty(P+USER_EDIT_CONSENT, "true");

		p.setProperty(P+SKIP_CONSENT, "false");
		p.setProperty(P+TRANSLATION_PROFILE, "saml");
		p.setProperty(P+SKIP_USERIMPORT, "false");

		p.setProperty(P+USERIMPORT_PFX + "1." + USERIMPORT_IMPORTER, "importer");
		p.setProperty(P+USERIMPORT_PFX + "1." + USERIMPORT_IDENTITY_TYPE, "type");

		p.setProperty(P+ACTIVE_VALUE_SELECTION_PFX + "1." + ACTIVE_VALUE_CLIENT, "client");
		p.setProperty(P+ACTIVE_VALUE_SELECTION_PFX + "1." + ACTIVE_VALUE_SINGLE_SELECTABLE + "1", "val1");
		p.setProperty(P+ACTIVE_VALUE_SELECTION_PFX + "1." + ACTIVE_VALUE_MULTI_SELECTABLE + "1", "val2");

		p.setProperty(P+POLICY_AGREEMENTS_TITLE, "title");
		p.setProperty(P+POLICY_AGREEMENTS_INFO, "info");
		p.setProperty(P+POLICY_AGREEMENTS_WIDTH, "10");
		p.setProperty(P+POLICY_AGREEMENTS_WIDTH_UNIT, "10min");
		p.setProperty(P+POLICY_AGREEMENTS_PFX + "1." + POLICY_AGREEMENT_DOCUMENTS, "10");
		p.setProperty(P+POLICY_AGREEMENTS_PFX + "1." + POLICY_AGREEMENT_PRESENTATION_TYPE, "CHECKBOX_SELECTED");
		p.setProperty(P+POLICY_AGREEMENTS_PFX + "1." + POLICY_AGREEMENT_TEXT, "txt");

		p.setProperty(P+IDENTITY_MAPPING_PFX+"1."+IDENTITY_LOCAL, "qqq");
		p.setProperty(P+IDENTITY_MAPPING_PFX+"1."+IDENTITY_SAML, "123");
		p.setProperty(P+IDENTITY_MAPPING_PFX+"2."+IDENTITY_LOCAL, "aaa");
		p.setProperty(P+IDENTITY_MAPPING_PFX+"2."+IDENTITY_SAML, SAMLConstants.NFORMAT_TRANSIENT);
		p.setProperty(P+IDENTITY_MAPPING_PFX+"3."+IDENTITY_LOCAL, "");
		p.setProperty(P+IDENTITY_MAPPING_PFX+"3."+IDENTITY_SAML, "unity:identifier");

		p.setProperty(P+SPMETA_PREFIX+"1." + METADATA_URL, "file:src/test/resources/metadata.switchaai.xml");
		p.setProperty(P+SPMETA_PREFIX+"1." + METADATA_SIGNATURE, "require");
		p.setProperty(P+SPMETA_PREFIX+"1." + METADATA_REFRESH, "100");
		p.setProperty(P+SPMETA_PREFIX+"1." + METADATA_HTTPS_TRUSTSTORE, "trustsore");
		p.setProperty(P+SPMETA_PREFIX+"1." + METADATA_ISSUER_CERT, "cert");

		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_ENTITY, "clientEntityId");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_RETURN_URL, "URL");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_RETURN_URLS + "1", "URL1");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_NAME, "Name");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_LOGO, "logo");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_CERTIFICATE, "certificate");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_CERTIFICATES + "1", "certificate");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + REDIRECT_LOGOUT_URL, "redUrl");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + POST_LOGOUT_URL, "postUrl");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + REDIRECT_LOGOUT_RET_URL, "redRetUrl");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + POST_LOGOUT_RET_URL, "postRetUrl");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + SOAP_LOGOUT_URL, "soapUrl");

		p.setProperty(P+SEND_NOT_BEFORE_CONSTRAINT, "true");

		PKIManagement pkiManagement = mock(PKIManagement.class);
		X509Certificate x509Certificatecertificate = mock(X509Certificate.class);
		NamedCertificate certificate = new NamedCertificate("certificate", x509Certificatecertificate);
		when(pkiManagement.getCertificate("certificate")).thenReturn(certificate);
		when(pkiManagement.getCredentialNames()).thenReturn(Set.of("credential"));
		MessageSource messageSource = mock(MessageSource.class);
		SAMLIdPConfigurationParser samlIdPConfigurationParser = new SAMLIdPConfigurationParser(pkiManagement, messageSource);

		SAMLIdPConfiguration configuration = samlIdPConfigurationParser.parse(p);


		assertThat(configuration.authenticationTimeout).isEqualTo(10);
		assertThat(configuration.signResponses).isEqualTo(SAMLIdPConfiguration.ResponseSigningPolicy.always);
		assertThat(configuration.signAssertion).isEqualTo(SAMLIdPConfiguration.AssertionSigningPolicy.always);
		assertThat(configuration.credentialName).isEqualTo("credential");
		assertThat(configuration.truststore).isEqualTo("truststore");
		assertThat(configuration.validityPeriod).isEqualTo(Duration.of(10, ChronoUnit.SECONDS));
		assertThat(configuration.requestValidityPeriod).isEqualTo(Duration.of(12, ChronoUnit.SECONDS));
		assertThat(configuration.issuerURI).isEqualTo("issuerUri");
		assertThat(configuration.returnSingleAssertion).isEqualTo(true);
		assertThat(configuration.spAcceptPolicy).isEqualTo(SAMLIdPConfiguration.RequestAcceptancePolicy.all);
		assertThat(configuration.groupChooser).isEqualTo(new GroupChooser(Map.of("group", "group"), "/"));
		assertThat(configuration.userCanEditConsent).isEqualTo(true);
		assertThat(configuration.skipConsent).isEqualTo(false);
		assertThat(configuration.userImportConfigs).isEqualTo(getUserImportConfigs());
		assertThat(configuration.activeValueClient).isEqualTo(Set.of(getActiveValueClient()));
		assertThat(configuration.policyAgreements).isEqualTo(getIdpPolicyAgreementsConfiguration());
		assertThat(configuration.idTypeMapper.mapIdentity("123")).isEqualTo("qqq");
		assertThat(configuration.idTypeMapper.mapIdentity(SAMLConstants.NFORMAT_TRANSIENT)).isEqualTo("aaa");
		assertThat(configuration.trustedMetadataSourcesByUrl.values().iterator().next()).isEqualTo(BaseSamlConfiguration.RemoteMetadataSource.builder()
						.withTranslationProfile(getTranslationProfile())
						.withUrl("file:src/test/resources/metadata.switchaai.xml")
						.withRefreshInterval(Duration.of(100, ChronoUnit.SECONDS))
						.withSignatureValidation(SAMLSPProperties.MetadataSignatureValidation.require)
						.withHttpsTruststore("trustsore")
						.withIssuerCertificate("cert")
						.build()
		);
		assertThat(configuration.trustedServiceProviders.getSPConfigs().iterator().next()).isEqualTo(TrustedServiceProvider.builder()
						.withAllowedKey("1")
						.withEntityId("clientEntityId")
						.withReturnUrl("URL")
						.withReturnUrls(Set.of("URL1"))
						.withName(new I18nString("Name"))
						.withLogoUri(new I18nString("logo"))
						.withCertificateName("certificate")
						.withCertificateNames(Set.of("certificate"))
						.withCertificate(x509Certificatecertificate)
						.withCertificates(Set.of(x509Certificatecertificate))
						.withRedirectLogoutUrl("redUrl")
						.withPostLogoutUrl("postUrl")
						.withRedirectLogoutRetUrl("redRetUrl")
						.withPostLogoutRetUrl("postRetUrl")
						.withSoapLogoutUrl("soapUrl")
						.build()
		);
		assertThat(configuration.sendNotBeforeConstraint).isEqualTo(true);

	}

	private static IdpPolicyAgreementsConfiguration getIdpPolicyAgreementsConfiguration()
	{
		return new IdpPolicyAgreementsConfiguration(new I18nString("title"), new I18nString("info"), 10, "10min", List.of(new PolicyAgreementConfiguration(List.of(10L), CHECKBOX_SELECTED, new I18nString("txt"))));
	}

	private static ActiveValueClient getActiveValueClient()
	{
		return new ActiveValueClient("activeValue.1.", "client", List.of("val1"), List.of("val2"));
	}

	private static UserImportConfigs getUserImportConfigs()
	{
		return new UserImportConfigs(false, Set.of(new UserImportConfig("userImport.1.", "importer", "type")));
	}

	private static TranslationProfile getTranslationProfile()
	{
		return new TranslationProfile("Embedded", "", ProfileType.INPUT, List.of(new TranslationRule("true", new TranslationAction("includeInputProfile", "sys:saml"))));
	}
}
