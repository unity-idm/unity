/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_7;


import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;

/**
 * Shared code updating authenticators. 
 */
class UpdateHelperFrom2_7
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateHelperFrom2_7.class);
	
	private static final String TRIGGER_MODE_OLD_NAME = "afterRemoteLogin";
	private static final String TRIGGER_MODE_NEW_NAME = "afterRemoteLoginWhenUnknownUser";
	
	static ObjectNode updateAuthenticator(ObjectNode authenticator)
	{
		log.info("Updating authenticator from: \n{}", JsonUtil.toJsonString(authenticator));
		String name = authenticator.get("id").asText();
		String verificationMethod = authenticator.get("typeDescription").get("verificationMethod").asText();
		String configuration = authenticator.hasNonNull("verificatorConfiguration") ?
				authenticator.get("verificatorConfiguration").asText() : null;
		String localCredentialName = authenticator.hasNonNull("localCredentialName") ? 
				authenticator.get("localCredentialName").asText() : null;
		long revision = authenticator.hasNonNull("revision") ? 
				authenticator.get("revision").asLong() : 0;
		
		ObjectNode authenticatorConfig = Constants.MAPPER.createObjectNode();
		authenticatorConfig.put("name", name);
		authenticatorConfig.put("verificationMethod", verificationMethod);
		if (configuration != null)
			authenticatorConfig.put("configuration", configuration);
		if (localCredentialName != null)
			authenticatorConfig.put("localCredentialName", localCredentialName);
		authenticatorConfig.put("revision", revision);
		
		log.info("Updated authenticator to: \n{}", JsonUtil.toJsonString(authenticatorConfig));
		return authenticatorConfig;
	}
	
	static ObjectNode updateRegistrationRequest(ObjectNode registrationRequest)
	{
		return updateContext("Registration Reqeust", registrationRequest);
	}
	
	static ObjectNode updateEnquiryResponse(ObjectNode registrationRequest)
	{
		return updateContext("Enquiry Response", registrationRequest);
	}
	
	private static ObjectNode updateContext(String source, ObjectNode request)
	{
		if (JsonUtil.notNull(request, "Context"))
		{
			ObjectNode context = (ObjectNode) request.get("Context");
			String triggeringMode = context.get("triggeringMode").asText();
			if (TRIGGER_MODE_OLD_NAME.equals(triggeringMode))
			{
				log.info("Updating {} triggerMode from {} to {}\n{}", source, 
						TRIGGER_MODE_OLD_NAME, TRIGGER_MODE_NEW_NAME, request);
				context.put("triggeringMode", TRIGGER_MODE_NEW_NAME);
			}
		}
		return request;
	}
	
	static ObjectNode updateInvitationWithCode(ObjectNode invitationWithCode)
	{
		log.info("Updating invitationWithCode from: \n{}", JsonUtil.toJsonString(invitationWithCode));
		invitationWithCode.put("type", InvitationType.REGISTRATION.toString());
		
		log.info("Updated invitationWithCode to: \n{}", JsonUtil.toJsonString(invitationWithCode));
		return invitationWithCode;
	}
}
