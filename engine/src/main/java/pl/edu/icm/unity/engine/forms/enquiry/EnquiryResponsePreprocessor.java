/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import java.util.Collection;
import java.util.Comparator;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.forms.BaseRequestPreprocessor;
import pl.edu.icm.unity.engine.forms.InvitationPrefillInfo;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;

/**
 * Helper component with methods to validate {@link EnquiryResponse}.
 * 
 * @author K. Benedyczak
 */
@Component
public class EnquiryResponsePreprocessor extends BaseRequestPreprocessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER,
			EnquiryResponsePreprocessor.class);
	
	public void validateSubmittedResponse(EnquiryForm form, EnquiryResponse response,
			boolean doCredentialCheckAndUpdate) throws IllegalFormContentsException
	{	
		InvitationPrefillInfo invitationInfo = getInvitationPrefillInfo(form, response);

		super.validateSubmittedRequest(form, response, invitationInfo, doCredentialCheckAndUpdate);

		if (invitationInfo.isByInvitation())
		{
			String code = response.getRegistrationCode();
			log.debug("Received enquiry response for invitation " + code + ", removing it");
			removeInvitation(code);
		}
	}

	public void validateTranslatedRequest(EnquiryForm form, EnquiryResponse response, 
			TranslatedRegistrationRequest request) throws EngineException
	{
		validateFinalAttributes(request.getAttributes());
		validateFinalCredentials(response.getCredentials());
		validateFinalIdentities(request.getIdentities());
		validateFinalGroups(request.getGroups());
	}

	@Override
	protected void validateFinalIdentities(Collection<IdentityParam> identities) 
			throws EngineException
	{
		for (IdentityParam idParam: identities)
		{
			if (idParam.getTypeId() == null || idParam.getValue() == null)
				throw new WrongArgumentException("Identity " + idParam + " contains null values");
			identityTypesRegistry.getByName(idParam.getTypeId()).validate(idParam.getValue());
			checkIdentityIsNotPresent(idParam);
		}
	}
	
	public Long getEntityFromInvitationAndValidateCode(String formId, String code) 
			throws IllegalFormContentsException
	{
		if (code == null)
		{
			return null;
		}
			
		InvitationParam invitation = getInvitation(code).getInvitation();
		
		if (!invitation.getFormId().equals(formId))
			throw new IllegalFormContentsException("The invitation is for different enquiry form");
		
		if (invitation.isExpired())
			throw new IllegalFormContentsException("The invitation has already expired");
		
		EnquiryInvitationParam enquiryInvitation = (EnquiryInvitationParam) invitation;
		if (enquiryInvitation.getEntity() == null)
			throw new IllegalFormContentsException("The invitation has no entity set");
		
		return enquiryInvitation.getEntity();
	}
	
	private InvitationPrefillInfo getInvitationPrefillInfo(EnquiryForm form, EnquiryResponse response) throws IllegalFormContentsException
	{
		String codeFromRequest = response.getRegistrationCode();
		if (codeFromRequest == null)
		{
			return new InvitationPrefillInfo();
		}
		InvitationPrefillInfo invitationInfo = new InvitationPrefillInfo(true);
		
		InvitationParam invitation = getInvitation(codeFromRequest).getInvitation();
		processInvitationElements(form.getIdentityParams(), response.getIdentities(), 
				invitation.getIdentities(), "identity", Comparator.comparing(IdentityParam::getValue),
				invitationInfo::setPrefilledIdentity);
		processInvitationElements(form.getAttributeParams(), response.getAttributes(), 
				invitation.getAttributes(), "attribute", null,
				invitationInfo::setPrefilledAttribute);
		processInvitationElements(form.getGroupParams(), response.getGroupSelections(), 
				filterValueReadOnlyAndHiddenGroupFromInvitation(invitation.getGroupSelections(), form.getGroupParams()), "group", null,
				i -> {});
		return invitationInfo;
	}
}
