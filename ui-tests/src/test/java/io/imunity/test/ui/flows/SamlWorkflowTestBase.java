package io.imunity.test.ui.flows;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;

abstract class SamlWorkflowTestBase extends CredentialWorkflowTestBase
{
	static final String SP = "https://localhost:2443/ui-saml-sp";
	static final By PROVIDER = By.className("u-idpAuthentication-ui-saml-1");

	@Autowired
	@Qualifier("insecure")
	private TranslationProfileManagement profiles;
	@Autowired
	@Qualifier("insecure")
	private PKIManagement pki;
	@Autowired
	@Qualifier("insecure")
	private RealmsManagement realms;

	final void deploySaml(String extraSettings, List<TranslationRule> additionalInputRules) throws Exception
	{
		deploySaml(extraSettings, additionalInputRules, "unity:persistent");
	}

	final void deploySaml(String extraSettings, List<TranslationRule> additionalInputRules, String identityFormat)
			throws Exception
	{
		realms.addRealm(new AuthenticationRealm("ui-saml-idp", "Local Selenium IdP", 5, 8,
				RememberMePolicy.disallow, 0, 600));
		pki.addPersistedCertificate(new NamedCertificate("ui-saml-certificate",
				pki.getCredential("MAIN").getCertificate()));
		List<TranslationRule> output = new ArrayList<>(profiles.getOutputProfile("sys:default").getRules());
		output.add(new TranslationRule("true", new TranslationAction("createAttribute", "username",
				"idsByType['userName']", "false", "Username", "Local username")));
		profiles.addProfile(new TranslationProfile("ui-saml-output", "Local Selenium IdP", ProfileType.OUTPUT, output));
		List<TranslationRule> input = new ArrayList<>();
		input.add(new TranslationRule("true", new TranslationAction("mapIdentity", "userName", "attr['username']",
				"sys:all", "MATCH")));
		input.addAll(additionalInputRules);
		profiles.addProfile(new TranslationProfile("ui-saml-input", "Local Selenium SP", ProfileType.INPUT, input));
		deployAt("SAMLWebIdP", "/saml-idp", """
				unity.saml.issuerURI=https://localhost:2443/ui-saml-idp
				unity.saml.credential=MAIN
				unity.saml.truststore=MAIN
				unity.saml.signResponses=always
				unity.saml.signAssertion=always
				unity.saml.defaultGroup=/
				unity.saml.translationProfile=ui-saml-output
				unity.saml.spAcceptPolicy=validRequester
				unity.saml.acceptedSP.1.entity=https://localhost:2443/ui-saml-sp
				unity.saml.acceptedSP.1.name=Local Selenium SP
				unity.saml.acceptedSP.1.returnURL=https://localhost:2443/unitygw/spSAMLResponseConsumer
				unity.saml.acceptedSP.1.certificate=ui-saml-certificate
				unity.saml.acceptedSP.1.redirectLogoutEndpoint=https://localhost:2443/unitygw/SPSLO/WEB/ui-slo
				unity.saml.acceptedSP.1.redirectLogoutResponseEndpoint=\
				https://localhost:2443/unitygw/SAMLSLOResponseConsumer
				""" + extraSettings, List.of("pwdSys"), "ui-saml-idp");
		authenticators.createAuthenticator("ui-saml", "saml2", """
				unity.saml.requester.requesterEntityId=https://localhost:2443/ui-saml-sp
				unity.saml.requester.requesterCredential=MAIN
				unity.saml.requester.metadataPath=ui-metadata
				unity.saml.requester.sloPath=ui-slo
				unity.saml.requester.sloRealm=ui
				unity.saml.requester.acceptedNameFormats.1=urn:oasis:names:tc:SAML:2.0:nameid-format:persistent
				unity.saml.requester.acceptedNameFormats.2=unity:userName
				unity.saml.requester.remoteIdp.1.name=Local Selenium SAML
				unity.saml.requester.remoteIdp.1.address=https://localhost:2443/saml-idp/saml2idp-web
				unity.saml.requester.remoteIdp.1.binding=HTTP_REDIRECT
				unity.saml.requester.remoteIdp.1.samlId=https://localhost:2443/ui-saml-idp
				unity.saml.requester.remoteIdp.1.certificate=ui-saml-certificate
				unity.saml.requester.remoteIdp.1.signRequest=false
				unity.saml.requester.remoteIdp.1.requestedNameFormat=%s
				unity.saml.requester.remoteIdp.1.translationProfile=ui-saml-input
				unity.saml.requester.remoteIdp.1.redirectLogoutEndpoint=https://localhost:2443/saml-idp/SLO-WEB
				unity.saml.requester.remoteIdp.1.redirectLogoutResponseEndpoint=\
				https://localhost:2443/unitygw/SAMLSLOResponseConsumer
				""".formatted(identityFormat), null);
		deployAt("UserHomeUI", "/saml-home", "", List.of("ui-saml"));
	}
}
