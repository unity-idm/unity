/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.registration;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.reg.InvitationWithCodeDB;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;

/**
 * Helper component with methods to validate registration requests. There are methods to validate both the request 
 * being submitted (i.e. whether it is valid wrt its form) and validating the translated request before it is 
 * accepted.
 * <p>
 * What is more this component implements invitations handling, i.e. the overall validation and 
 * updating the request with mandatory invitation information (what must be done prior to base validation).
 * 
 * @author K. Benedyczak
 */
@Component
public class RegistrationRequestValidator extends BaseRequestValidator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER,
			RegistrationRequestValidator.class);
	@Autowired
	private InvitationWithCodeDB invitationDB;
	
	public void validateSubmittedRequest(RegistrationForm form, RegistrationRequest request,
			boolean doCredentialCheckAndUpdate, SqlSession sql) throws EngineException
	{
		boolean byInvitation = processInvitationAndValidateCode(form, request, sql);
		
		super.validateSubmittedRequest(form, request, doCredentialCheckAndUpdate, sql);
		
		if (byInvitation)
		{
			String code = request.getRegistrationCode();
			log.debug("Received registration request for invitation " + code + ", removing it");
			invitationDB.remove(code, sql);
		}
	}

	public void validateTranslatedRequest(RegistrationForm form, RegistrationRequest originalRequest, 
			TranslatedRegistrationRequest request, SqlSession sql) throws EngineException
	{
		validateFinalAttributes(request.getAttributes(), sql);
		validateFinalCredentials(originalRequest.getCredentials(), sql);
		validateFinalIdentities(request.getIdentities(), sql);
	}

	/**
	 * Code is validated, wrt to invitation or form fixed code. What is more the request attributes
	 * groups and identities are set to those from invitation when necessary and errors are reported
	 * if request tries to overwrite mandatory elements from invitation.
	 * 
	 * @param form
	 * @param request
	 * @param sql
	 * @return true if the request is by invitation
	 * @throws EngineException
	 */
	private boolean processInvitationAndValidateCode(RegistrationForm form, RegistrationRequest request,
			SqlSession sql) throws EngineException
	{
		String codeFromRequest = request.getRegistrationCode();

		if (codeFromRequest == null && form.isByInvitationOnly())
			throw new WrongArgumentException("This registration form is available "
					+ "only by invitation with correct code");
		
		if (codeFromRequest == null || form.getRegistrationCode() != null)
		{
			validateRequestCode(form, request);
			return false;
		}
				
		InvitationWithCode invitation = getInvitation(codeFromRequest, sql);
		
		if (!invitation.getFormId().equals(form.getName()))
			throw new WrongArgumentException("The invitation is for different registration form");
		
		if (invitation.isExpired())
			throw new WrongArgumentException("The invitation has already expired");
		
		processInvitationElements(form.getIdentityParams(), request.getIdentities(), 
				invitation.getIdentities(), "identity");
		processInvitationElements(form.getAttributeParams(), request.getAttributes(), 
				invitation.getAttributes(), "attribute");
		processInvitationElements(form.getGroupParams(), request.getGroupSelections(), 
				invitation.getGroupSelections(), "group");
		return true;
	}

	private <T> void processInvitationElements(List<? extends RegistrationParam> paramDef,
			List<T> requested, Map<Integer, PrefilledEntry<T>> fromInvitation, String elementName) 
					throws EngineException
	{
		validateParamsCount(paramDef, requested, elementName);
		for (Map.Entry<Integer, PrefilledEntry<T>> invitationEntry : fromInvitation.entrySet())
		{
			if (invitationEntry.getKey() >= requested.size())
			{
				log.warn("Invitation has " + elementName + 
						" parameter beyond form limit, skipping it: " + invitationEntry.getKey());
				continue;
			}
			
			if (invitationEntry.getValue().getMode() == PrefilledEntryMode.DEFAULT)
			{
				if (requested.get(invitationEntry.getKey()) == null)
					requested.set(invitationEntry.getKey(), invitationEntry.getValue().getEntry());
			} else
			{
				if (requested.get(invitationEntry.getKey()) != null)
					throw new WrongArgumentException("Registration request can not override " 
							+ elementName +	" " + invitationEntry.getKey() + 
							" specified in invitation");
				requested.set(invitationEntry.getKey(), invitationEntry.getValue().getEntry());
			}
		}
	}
	
	
	private InvitationWithCode getInvitation(String codeFromRequest, SqlSession sql) throws EngineException
	{
		try
		{
			return invitationDB.get(codeFromRequest, sql);
		} catch (WrongArgumentException e)
		{
			throw new WrongArgumentException("The provided registration code is invalid", e);
		}
	}
	
	private void validateRequestCode(RegistrationForm form, RegistrationRequest request)
			throws WrongArgumentException
	{
		String formCode = form.getRegistrationCode();
		String code = request.getRegistrationCode();
		if (formCode != null && code == null)
			throw new WrongArgumentException("This registration "
					+ "form require a registration code.");
		if (formCode != null && code != null && !formCode.equals(code))
			throw new WrongArgumentException("The registration code is invalid.");
	}

	private void validateParamsCount(List<? extends RegistrationParam> paramDefinitions,
			List<?> params, String info) throws WrongArgumentException
	{
		if (paramDefinitions.size() != params.size())
			throw new WrongArgumentException("There should be "
					+ paramDefinitions.size() + " " + info + " parameters");
	}	
}
