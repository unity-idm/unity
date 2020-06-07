/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;

class InvitationValidator
{
	static void validate(InvitationParam invitation, BaseForm form)
	{
		assertIdentitiesMatch(invitation, form);
		assertAttributesMatch(invitation, form);
		assertPrefilledGroupsMatch(invitation, form);
	}

	private static void assertPrefilledGroupsMatch(InvitationParam invitation, BaseForm form)
	{
		int maxIndex = form.getGroupParams().size()-1;
		invitation.getGroupSelections().forEach((index, param) ->
		{
			if (index > maxIndex)
				throw new IllegalArgumentException("Prefilled group index " + index 
						+ " has no corresponding group parameter in the form");
			GroupRegistrationParam groupRegistrationParam = form.getGroupParams().get(index);
			if (!groupRegistrationParam.isMultiSelect() && param.getEntry().getSelectedGroups().size() > 1)
				throw new IllegalArgumentException("Prefilled group with index " + index 
						+ " has multiple groups selected while only one is allowed.");
			for (String prefilledGroup: param.getEntry().getSelectedGroups())
				if (!GroupPatternMatcher.matches(prefilledGroup, groupRegistrationParam.getGroupPath()))
					throw new IllegalArgumentException(
						"Prefilled group " + prefilledGroup + " is not matching allowed groups spec " 
								+ groupRegistrationParam.getGroupPath());
		});
	}

	private static void assertAttributesMatch(InvitationParam invitation, BaseForm form)
	{
		int maxIndex = form.getAttributeParams().size()-1;
		invitation.getAttributes().forEach((index, param) ->
		{
			if (index > maxIndex)
				throw new IllegalArgumentException("Prefilled attribute index " + index 
						+ " has no corresponding attribute parameter in the form");
			AttributeRegistrationParam attributeRegistrationParam = form.getAttributeParams().get(index);
			if (!attributeRegistrationParam.getAttributeType().equals(param.getEntry().getName()))
				throw new IllegalArgumentException("Prefilled attribute at index " + index 
						+ " has other attribute then the one in the form: " 
						+ param.getEntry().getName() + " while expected " 
						+ attributeRegistrationParam.getAttributeType());
		});
	}

	private static void assertIdentitiesMatch(InvitationParam invitation, BaseForm form)
	{
		int maxIndex = form.getIdentityParams().size()-1;
		invitation.getIdentities().forEach((index, param) ->
		{
			if (index > maxIndex)
				throw new IllegalArgumentException("Prefilled identity index " + index 
						+ " has no corresponding identity parameter in the form");
			IdentityRegistrationParam identityRegistrationParam = form.getIdentityParams().get(index);
			if (!identityRegistrationParam.getIdentityType().equals(param.getEntry().getTypeId()))
				throw new IllegalArgumentException("Prefilled identity index " + index 
						+ " has different type then the form's param: " 
						+ param.getEntry().getTypeId() + ", while expected: " 
						+ identityRegistrationParam.getIdentityType());
		});
	}
}
