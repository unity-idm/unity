/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;


public class AdditionalAuthenticationIntegrationTest extends DBIntegrationTestBase
{
	@Autowired
	protected SessionManagement sessionMan;



	@Test
	public void shouldNotRequireAdditionalAuthnAfterItIsPerformedForCredChange() throws Exception
	{
		setupPasswordAuthn();
		setupPasswordAndCertAuthn();
		createCertUserNoPassword(InternalAuthorizationManagerImpl.USER_ROLE); //Has no password set, but password is allowed
		setupUserContext(sessionMan, identityResolver, "user2", null, getEndpointFlows());

		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user2")); 
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty").toJson());

		sessionMan.recordAdditionalAuthentication(InvocationContext.getCurrent().getLoginSession().getId(), 
				"authenticator1");
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson());
	}
	
	@Test
	public void shouldRequireAdditionalAuthnForCredChange() throws Exception
	{
		setupPasswordAuthn();
		setupPasswordAndCertAuthn();
		createCertUserNoPassword(InternalAuthorizationManagerImpl.USER_ROLE); //Has no password set, but password is allowed
		setupUserContext(sessionMan, identityResolver, "user2", null, getEndpointFlows());

		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user2")); 
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty").toJson());

		Throwable exception = catchThrowable(() -> eCredMan.setEntityCredential(user, "credential1", 
				new PasswordToken("qw!Erty2").toJson()));
		
		assertThat(exception).isInstanceOf(AdditionalAuthenticationRequiredException.class);
	}

	private List<AuthenticationFlow> getEndpointFlows()
	{
		AuthenticatorInstanceMetadata authnInstance = mock(AuthenticatorInstanceMetadata.class);
		when(authnInstance.getLocalCredentialName()).thenReturn("credential1");
		when(authnInstance.getId()).thenReturn("authenticator1");
		CredentialRetrieval retrieval = mock(CredentialRetrieval.class);
		when(retrieval.requiresRedirect()).thenReturn(false);
		
		AuthenticatorInstance authn = mock(AuthenticatorInstance.class);
		when(authn.getMetadata()).thenReturn(authnInstance);
		when(authn.getRetrieval()).thenReturn(retrieval);
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER,
				Sets.newHashSet(authn), Collections.emptyList(), 1);
		return Lists.newArrayList(flow);
	}
}
