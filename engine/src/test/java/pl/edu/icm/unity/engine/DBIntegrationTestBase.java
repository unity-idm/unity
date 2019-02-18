/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.mock.MockPasswordVerificatorFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.cert.CertificateVerificator;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Same as {@link SecuredDBIntegrationTestBase} but additionally puts admin user in authentication context
 * so all operations are authZed 
 * @author K. Benedyczak
 */
public abstract class DBIntegrationTestBase extends SecuredDBIntegrationTestBase
{
	public static final String DEMO_KS_PASS = "the!unity";
	public static final String DEMO_KS_ALIAS = "unity-test-server";
	public static final String DEMO_SERVER_DN = "CN=Unity Test Server,O=Unity,L=Warsaw,C=EU";
	public static final String CRED_REQ_PASS = "cr-pass";
	public static final String DEF_USER = "mockuser1";
	public static final String DEF_PASSWORD = "mock~!)(@*#&$^%:?,'.\\|";
	
	@Autowired
	protected InternalAuthorizationManager authzMan;
	@Autowired
	protected SessionManagement sessionMan;
	
	@Before
	public void setupAdmin() throws Exception
	{
		setupUserContext("admin", null);
		authzMan.clearCache();
	}
	@After
	public void clearAuthnCtx() throws EngineException
	{
		InvocationContext.setCurrent(null);
		authzMan.clearCache();
	}	
	
	public static KeystoreCredential getDemoCredential() throws KeyStoreException, IOException
	{
		return new KeystoreCredential("src/test/resources/pki/demoKeystore.p12", 
				DEMO_KS_PASS.toCharArray(), DEMO_KS_PASS.toCharArray(), DEMO_KS_ALIAS, "PKCS12");
	}
	
	public static KeystoreCertChainValidator getDemoValidator() throws KeyStoreException, IOException
	{
		return new KeystoreCertChainValidator("src/test/resources/pki/demoTruststore.jks", 
				DEMO_KS_PASS.toCharArray(), "JKS", -1);
	}
	
	protected long setupUserContext(String user, String outdatedCred) throws Exception
	{
		long ret = setupUserContext(sessionMan, identityResolver, user, outdatedCred, Collections.emptyList());
		authzMan.clearCache();
		return ret;
	}

	public static long setupUserContext(SessionManagement sessionMan, IdentityResolver identityResolver,
			String user, String credentialId, List<AuthenticationFlow> endpointFlows) throws Exception
	{
		EntityWithCredential entity = identityResolver.resolveIdentity(user, new String[] {UsernameIdentity.ID}, 
				MockPasswordVerificatorFactory.ID);
		InvocationContext virtualAdmin = new InvocationContext(null, getDefaultRealm(), endpointFlows);
		LoginSession ls = sessionMan.getCreateSession(entity.getEntityId(), getDefaultRealm(),
				user, credentialId, null, null, null);
		virtualAdmin.setLoginSession(ls);
		virtualAdmin.setLocale(Locale.ENGLISH);
		//override for tests: it can happen that existing session is returned, therefore old state of cred is
		// there.
		ls.setOutdatedCredentialId(credentialId);
		InvocationContext.setCurrent(virtualAdmin);
		return entity.getEntityId();
	}
	
	private static AuthenticationRealm getDefaultRealm()
	{
		return new AuthenticationRealm("DEFAULT_AUTHN_REALM", 
				"For tests", 5, 10, RememberMePolicy.disallow , 1, 30*60);
	}

	protected Identity createUsernameUser(String username) throws Exception
	{
		return createUsernameUser(username, null, DEF_PASSWORD, CR_MOCK);
	}
	
	protected Identity createUsernameUserWithRole(String role) throws Exception
	{
		return createUsernameUser(DEF_USER, role, DEF_PASSWORD, CRED_REQ_PASS);
	}

	/**
	 * Creates entity with username identity, password and given role. 
	 * The {@link #setupPasswordAuthn()} must be called before. 
	 * @param username
	 * @param role
	 * @return
	 * @throws Exception
	 */
	protected Identity createUsernameUser(String username, String role, String password, String cr) throws Exception
	{
		Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, username), 
				cr, EntityState.valid, false);
		eCredMan.setEntityCredential(new EntityParam(added1), "credential1", 
				new PasswordToken(password).toJson());
		if (role != null)
		{
			Attribute sa = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, 
				"/", Lists.newArrayList(role));
			attrsMan.createAttribute(new EntityParam(added1), sa);
		}
		return added1;
	}
	
	protected void createCertUser() throws EngineException
	{
		Identity added2 = createCertUserNoPassword(null);
		eCredMan.setEntityCredential(new EntityParam(added2), "credential1", 
				new PasswordToken("mockPassword2").toJson());
	}

	protected Identity createCertUserNoPassword(String role) throws EngineException
	{
		Identity added2 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user2"), 
				"cr-certpass", EntityState.valid, false);
		idsMan.addIdentity(new IdentityParam(X500Identity.ID, DEMO_SERVER_DN), 
				new EntityParam(added2), false);
		if (role != null)
		{
			Attribute sa = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, 
				"/", Lists.newArrayList(role));
			attrsMan.createAttribute(new EntityParam(added2), sa);
		}
		return added2;
	}
		
	protected void setupPasswordAuthn() throws EngineException
	{
		setupPasswordAuthn(4, true);
	}

	protected void setupPasswordAuthn(int minLen, boolean denySeq) throws EngineException
	{
		CredentialDefinition credDef = new CredentialDefinition(
				PasswordVerificator.NAME, "credential1");
		credDef.setConfiguration("{\"minLength\": " + minLen + ", " +
				"\"historySize\": 5," +
				"\"minClassesNum\": 1," +
				"\"denySequences\": " + denySeq + "," +
				"\"maxAge\": 30758400}");
		credMan.addCredentialDefinition(credDef);
		
		CredentialRequirements cr = new CredentialRequirements(CRED_REQ_PASS, "", 
				Collections.singleton(credDef.getName()));
		credReqMan.addCredentialRequirement(cr);
	}
	
	
	protected void setupPasswordAndCertAuthn() throws EngineException
	{
		CredentialDefinition credDef2 = new CredentialDefinition(
				CertificateVerificator.NAME, "credential2");
		credMan.addCredentialDefinition(credDef2);
		
		CredentialRequirements cr2 = new CredentialRequirements("cr-cert", "", 
				Collections.singleton(credDef2.getName()));
		credReqMan.addCredentialRequirement(cr2);

		Set<String> creds = new HashSet<String>();
		Collections.addAll(creds, "credential1", credDef2.getName());
		CredentialRequirements cr3 = new CredentialRequirements("cr-certpass", "", creds);
		credReqMan.addCredentialRequirement(cr3);
	}

}
