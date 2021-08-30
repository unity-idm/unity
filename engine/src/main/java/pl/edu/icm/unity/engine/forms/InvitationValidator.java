/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;

import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.ComboInvitationParam;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

@Component
class InvitationValidator implements pl.edu.icm.unity.types.registration.invite.InvitationValidator
{
	private final RegistrationFormDB registrationDB;
	private final EnquiryFormDB enquiryDB;

	@Autowired
	public InvitationValidator(RegistrationFormDB registrationDB, EnquiryFormDB enquiryDB)
	{
		this.registrationDB = registrationDB;
		this.enquiryDB = enquiryDB;
	}

	public void validateUpdate(RegistrationInvitationParam currentRegistrationInvitationParam,
			InvitationParam newRegistrationInvitationParam) throws WrongArgumentException
	{

		validateType(currentRegistrationInvitationParam, newRegistrationInvitationParam);
		RegistrationInvitationParam newInv = (RegistrationInvitationParam) newRegistrationInvitationParam;
		if (!Objects.equal(currentRegistrationInvitationParam.getFormPrefill().getFormId(),
				newInv.getFormPrefill().getFormId()))
			throw new WrongArgumentException("Can not update form of an invitation");
	}

	public void validateUpdate(EnquiryInvitationParam currentInvitationParam, InvitationParam newInvitationParam)
			throws WrongArgumentException
	{
		validateType(currentInvitationParam, newInvitationParam);
		EnquiryInvitationParam newInv = (EnquiryInvitationParam) newInvitationParam;
		if (!Objects.equal(currentInvitationParam.getFormPrefill().getFormId(),
				newInv.getFormPrefill().getFormId()))
			throw new WrongArgumentException("Can not update form of an invitation");
	}

	public void validateUpdate(ComboInvitationParam currentInvitationParam, InvitationParam newInvitationParam)
			throws WrongArgumentException
	{
		validateType(currentInvitationParam, newInvitationParam);
		ComboInvitationParam newInv = (ComboInvitationParam) newInvitationParam;

		if (!Objects.equal(currentInvitationParam.getRegistrationFormPrefill().getFormId(),
				newInv.getRegistrationFormPrefill().getFormId()))
			throw new WrongArgumentException("Can not update form of an invitation");

		if (!Objects.equal(currentInvitationParam.getEnquiryFormPrefill().getFormId(),
				newInv.getEnquiryFormPrefill().getFormId()))
			throw new WrongArgumentException("Can not update form of an invitation");
	}

	private void validateType(InvitationParam currentInvitationParam, InvitationParam newInvitationParam)
			throws WrongArgumentException
	{
		if (!Objects.equal(currentInvitationParam.getType(), newInvitationParam.getType()))
			throw new WrongArgumentException("Can not update type of invitation");
	}

	@Override
	public void validate(RegistrationInvitationParam invitationParam) throws EngineException
	{
		if (invitationParam.getFormPrefill().getFormId() == null)
		{
			throw new WrongArgumentException("The invitation has no form configured");
		}

		RegistrationForm form = registrationDB.get(invitationParam.getFormPrefill().getFormId());
		if (!form.isPubliclyAvailable())
			throw new WrongArgumentException("Invitations can be attached to public forms only");
		if (form.getRegistrationCode() != null)
			throw new WrongArgumentException("Invitations can not be attached to forms with a fixed registration code");
		validate(invitationParam.getFormPrefill(), form);

	}

	@Override
	public void validate(EnquiryInvitationParam invitationParam) throws EngineException
	{
		if (invitationParam.getFormPrefill().getFormId() == null)
		{
			throw new WrongArgumentException("The invitation has no form configured");
		}
		EnquiryForm enquiryForm = enquiryDB.get(invitationParam.getFormPrefill().getFormId());
		validate(invitationParam.getFormPrefill(), enquiryForm);
	}

	@Override
	public void validate(ComboInvitationParam invitationParam) throws EngineException
	{
		if (invitationParam.getRegistrationFormPrefill().getFormId() == null
				|| invitationParam.getEnquiryFormPrefill().getFormId() == null)
		{
			throw new WrongArgumentException("The invitation has no form configured");
		}
		EnquiryForm enquiryForm = enquiryDB.get(invitationParam.getEnquiryFormPrefill().getFormId());
		validate(invitationParam.getEnquiryFormPrefill(), enquiryForm);
		RegistrationForm form = registrationDB.get(invitationParam.getRegistrationFormPrefill().getFormId());
		validate(invitationParam.getRegistrationFormPrefill(), form);
	}

	private void validate(FormPrefill invitation, BaseForm form)
	{
		assertIdentitiesMatch(invitation, form);
		assertAttributesMatch(invitation, form);
		assertPrefilledGroupsMatch(invitation, form);
	}

	private void assertPrefilledGroupsMatch(FormPrefill invitation, BaseForm form)
	{
		int maxIndex = form.getGroupParams().size() - 1;
		invitation.getGroupSelections().forEach((index, param) ->
		{
			if (index > maxIndex)
				throw new IllegalArgumentException(
						"Prefilled group index " + index + " has no corresponding group parameter in the form");
			GroupRegistrationParam groupRegistrationParam = form.getGroupParams().get(index);
			if (!groupRegistrationParam.isMultiSelect() && param.getEntry().getSelectedGroups().size() > 1)
				throw new IllegalArgumentException("Prefilled group with index " + index
						+ " has multiple groups selected while only one is allowed.");
			for (String prefilledGroup : param.getEntry().getSelectedGroups())
				if (!GroupPatternMatcher.matches(prefilledGroup, groupRegistrationParam.getGroupPath()))
					throw new IllegalArgumentException("Prefilled group " + prefilledGroup
							+ " is not matching allowed groups spec " + groupRegistrationParam.getGroupPath());
		});
	}

	private void assertAttributesMatch(FormPrefill invitation, BaseForm form)
	{
		int maxIndex = form.getAttributeParams().size() - 1;
		invitation.getAttributes().forEach((index, param) ->
		{
			if (index > maxIndex)
				throw new IllegalArgumentException(
						"Prefilled attribute index " + index + " has no corresponding attribute parameter in the form");
			AttributeRegistrationParam attributeRegistrationParam = form.getAttributeParams().get(index);
			if (!attributeRegistrationParam.getAttributeType().equals(param.getEntry().getName()))
				throw new IllegalArgumentException("Prefilled attribute at index " + index
						+ " has other attribute then the one in the form: " + param.getEntry().getName()
						+ " while expected " + attributeRegistrationParam.getAttributeType());
		});
	}

	private void assertIdentitiesMatch(FormPrefill invitation, BaseForm form)
	{
		int maxIndex = form.getIdentityParams().size() - 1;
		invitation.getIdentities().forEach((index, param) ->
		{
			if (index > maxIndex)
				throw new IllegalArgumentException(
						"Prefilled identity index " + index + " has no corresponding identity parameter in the form");
			IdentityRegistrationParam identityRegistrationParam = form.getIdentityParams().get(index);
			if (!identityRegistrationParam.getIdentityType().equals(param.getEntry().getTypeId()))
				throw new IllegalArgumentException("Prefilled identity index " + index
						+ " has different type then the form's param: " + param.getEntry().getTypeId()
						+ ", while expected: " + identityRegistrationParam.getIdentityType());
		});
	}

}
