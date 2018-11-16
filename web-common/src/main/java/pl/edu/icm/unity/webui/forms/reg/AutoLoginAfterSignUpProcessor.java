/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.collect.Lists;
import com.vaadin.server.VaadinService;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;

/**
 * Used in standalone registration from to automatically sign in user, only in
 * case where:
 * <ul>
 * <li>the registration request was accepted
 * <li>realm is configured at registration form level
 * </ul>
 * 
 * It bypass the second factor authn, whenever registration request is
 * successful, it creates login session and sign in newly created by accepted
 * registration request user. It does not set either first nor secondary factor
 * authn option in the session.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@PrototypeComponent
class AutoLoginAfterSignUpProcessor
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, AutoLoginAfterSignUpProcessor.class);
	
	@Autowired
	private StandardWebAuthenticationProcessor standardAuthnProcessor;
	
	@Autowired
	@Qualifier("insecure")  
	private RealmsManagement realmsManagement;
	
	public void signInIfPossible(RegistrationRequestEditor editor, RegistrationRequestState requestState)
	{
		if (requestState == null)
			return;

		if (requestState.getStatus() != RegistrationRequestStatus.accepted)
		{
			LOG.debug("Registration request {} is not eligible for automatic sign in, "
					+ "status was: {}, expected: {}", requestState.getRequestId(),
					requestState.getStatus(), RegistrationRequestStatus.accepted);
			return;
		}
		
		RegistrationForm form = editor.getForm();
		if (form.getRealmName() == null)
		{
			LOG.debug("Automatic login for registration form {} disabled, skipping "
					+ "sign in for registration request {}", form.getName(), requestState.getRequestId());
			return;
		}
		AuthenticationRealm realm;
		try
		{
			realm = realmsManagement.getRealm(form.getRealmName());
		} catch (EngineException e)
		{
			LOG.error("Unable to automatically sign in entity {}.", requestState.getCreatedEntityId(), e);
			return;
		}
		
		RemotelyAuthenticatedContext remoteContext = editor.getRemotelyAuthContext();
		AuthenticatedEntity authenticatedEntity = new AuthenticatedEntity(requestState.getCreatedEntityId(), 
		        remoteContext.getMappingResult().getAuthenticatedWith(), null);
		authenticatedEntity.setRemoteIdP(remoteContext.getRemoteIdPName());
		
		LoginSession ls = getLoginSessionForEntity(authenticatedEntity, realm);
		
		logged(authenticatedEntity, realm, ls, remoteContext);
	}

	private LoginSession getLoginSessionForEntity(AuthenticatedEntity authenticatedEntity, AuthenticationRealm realm)
	{
		LoginSession ls = standardAuthnProcessor.getLoginSessionForEntity(authenticatedEntity, realm, null, null);
		return ls;
	}
	
	private void logged(AuthenticatedEntity authenticatedEntity, AuthenticationRealm realm, LoginSession ls,
			RemotelyAuthenticatedContext remoteContext)
	{
		String clientIp = VaadinService.getCurrentRequest().getRemoteAddr();
		try
		{
			standardAuthnProcessor.logged(authenticatedEntity, ls, realm, clientIp, false, 
					extractParticipants(remoteContext));
		} catch (AuthenticationException e)
		{
			LOG.error("Unable to automatically sign in entity {}.", authenticatedEntity.getEntityId(), e);
		}
	}

	private List<SessionParticipant> extractParticipants(RemotelyAuthenticatedContext remoteContext)
	{
		List<SessionParticipant> ret = Lists.newArrayList();
		if (remoteContext.getSessionParticipants() != null)
		{
			ret.addAll(remoteContext.getSessionParticipants());
		}
		return ret;
	}
}
