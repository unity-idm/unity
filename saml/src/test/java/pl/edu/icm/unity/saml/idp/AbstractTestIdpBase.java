/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.translation.out.action.CreateAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.out.action.FilterAttributeActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.saml.idp.ws.SamlSoapEndpoint;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttribute;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.cert.CertificateVerificator;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public abstract class AbstractTestIdpBase extends DBIntegrationTestBase
{
	private static final String SAML_ENDP_CFG = "unity.endpoint.sessionTimeout=3600\n" +
			"unity.saml.issuerURI=http://example-saml-idp.org\n" +
			"unity.saml.signResponses=asRequest\n" +
			"unity.saml.validityPeriod=3600\n" +
			"unity.saml.requestValidityPeriod=600\n" +
			"unity.saml.authenticationTimeout=20\n" +
			"unity.saml.spAcceptPolicy=all\n" +
			"#unity.saml.acceptedUriSP.xx=\n" +
			"#unity.saml.acceptedDNSP.xx=\n" +
			"unity.saml.defaultGroup=/\n" +
			"unity.saml.translationProfile=testOutProfile\n" +
			"unity.saml.credential=MAIN\n";

	public static final String REALM_NAME = "testr";
	
	@Autowired
	private OutputTranslationActionsRegistry tactionReg;
	@Autowired
	private TranslationProfileManagement profilesMan;
	@Autowired
	private AuthenticatorManagement authnMan;
	@Autowired
	private AuthenticationFlowManagement authnFlowMan;
	
	
	@Before
	public void setup() throws Exception
	{
		setupMockAuthn();
		createUsers();
		profilesMan.addProfile(createOutputProfile());
		AuthenticationRealm realm = new AuthenticationRealm(REALM_NAME, "", 
				10, 100, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);
		
		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER,
				Sets.newHashSet("Apass", "Acert")));
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpointIDP"), "desc", 
				Lists.newArrayList("flow1"), SAML_ENDP_CFG, REALM_NAME);
		endpointMan.deploy(SamlSoapEndpoint.NAME, "endpointIDP", "/saml", cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		httpServer.start();
	}
	
	private TranslationProfile createOutputProfile() throws IllegalTypeException, EngineException
	{
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = tactionReg.getByName(
				CreateAttributeActionFactory.NAME).getInstance(
				"memberOf", 
				"groups", "false");
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = tactionReg.getByName(
				CreateAttributeActionFactory.NAME).getInstance(
				"unity:identity:userName", 
				"idsByType['userName']", "false");
		rules.add(new TranslationRule("idsByType['userName'] != null", action2));
		TranslationAction action3 = tactionReg.getByName(
				CreateAttributeActionFactory.NAME).getInstance(
				"unity:identity:x500Name", 
				"idsByType['x500Name']", "false");
		rules.add(new TranslationRule("idsByType['x500Name'] != null", action3));
		TranslationAction action4 = tactionReg.getByName(
				CreateAttributeActionFactory.NAME).getInstance(
				"unity:identity:persistent", 
				"idsByType['persistent']", "false");
		rules.add(new TranslationRule("idsByType['persistent'] != null", action4));
		TranslationAction action5 = tactionReg.getByName(
				FilterAttributeActionFactory.NAME).getInstance(
				"sys:.*");
		rules.add(new TranslationRule("true", action5));
		return new TranslationProfile("testOutProfile", "", ProfileType.OUTPUT, rules);
	}
	
	protected DefaultClientConfiguration getClientCfg() throws KeyStoreException, IOException
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.getHttpClientProperties().setSocketTimeout(3600000);
		return clientCfg;
	}
	
	protected void createUsers() throws Exception
	{
		Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user1"), 
				"cr-pass", EntityState.valid, false);
		EntityParam e1 = new EntityParam(added1);
		eCredMan.setEntityCredential(e1, "credential1", new PasswordToken("mockPassword1").toJson());
		
		Identity added2 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user2"), 
				"cr-certpass", EntityState.valid, false);
		EntityParam e2 = new EntityParam(added2);
		idsMan.addIdentity(new IdentityParam(X500Identity.ID, DBIntegrationTestBase.DEMO_SERVER_DN), 
				e2, false);
		eCredMan.setEntityCredential(new EntityParam(added2), "credential1", 
				new PasswordToken("mockPassword2").toJson());
		
		aTypeMan.addAttributeType(new AttributeType("stringA", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("intA", IntegerAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("emailA", VerifiableEmailAttributeSyntax.ID));
		AttributeType fAT = new AttributeType("floatA", FloatingPointAttributeSyntax.ID);
		fAT.setMaxElements(100);
		aTypeMan.addAttributeType(fAT);
		
		attrsMan.createAttribute(e1, StringAttribute.of("stringA", "/", "value"));
		attrsMan.createAttribute(e1, IntegerAttribute.of("intA", "/", 123));
		List<Double> vals = new ArrayList<Double>();
		vals.add(123.1);
		vals.add(124.1);
		vals.add(14.2);
		attrsMan.createAttribute(e1, FloatingPointAttribute.of("floatA", "/", vals));
		attrsMan.createAttribute(e1, VerifiableEmailAttribute.of("emailA", "/", "example@example.com"));

		attrsMan.createAttribute(e2, StringAttribute.of("stringA", "/"));
		attrsMan.createAttribute(e2, IntegerAttribute.of("intA", "/", 1));
		attrsMan.createAttribute(e2, FloatingPointAttribute.of("floatA", "/", vals));
		
		attrsMan.createAttribute(e1, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, 
				"/", "Inspector"));
		attrsMan.createAttribute(e2, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, 
				"/", "Regular User"));
	}
	
	protected void setupMockAuthn() throws Exception
	{
		CredentialDefinition credDef = new CredentialDefinition(
				PasswordVerificator.NAME, "credential1");
		credDef.setConfiguration("{\"minLength\": 4, " +
				"\"historySize\": 5," +
				"\"minClassesNum\": 1," +
				"\"denySequences\": true," +
				"\"maxAge\": 30758400}");
		credMan.addCredentialDefinition(credDef);
		CredentialDefinition credDef2 = new CredentialDefinition(
				CertificateVerificator.NAME, "credential2");
		credDef2.setConfiguration("");
		credMan.addCredentialDefinition(credDef2);
		
		CredentialRequirements cr = new CredentialRequirements("cr-pass", "", 
				Collections.singleton(credDef.getName()));
		credReqMan.addCredentialRequirement(cr);
		
		Set<String> creds = new HashSet<String>();
		Collections.addAll(creds, credDef.getName(), credDef2.getName());
		CredentialRequirements cr3 = new CredentialRequirements("cr-certpass", "", creds);
		credReqMan.addCredentialRequirement(cr3);
		
		authnMan.createAuthenticator("Apass", "password", "", "credential1");
		authnMan.createAuthenticator("Acert", "certificate", "", "credential2");
	}
}
