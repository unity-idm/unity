/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.forms.BaseRequestPreprocessor;
import pl.edu.icm.unity.engine.forms.InvitationPrefillInfo;
import pl.edu.icm.unity.engine.forms.PolicyAgreementsValidator;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;

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
	private final EnquiryFormDB enquiryFormDB;
	
	@Autowired
	public EnquiryResponsePreprocessor(PolicyAgreementsValidator agreementValidator,
			BaseRequestPreprocessor baseRequestPreprocessor, EnquiryFormDB enquiryFormDB)
	{
		this.agreementValidator = agreementValidator;
		this.basePreprocessor = baseRequestPreprocessor;
		this.enquiryFormDB = enquiryFormDB;
	}

	public InvitationPrefillInfo validateSubmittedResponse(EnquiryForm form, EnquiryResponseState response,
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
		
		return invitationInfo;
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
			throws IllegalFormContentsException, IllegalFormTypeException
	{
		if (code == null)
			return null;
			
		InvitationParam invitation = basePreprocessor.getInvitation(code).getInvitation();
		
		if (!invitation.matchesForm(enquiryFormDB.get(formId)))
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
		
		
		InvitationWithCode invitationWithCode = basePreprocessor.getInvitation(codeFromRequest);
		InvitationPrefillInfo invitationInfo = new InvitationPrefillInfo(invitationWithCode);
		InvitationParam invitation = invitationWithCode.getInvitation();
		
		FormPrefill formInfo;
		try
		{
			formInfo = invitation.getPrefillForForm(form);
		} catch (EngineException e)
		{
			throw new IllegalFormContentsException("Form " + form.getName() + " not match to invitation", e);
		}
		
		basePreprocessor.processInvitationElements(form.getIdentityParams(), response.getIdentities(), 
				formInfo.getIdentities(), "identity");
		basePreprocessor.processInvitationElements(form.getAttributeParams(), response.getAttributes(), 
				formInfo.getAttributes(), "attribute");
		basePreprocessor.processInvitationElements(form.getGroupParams(), response.getGroupSelections(), 
				basePreprocessor.filterValueReadOnlyAndHiddenGroupFromInvitation(formInfo.getGroupSelections(), form.getGroupParams()), 
				"group");
		return invitationInfo;
	}
}
