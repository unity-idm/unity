/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
		AuthenticationRealm userRealm = mock(AuthenticationRealm.class);
		AuthenticationRealm endpointRealm = mock(AuthenticationRealm.class);
		LoginMachineDetails loginMachineDetails = mock(LoginMachineDetails.class);
		InteractiveAuthenticationProcessor.SessionReinitializer sessionReinitializer =
			mock(InteractiveAuthenticationProcessor.SessionReinitializer.class);
		HttpServletResponse httpResponse = mock(HttpServletResponse.class);
		HttpSession httpSession = mock(HttpSession.class);
		LoginSession loginSession = mock(LoginSession.class);
		LoginSession.RememberMeInfo rememberMeInfo = new LoginSession.RememberMeInfo(false, false);


		String realmName = "realm1";
		when(userRealm.getName()).thenReturn(realmName);
		when(endpointRealm.getName()).thenReturn(realmName);

		when(loginSession.getRememberMeInfo()).thenReturn(rememberMeInfo);
		when(authenticatedEntity.getEntityId()).thenReturn(1L);
		when(authenticatedEntity.getOutdatedCredentialId()).thenReturn("out");
		when(sessionReinitializer.reinitialize()).thenReturn(httpSession);
		when(entityMan.getEntityLabel(new EntityParam(1L))).thenReturn("label");
		when(sessionMan.getCreateSession(1, userRealm, "label", "out",
			rememberMeInfo, authenticationOptionKey, null, null, Set.of(AuthenticationMethod.unkwown))).thenReturn(loginSession);
		InvocationContext.setCurrent(new InvocationContext(null, endpointRealm, List.of()));

		processor.syntheticAuthenticate(null, authenticatedEntity, List.of(), authenticationOptionKey, userRealm,
			loginMachineDetails, false, httpResponse, sessionReinitializer);

		verify(sessionBinder).bindHttpSession(httpSession, loginSession);
	}

	@Test
	public void shouldNotBindSessionWhenRealmsAreNotEqual() throws EngineException
	{
		AuthenticatedEntity authenticatedEntity = mock(AuthenticatedEntity.class);
		AuthenticationOptionKey authenticationOptionKey = mock(AuthenticationOptionKey.class);
		AuthenticationRealm userRealm = mock(AuthenticationRealm.class);
		AuthenticationRealm endpointRealm = mock(AuthenticationRealm.class);
		LoginMachineDetails loginMachineDetails = mock(LoginMachineDetails.class);
		InteractiveAuthenticationProcessor.SessionReinitializer sessionReinitializer =
			mock(InteractiveAuthenticationProcessor.SessionReinitializer.class);
		HttpServletResponse httpResponse = mock(HttpServletResponse.class);
		HttpSession httpSession = mock(HttpSession.class);
		LoginSession loginSession = mock(LoginSession.class);
		LoginSession.RememberMeInfo rememberMeInfo = new LoginSession.RememberMeInfo(false, false);

		when(userRealm.getName()).thenReturn("realm1");
		when(endpointRealm.getName()).thenReturn("ANOTHER_REALM");

		when(loginSession.getRememberMeInfo()).thenReturn(rememberMeInfo);
		when(authenticatedEntity.getEntityId()).thenReturn(1L);
		when(authenticatedEntity.getOutdatedCredentialId()).thenReturn("out");
		when(sessionReinitializer.reinitialize()).thenReturn(httpSession);
		when(entityMan.getEntityLabel(new EntityParam(1L))).thenReturn("label");
		when(sessionMan.getCreateSession(1, userRealm, "label", "out",
			rememberMeInfo, authenticationOptionKey, null, null, Set.of(AuthenticationMethod.unkwown))).thenReturn(loginSession);
		InvocationContext.setCurrent(new InvocationContext(null, endpointRealm, List.of()));

		processor.syntheticAuthenticate(null, authenticatedEntity, List.of(), authenticationOptionKey, userRealm,
			loginMachineDetails, false, httpResponse, sessionReinitializer);

		verify(sessionBinder, times(0)).bindHttpSession(httpSession, loginSession);
	}
	
}
