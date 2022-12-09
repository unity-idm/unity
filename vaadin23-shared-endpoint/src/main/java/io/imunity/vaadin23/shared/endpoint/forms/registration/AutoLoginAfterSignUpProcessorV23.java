/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.forms.registration;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

import java.util.List;

@PrototypeComponent
class AutoLoginAfterSignUpProcessorV23
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, AutoLoginAfterSignUpProcessorV23.class);
	
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
		
		if (editor.getAuthnOptionKey() == null)
		{
			LOG.debug("Automatic login for registration request {} is not supported, "
					+ "auto sign in requires information on the authentication option key used for sign in", 
					requestState.getRequestId());
			return false;
		}
		
		return false;
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
