/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import eu.unicore.security.wsutil.client.WSClientFactory;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.ws.mock.MockWSEndpointFactory;
import pl.edu.icm.unity.ws.mock.MockWSSEI;
import xmlbeans.org.oasis.saml2.assertion.NameIDDocument;

/**
 * 
 * @author P.Piernik
 *
 */
public class CompositePasswordTest extends DBIntegrationTestBase
{
	@Autowired
	private AuthenticatorManagement authnMan;

	protected Identity createUsernameUser(String username, String credential, String password)
			throws Exception
	{
		Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, username),
				"sys:all", EntityState.valid, false);
		eCredMan.setEntityCredential(new EntityParam(added1), credential,
				new PasswordToken(password).toJson());
		return added1;
	}

	private void setupPasswordCred(String name) throws EngineException
	{
		CredentialDefinition credDef = new CredentialDefinition(PasswordVerificator.NAME,
				name);
		credDef.setConfiguration("{\"minLength\": 3, " + "\"historySize\": 5,"
				+ "\"minClassesNum\": 1," + "\"denySequences\":0 ,"
				+ "\"maxAge\": 30758400}");
		credMan.addCredentialDefinition(credDef);
	}

	@Test
	public void shouldAuthnWithFirstPossibleCredential() throws Exception
	{
		setupPasswordCred("pass1");
		setupPasswordCred("pass2");
		Identity id1 = createUsernameUser("test1", "pass1", "the!test");
		createUsernameUser("test2", "pass2", "the!test2");

		authnMan.createAuthenticator("pass1", "composite-password",
				"compositePassword.verificators.1.verificatorType=password\n"
						+ "compositePassword.verificators.1.verificatorCredential=pass1\n"
						+ "\n"
						+ "compositePassword.verificators.2.verificatorType=password\n"
						+ "compositePassword.verificators.2.verificatorCredential=pass2",
				null);

		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 5, 1, RememberMePolicy.disallow ,1, 600);
		realmsMan.addRealm(realm);

		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"),
				"desc", Lists.newArrayList("pass1"), "", realm.getName());
		endpointMan.deploy(MockWSEndpointFactory.NAME, "endpoint1", "/mock", cfg);

		httpServer.start();

		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(false);

		clientCfg.setHttpUser("test1");
		clientCfg.setHttpPassword("the!test");

		clientCfg.setHttpAuthn(true);
		WSClientFactory factory = new WSClientFactory(clientCfg);
		MockWSSEI wsProxy = factory.createPlainWSProxy(MockWSSEI.class,
				"https://localhost:53456/mock"
						+ MockWSEndpointFactory.SERVLET_PATH);

		NameIDDocument ret = wsProxy.getAuthenticatedUser();
		assertEquals("[test1]", ret.getNameID().getStringValue());

		clientCfg.setHttpUser("test2");
		clientCfg.setHttpPassword("the!test2");
		factory = new WSClientFactory(clientCfg);
		wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"
				+ MockWSEndpointFactory.SERVLET_PATH);

		ret = wsProxy.getAuthenticatedUser();
		assertEquals("[test2]", ret.getNameID().getStringValue());

		eCredMan.setEntityCredential(new EntityParam(id1), "pass2",
				new PasswordToken("the!test3").toJson());

		clientCfg.setHttpUser("test1");
		clientCfg.setHttpPassword("the!test");
		factory = new WSClientFactory(clientCfg);
		wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"
				+ MockWSEndpointFactory.SERVLET_PATH);

		ret = wsProxy.getAuthenticatedUser();
		assertEquals("[test1]", ret.getNameID().getStringValue());

	}

}
