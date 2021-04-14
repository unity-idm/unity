/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.forms.BaseRequestPreprocessor;
import pl.edu.icm.unity.engine.forms.InvitationPrefillInfo;
import pl.edu.icm.unity.engine.forms.PolicyAgreementsValidator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;

/**
 * Helper component with methods to validate {@link EnquiryResponse}.
 */
@Component
public class EnquiryResponsePreprocessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FORMS,
			EnquiryResponsePreprocessor.class);
	
	private final PolicyAgreementsValidator agreementValidator;
	private final BaseRequestPreprocessor basePreprocessor;
	
	@Autowired
	public EnquiryResponsePreprocessor(PolicyAgreementsValidator agreementValidator,
			BaseRequestPreprocessor baseRequestPreprocessor)
	{
		this.agreementValidator = agreementValidator;
		this.basePreprocessor = baseRequestPreprocessor;
	}

	public void validateSubmittedResponse(EnquiryForm form, EnquiryResponseState response,
			boolean doCredentialCheckAndUpdate) throws IllegalFormContentsException
	{	
		InvitationPrefillInfo invitationInfo = getInvitationPrefillInfo(form, response.getRequest());

		basePreprocessor.validateSubmittedRequest(form, response.getRequest(), doCredentialCheckAndUpdate);
		agreementValidator.validate(new EntityParam(response.getEntityId()), form, response.getRequest());
		
		if (invitationInfo.isByInvitation())
		{
			String code = response.getRequest().getRegistrationCode();
			log.info("Received enquiry response for invitation " + code + ", removing it");
			basePreprocessor.removeInvitation(code);
		}
	}

	public void validateTranslatedRequest(EnquiryForm form, EnquiryResponse response, 
			TranslatedRegistrationRequest request) throws EngineException
	{
		basePreprocessor.validateFinalAttributes(request.getAttributes());
		basePreprocessor.validateFinalCredentials(response.getCredentials());
		validateFinalIdentities(request.getIdentities());
		basePreprocessor.validateFinalGroups(request.getGroups());
	}

	private void validateFinalIdentities(Collection<IdentityParam> identities) 
			throws EngineException
	{
		for (IdentityParam idParam: identities)
		{
			if (idParam.getTypeId() == null || idParam.getValue() == null)
				throw new WrongArgumentException("Identity " + idParam + " contains null values");
			basePreprocessor.identityTypesRegistry.getByName(idParam.getTypeId()).validate(idParam.getValue());
			basePreprocessor.assertIdentityIsNotPresentOnConfirm(idParam);
		}
	}
	
	public Long getEntityFromInvitationAndValidateCode(String formId, String code) 
			throws IllegalFormContentsException
	{
		if (code == null)
			return null;
			
		InvitationParam invitation = basePreprocessor.getInvitation(code).getInvitation();
		
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
		
		InvitationParam invitation = basePreprocessor.getInvitation(codeFromRequest).getInvitation();
		basePreprocessor.processInvitationElements(form.getIdentityParams(), response.getIdentities(), 
				invitation.getIdentities(), "identity");
		basePreprocessor.processInvitationElements(form.getAttributeParams(), response.getAttributes(), 
				invitation.getAttributes(), "attribute");
		basePreprocessor.processInvitationElements(form.getGroupParams(), response.getGroupSelections(), 
				basePreprocessor.filterValueReadOnlyAndHiddenGroupFromInvitation(invitation.getGroupSelections(), form.getGroupParams()), 
				"group");
		return invitationInfo;
	}
}
