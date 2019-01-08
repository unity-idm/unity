/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.session;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.AuthNInfo;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationMisconfiguredException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;

@RunWith(MockitoJUnitRunner.class)
public class AdditionalAuthenticationServiceTest
{
	@Mock
	private AuthenticationProcessor authnProcessor;
	
	@Test
	public void shouldReturnOption_Current()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Collections.emptyList(), 1);
		setupContext(flow);
		when(authnProcessor.checkIfUserHasCredential(any(), eq(1L))).thenReturn(true);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"CURRENT", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements("cred-1"));
		
		assertRequired(exception, "authn-1");
	}

	@Test
	public void shouldNotReturnOptionRequiringRedirect()
	{
		AuthenticatorInstance auth1 = getAuthenticator("remote-authn", "cred-1");
		when(auth1.getRetrieval().requiresRedirect()).thenReturn(true);
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Collections.emptyList(), 1);
		setupContext(flow);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"remote-authn", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements("cred-1"));
		
		assertMisconfigured(exception);
	}
	
	@Test
	public void shouldNotReturnNotMatching_Current()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Collections.emptyList(), 1);
		setupContext(flow);
		when(authnProcessor.checkIfUserHasCredential(any(), eq(1L))).thenReturn(false);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"CURRENT", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements("cred-1"));
		
		assertMisconfigured(exception);
	}
	
	@Test
	public void shouldReturnOption_Endpoint()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticatorInstance auth2 = getAuthenticator("authn-2", "cred-2");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Lists.newArrayList(auth2), 1);
		setupContext(flow);
		when(authnProcessor.getValidAuthenticatorForEntity(any(), eq(1L))).thenReturn(auth2);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"ENDPOINT_2F", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements());
		
		assertRequired(exception, "authn-2");
	}

	@Test
	public void shouldNotReturnNotMatching_Endpoint()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticatorInstance auth2 = getAuthenticator("authn-2", "cred-2");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Lists.newArrayList(auth2), 1);
		setupContext(flow);
		when(authnProcessor.getValidAuthenticatorForEntity(any(), eq(1L))).thenReturn(null);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"ENDPOINT_2F", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements());

		assertMisconfigured(exception);
	}
	
	@Test
	public void shouldReturnOption_Session1()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticatorInstance auth2 = getAuthenticator("authn-2", "cred-2");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Lists.newArrayList(auth2), 1);
		setupContext(flow, "authn-1", null);
		when(authnProcessor.checkIfUserHasCredential(any(), eq(1L))).thenReturn(true);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"SESSION_1F", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements());
		
		assertRequired(exception, "authn-1");
	}
	
	@Test
	public void shouldReturnOption_Session2()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticatorInstance auth2 = getAuthenticator("authn-2", "cred-2");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Lists.newArrayList(auth2), 1);
		setupContext(flow, "authn-1", "authn-2");
		when(authnProcessor.checkIfUserHasCredential(any(), eq(1L))).thenReturn(true);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"SESSION_2F", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements());
		
		assertRequired(exception, "authn-2");
	}

	@Test
	public void shouldReturnOption_Direct()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Lists.newArrayList(), 1);
		setupContext(flow, "authn-1", null);
		when(authnProcessor.checkIfUserHasCredential(any(), eq(1L))).thenReturn(true);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"authn-1", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements());
		
		assertRequired(exception, "authn-1");
	}

	
	@Test
	public void shouldNotReturnNotMatching_Session1()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticatorInstance auth2 = getAuthenticator("authn-2", "cred-2");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Lists.newArrayList(auth2), 1);
		setupContext(flow, "authn-1", null);
		when(authnProcessor.checkIfUserHasCredential(any(), eq(1L))).thenReturn(false);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"SESSION_1F", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements());
		
		assertMisconfigured(exception);
	}
	
	@Test
	public void shouldNotReturnNotMatching_Session2()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticatorInstance auth2 = getAuthenticator("authn-2", "cred-2");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Lists.newArrayList(auth2), 1);
		setupContext(flow, "authn-1", "authn-2");
		when(authnProcessor.checkIfUserHasCredential(any(), eq(1L))).thenReturn(false);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"SESSION_2F", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements());
		
		assertMisconfigured(exception);
	}
	
	@Test
	public void shouldNotReturnNotMatching_Direct()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Lists.newArrayList(), 1);
		setupContext(flow, "authn-1", null);
		when(authnProcessor.checkIfUserHasCredential(any(), eq(1L))).thenReturn(false);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"authn-1", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements());
		
		assertMisconfigured(exception);
	}

	@Test
	public void shouldNotThrowWhenNoMatchAndConfiguredToIgnore()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Lists.newArrayList(), 1);
		setupContext(flow, "authn-1", null);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"", false, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements());
		
		Assertions.assertThat(exception).isNull();
	}

	@Test
	public void shouldReturnWhenSecondIsMatching()
	{
		AuthenticatorInstance auth1 = getAuthenticator("authn-1", "cred-1");
		AuthenticatorInstance auth2 = getAuthenticator("authn-2", "cred-2");
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, 
				Sets.newHashSet(auth1), Lists.newArrayList(auth2), 1);
		setupContext(flow, "authn-1", null);
		when(authnProcessor.checkIfUserHasCredential(any(), eq(1L))).thenReturn(true);
		
		AdditionalAuthenticationService service = new AdditionalAuthenticationService(authnProcessor,
				"CURRENT  SESSION_1F", true, 1);
		
		Throwable exception = catchThrowable(() -> service.checkAdditionalAuthenticationRequirements());
		
		assertRequired(exception, "authn-1");
	}
	

	
	private AuthenticatorInstance getAuthenticator(String authenticator, String credential)
	{
		AuthenticatorInstance auth1 = mock(AuthenticatorInstance.class);
		AuthenticatorInstanceMetadata instance1 = mock(AuthenticatorInstanceMetadata.class);
		when(instance1.getLocalCredentialName()).thenReturn(credential);
		when(instance1.getId()).thenReturn(authenticator);
		when(auth1.getMetadata()).thenReturn(instance1);
		CredentialRetrieval retrieval = mock(CredentialRetrieval.class);
		when(auth1.getRetrieval()).thenReturn(retrieval);
		return auth1;
	}

	private void setupContext(AuthenticationFlow flow)
	{
		setupContext(flow, "sessionAuthn", null);
	}
	
	private void setupContext(AuthenticationFlow flow, String firstAuthn, String secondAuthn)
	{
		InvocationContext invocationContext = new InvocationContext(null, null, Lists.newArrayList(flow));
		LoginSession loginSession = new LoginSession("id", new Date(200), 10, 1, "realm", 
				new RememberMeInfo(false, false), 
				new AuthNInfo(firstAuthn + ".password", new Date(200)), 
				secondAuthn != null ? new AuthNInfo(secondAuthn + ".sms", new Date(200)) : null);
		invocationContext.setLoginSession(loginSession);
		InvocationContext.setCurrent(invocationContext);
	}
	
	private void assertMisconfigured(Throwable exception)
	{
		Assertions.assertThat(exception).isNotNull()
			.isInstanceOf(AdditionalAuthenticationMisconfiguredException.class);
	}

	private void assertRequired(Throwable exception, String optionId)
	{
		Assertions.assertThat(exception).isNotNull()
			.isInstanceOf(AdditionalAuthenticationRequiredException.class);
		assertThat(((AdditionalAuthenticationRequiredException)exception).authenticationOption,
				is(optionId));
	}
}
