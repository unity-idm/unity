/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.EntityParam;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InteractiveAuthenticationProcessorTest
{
	@Mock
	private AuthenticationProcessor basicAuthnProcessor;
	@Mock
	private EntityManagement entityMan;
	@Mock
	private SessionManagement sessionMan;
	@Mock
	private SessionParticipantTypesRegistry participantTypesRegistry;
	@Mock
	private LoginToHttpSessionBinder sessionBinder;
	@Mock
	private RememberMeProcessorImpl rememberMeProcessor;

	@InjectMocks
	InteractiveAuthneticationProcessorImpl processor;

	@Test
	public void shouldBindSessionWhenRealmsAreEqual() throws EngineException
	{
		AuthenticatedEntity authenticatedEntity = mock(AuthenticatedEntity.class);
		AuthenticationOptionKey authenticationOptionKey = mock(AuthenticationOptionKey.class);
		AuthenticationRealm authenticationRealm = mock(AuthenticationRealm.class);
		AuthenticationRealm authenticationRealmInContext = mock(AuthenticationRealm.class);
		LoginMachineDetails loginMachineDetails = mock(LoginMachineDetails.class);
		InteractiveAuthenticationProcessor.SessionReinitializer sessionReinitializer =
			mock(InteractiveAuthenticationProcessor.SessionReinitializer.class);
		HttpServletResponse httpResponse = mock(HttpServletResponse.class);
		HttpSession httpSession = mock(HttpSession.class);
		LoginSession loginSession = mock(LoginSession.class);
		LoginSession.RememberMeInfo rememberMeInfo = new LoginSession.RememberMeInfo(false, false);


		when(authenticationRealm.getName()).thenReturn("admin");
		when(authenticationRealmInContext.getName()).thenReturn("admin");

		when(loginSession.getRememberMeInfo()).thenReturn(rememberMeInfo);
		when(authenticatedEntity.getEntityId()).thenReturn(1L);
		when(authenticatedEntity.getOutdatedCredentialId()).thenReturn("out");
		when(sessionReinitializer.reinitialize()).thenReturn(httpSession);
		when(entityMan.getEntityLabel(new EntityParam(1L))).thenReturn("label");
		when(sessionMan.getCreateSession(1, authenticationRealm, "label", "out",
			rememberMeInfo, authenticationOptionKey, null, null)).thenReturn(loginSession);
		InvocationContext.setCurrent(new InvocationContext(null, authenticationRealmInContext, List.of()));

		processor.syntheticAuthenticate(null, authenticatedEntity, List.of(), authenticationOptionKey, authenticationRealm,
			loginMachineDetails, false, httpResponse, sessionReinitializer);

		verify(sessionBinder).bindHttpSession(httpSession, loginSession);
	}

	@Test
	public void shouldNotBindSessionWhenRealmsAreNotEqual() throws EngineException
	{
		AuthenticatedEntity authenticatedEntity = mock(AuthenticatedEntity.class);
		AuthenticationOptionKey authenticationOptionKey = mock(AuthenticationOptionKey.class);
		AuthenticationRealm authenticationRealm = mock(AuthenticationRealm.class);
		AuthenticationRealm authenticationRealmInContext = mock(AuthenticationRealm.class);
		LoginMachineDetails loginMachineDetails = mock(LoginMachineDetails.class);
		InteractiveAuthenticationProcessor.SessionReinitializer sessionReinitializer =
			mock(InteractiveAuthenticationProcessor.SessionReinitializer.class);
		HttpServletResponse httpResponse = mock(HttpServletResponse.class);
		HttpSession httpSession = mock(HttpSession.class);
		LoginSession loginSession = mock(LoginSession.class);
		LoginSession.RememberMeInfo rememberMeInfo = new LoginSession.RememberMeInfo(false, false);


		when(authenticationRealm.getName()).thenReturn("admin");
		when(authenticationRealmInContext.getName()).thenReturn("home");

		when(loginSession.getRememberMeInfo()).thenReturn(rememberMeInfo);
		when(authenticatedEntity.getEntityId()).thenReturn(1L);
		when(authenticatedEntity.getOutdatedCredentialId()).thenReturn("out");
		when(sessionReinitializer.reinitialize()).thenReturn(httpSession);
		when(entityMan.getEntityLabel(new EntityParam(1L))).thenReturn("label");
		when(sessionMan.getCreateSession(1, authenticationRealm, "label", "out",
			rememberMeInfo, authenticationOptionKey, null, null)).thenReturn(loginSession);
		InvocationContext.setCurrent(new InvocationContext(null, authenticationRealmInContext, List.of()));

		processor.syntheticAuthenticate(null, authenticatedEntity, List.of(), authenticationOptionKey, authenticationRealm,
			loginMachineDetails, false, httpResponse, sessionReinitializer);

		verify(sessionBinder, times(0)).bindHttpSession(httpSession, loginSession);
	}
	
}
