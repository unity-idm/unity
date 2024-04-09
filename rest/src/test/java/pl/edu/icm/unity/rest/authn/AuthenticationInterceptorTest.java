/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.authn;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.DenyReason;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.rest.authn.ext.HttpBasicRetrievalBase;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationInterceptorTest
{

	@Test
	public void shouldGoToOptionalPathWhenNotDefCred() throws AuthenticationException
	{
		AuthenticatorInstance mockAuthenticator1 = mock(AuthenticatorInstance.class);
		when(mockAuthenticator1.getMetadata()).thenReturn(new AuthenticatorInstanceMetadata());
		when(mockAuthenticator1.getRetrieval()).thenReturn(new NotDefCredRetrieval());

		AuthenticationProcessor mockProcessor = mock(AuthenticationProcessor.class);
		AuthenticationFlow flow1 = new AuthenticationFlow("flow1", Policy.REQUIRE, Set.of(mockAuthenticator1),
				Collections.emptyList(), null, 0);
		AuthenticationInterceptor interceptor = new AuthenticationInterceptor(null, mockProcessor, List.of(flow1),
				new AuthenticationRealm("realm1", null, 0, 0, null, 0, 0), mock(SessionManagement.class), Set.of("/p1"),
				Set.of("/optional"), null, mock(EntityManagement.class));
		HTTPRequestContext.setCurrent(new HTTPRequestContext("192.168.0.1", "agent"));
		Message message = new MessageImpl();
		message.put(Message.REQUEST_URI, "/optional");
		interceptor.handleMessage(message);
	}

	@Test(expected = Fault.class)
	public void shouldFaultWhenGoToOptionalPathWhenInvalidCredential() throws AuthenticationException
	{
		AuthenticatorInstance mockAuthenticator1 = mock(AuthenticatorInstance.class);
		AuthenticationProcessor mockProcessor = mock(AuthenticationProcessor.class);
		AuthenticationFlow flow1 = new AuthenticationFlow("flow1", Policy.REQUIRE, Set.of(mockAuthenticator1),
				Collections.emptyList(), null, 0);

		when(mockAuthenticator1.getMetadata()).thenReturn(new AuthenticatorInstanceMetadata());
		when(mockAuthenticator1.getRetrieval()).thenReturn(new DenyRetrieval());
		when(mockProcessor.processPrimaryAuthnResult(any(), any(), any())).thenThrow(new AuthenticationException(""));
		SessionManagement sessionMan = mock(SessionManagement.class);

		AuthenticationInterceptor interceptor = new AuthenticationInterceptor(mock(MessageSource.class), mockProcessor,
				List.of(flow1), new AuthenticationRealm("realm1", null, 0, 0, null, 0, 0), sessionMan, Set.of("/p1"),
				Set.of("/optional"), null, mock(EntityManagement.class));
		HTTPRequestContext.setCurrent(new HTTPRequestContext("192.168.0.1", "agent"));

		Message message = new MessageImpl();
		message.put(Message.REQUEST_URI, "/optional");
		interceptor.handleMessage(message);
	}

	@Test
	public void shouldGoToSecodFlowWhenNotDefinedCredentialOnFirstFlow() throws AuthenticationException
	{
		AuthenticatorInstance mockAuthenticator1 = mock(AuthenticatorInstance.class);
		AuthenticatorInstance mockAuthenticator2 = mock(AuthenticatorInstance.class);
		AuthenticationProcessor mockProcessor = mock(AuthenticationProcessor.class);
		AuthenticationFlow flow1 = new AuthenticationFlow("flow1", Policy.REQUIRE, Set.of(mockAuthenticator1),
				Collections.emptyList(), null, 0);
		AuthenticationFlow flow2 = new AuthenticationFlow("flow2", Policy.REQUIRE, Set.of(mockAuthenticator2),
				Collections.emptyList(), null, 0);
		SessionManagement sessionMan = mock(SessionManagement.class);

		when(mockAuthenticator1.getMetadata()).thenReturn(new AuthenticatorInstanceMetadata());
		when(mockAuthenticator2.getMetadata()).thenReturn(new AuthenticatorInstanceMetadata());

		when(mockAuthenticator1.getRetrieval()).thenReturn(new NotDefCredRetrieval());
		when(mockAuthenticator2.getRetrieval()).thenReturn(new SuccessRetrieval());

		when(mockProcessor.processPrimaryAuthnResult(any(), any(), any()))
				.thenReturn(new PartialAuthnState(AuthenticationOptionKey.authenticatorOnlyKey("x"), null,
						LocalAuthenticationResult.successful(new AuthenticatedEntity(1L, "", "")), flow2));

		when(mockProcessor.finalizeAfterPrimaryAuthentication(any(), eq(false)))
				.thenReturn(new AuthenticatedEntity(1L, "", ""));
		when(sessionMan.getCreateSession(eq(1L), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(new LoginSession());

		AuthenticationInterceptor interceptor = new AuthenticationInterceptor(mock(MessageSource.class), mockProcessor,
				List.of(flow1, flow2), new AuthenticationRealm("realm1", null, 0, 0, null, 0, 0), sessionMan,
				Set.of("/p1"), Set.of("/optional"), null, mock(EntityManagement.class));
		HTTPRequestContext.setCurrent(new HTTPRequestContext("192.168.0.1", "agent"));
		interceptor.handleMessage(new MessageImpl());
	}

	@Test(expected = Fault.class)
	public void shouldThrowFaultWhenFirstFlowFail() throws AuthenticationException
	{
		AuthenticatorInstance mockAuthenticator1 = mock(AuthenticatorInstance.class);
		AuthenticatorInstance mockAuthenticator2 = mock(AuthenticatorInstance.class);
		AuthenticationProcessor mockProcessor = mock(AuthenticationProcessor.class);
		AuthenticationFlow flow1 = new AuthenticationFlow("flow1", Policy.REQUIRE, Set.of(mockAuthenticator1),
				Collections.emptyList(), null, 0);
		AuthenticationFlow flow2 = new AuthenticationFlow("flow2", Policy.REQUIRE, Set.of(mockAuthenticator2),
				Collections.emptyList(), null, 0);

		when(mockAuthenticator1.getMetadata()).thenReturn(new AuthenticatorInstanceMetadata());
		when(mockAuthenticator1.getRetrieval()).thenReturn(new DenyRetrieval());

		when(mockProcessor.processPrimaryAuthnResult(any(), any(), any())).thenThrow(new AuthenticationException(""));
		AuthenticationInterceptor interceptor = new AuthenticationInterceptor(mock(MessageSource.class), mockProcessor,
				List.of(flow1, flow2), new AuthenticationRealm("realm1", null, 0, 0, null, 0, 0),
				mock(SessionManagement.class), Set.of("/p1"), Set.of("/optional"), null, mock(EntityManagement.class));
		HTTPRequestContext.setCurrent(new HTTPRequestContext("192.168.0.1", "agent"));
		interceptor.handleMessage(new MessageImpl());
	}

	private static class NotDefCredRetrieval extends HttpBasicRetrievalBase
	{

		public NotDefCredRetrieval()
		{
			super("mock");
		}

		@Override
		public AuthenticationResult getAuthenticationResult(Properties endpointFeatures)
		{
			return LocalAuthenticationResult.failed(new AuthenticationResult.ResolvableError(""),
					DenyReason.undefinedCredential);
		}

		@Override
		public String getAuthenticatorId()
		{
			return "mock1";
		}

	}

	private static class SuccessRetrieval extends HttpBasicRetrievalBase
	{

		public SuccessRetrieval()
		{
			super("mock2");
		}

		@Override
		public AuthenticationResult getAuthenticationResult(Properties endpointFeatures)
		{
			return LocalAuthenticationResult.successful(new AuthenticatedEntity(1L, "", ""));
		}

		@Override
		public String getAuthenticatorId()
		{
			return "mock2";
		}

	}

	private static class DenyRetrieval extends HttpBasicRetrievalBase
	{

		public DenyRetrieval()
		{
			super("mock3");
		}

		@Override
		public AuthenticationResult getAuthenticationResult(Properties endpointFeatures)
		{
			return LocalAuthenticationResult.failed();
		}

		@Override
		public String getAuthenticatorId()
		{
			return "mock3";
		}
	}

}
