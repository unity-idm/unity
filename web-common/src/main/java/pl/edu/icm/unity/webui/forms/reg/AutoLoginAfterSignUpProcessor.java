/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.time.Instant;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.vaadin.server.VaadinServletResponse;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.column.VaadinSessionReinitializer;

/**
 * Used in standalone registration from to automatically sign in user, only in
 * case where:
 * <ul>
 * <li>the registration request was auto accepted
 * <li>realm is configured at registration form level
 * <li>remote sign up method was used to fill out the registration request
 * <li>time from remote authN to now should not be < realm's max session
 * inactivity time
 * </ul>
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@PrototypeComponent
class AutoLoginAfterSignUpProcessor
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, AutoLoginAfterSignUpProcessor.class);
	
	@Autowired
	private InteractiveAuthenticationProcessor authnProcessor;
	
	@Autowired
	@Qualifier("insecure")  
	private RealmsManagement realmsManagement;
	
	public boolean signInIfPossible(RegistrationRequestEditor editor, RegistrationRequestState requestState)
	{
		if (requestState == null)
			return false;

		if (requestState.getStatus() != RegistrationRequestStatus.accepted)
		{
			LOG.debug("Registration request {} is not eligible for automatic sign in, "
					+ "status was: {}, expected: {}", requestState.getRequestId(),
					requestState.getStatus(), RegistrationRequestStatus.accepted);
			return false;
		}
		
		RegistrationForm form = editor.getForm();
		if (Strings.isNullOrEmpty(form.getAutoLoginToRealm()))
		{
			LOG.debug("Automatic login for registration form {} disabled, skipping "
					+ "sign in for registration request {}", form.getName(), requestState.getRequestId());
			return false;
		}
		
		RemotelyAuthenticatedPrincipal remoteContext = editor.getRemoteAuthnContext();
		if (RemotelyAuthenticatedPrincipal.isLocalContext(remoteContext))
		{
			LOG.debug("Automatic login for registration request {} is not supported, "
					+ "auto sign in requires form to be submitted with remote sign up method", 
					requestState.getRequestId());
			return false;
		}
		
		if (editor.getAuthnOptionKey() == null)
		{
			LOG.debug("Automatic login for registration request {} is not supported, "
					+ "auto sign in requires information on the authentication option key used for sign in", 
					requestState.getRequestId());
			return false;
		}
		
		AuthenticationRealm realm;
		try
		{
			realm = realmsManagement.getRealm(form.getAutoLoginToRealm());
		} catch (EngineException e)
		{
			LOG.error("Unable to automatically sign in entity {}.", requestState.getCreatedEntityId(), e);
			return false;
		}
		
		if (remoteContext.getCreationTime() == null)
		{
			LOG.debug("Unable to determine whether session expired or not, "
					+ "entity {} is not eligible for sign up after registration {}.", 
					requestState.getCreatedEntityId(), requestState.getRequestId());
			return false;
		}
		
		if (isSessionExpiredDueToUserInactivity(remoteContext.getCreationTime(), realm))
		{
			LOG.debug("Automatic login for registration request {} is not possible, "
					+ "session expired.", requestState.getRequestId());
			return false;
		}
		
		try
		{
			AuthenticatedEntity authenticatedEntity = new AuthenticatedEntity(requestState.getCreatedEntityId(), 
					remoteContext.getMappingResult().getAuthenticatedWith(), null);
			authenticatedEntity.setRemoteIdP(remoteContext.getRemoteIdPName());
			
			loginUser(authenticatedEntity, realm, remoteContext, editor.getAuthnOptionKey());
			LOG.info("Entity Id {} automatically signed into realm {}, as the result of successful "
					+ "registration request processing: {}", requestState.getCreatedEntityId(), 
					form.getAutoLoginToRealm(), requestState.getRequestId());
			return true;
		} catch (Exception e)
		{
			LOG.error("Failed to automatically sign in entity {}", requestState.getCreatedEntityId(), e);
			return false;
		}
	}

	private boolean isSessionExpiredDueToUserInactivity(Instant loginTime, AuthenticationRealm realm)
	{
		long now = Instant.now().getEpochSecond();
		long login = loginTime.getEpochSecond();
		long userActivityDuration = now - login;
		return userActivityDuration > realm.getMaxInactivity();
	}

	private void loginUser(AuthenticatedEntity authenticatedEntity, AuthenticationRealm realm, 
			RemotelyAuthenticatedPrincipal remoteContext, AuthenticationOptionKey authenticationOption)
	{
		VaadinServletResponse servletResponse = VaadinServletResponse.getCurrent();
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor
				.getLoginMachineDetailsFromCurrentRequest();
		authnProcessor.syntheticAuthenticate(authenticatedEntity, extractParticipants(remoteContext), 
				authenticationOption, realm, loginMachineDetails, false, 
				servletResponse, new VaadinSessionReinitializer());
	}
	
	private List<SessionParticipant> extractParticipants(RemotelyAuthenticatedPrincipal remoteContext)
	{
		List<SessionParticipant> ret = Lists.newArrayList();
		if (remoteContext.getSessionParticipants() != null)
		{
			ret.addAll(remoteContext.getSessionParticipants());
		}
		return ret;
	}
}
