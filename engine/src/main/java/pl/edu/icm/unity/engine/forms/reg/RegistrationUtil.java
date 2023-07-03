/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import static pl.edu.icm.unity.engine.forms.reg.RegistrationRequestPreprocessor.applyContextGroupToAttributeIfNeeded;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;

class RegistrationUtil
{
	static List<Attribute> getPrefilledAndHiddenAttributes(FormPrefill invitation,
			RegistrationForm invitationRegistrationForm) throws IllegalFormContentsException
	{
		List<Attribute> attributes = Lists.newArrayList();

		if (invitation.getAttributes() == null || invitation.getAttributes().isEmpty())
			return attributes;
		
		Map<String, Integer> wildcardToGroupParamIndex = new HashMap<>();
		int j=0;
		for (GroupRegistrationParam groupParam: invitationRegistrationForm.getGroupParams())
			wildcardToGroupParamIndex.put(groupParam.getGroupPath(), j++);

		for (int i = 0; i < invitationRegistrationForm.getAttributeParams().size(); i++)
		{
			PrefilledEntry<Attribute> prefilled = invitation.getAttributes().get(i);
			if (prefilled == null)
				continue;
			
			List<String> values = prefilled.getEntry().getValues();
			if (prefilled.getMode() ==  PrefilledEntryMode.HIDDEN
					&& values != null && !values.isEmpty())
			{
				Attribute attr = prefilled.getEntry();
				applyContextGroupToAttributeIfNeeded(attr, invitationRegistrationForm, i, 
						groupResolver(invitation), wildcardToGroupParamIndex);
				attributes.add(attr);
			}
		}
		return attributes;
	}

	private static Function<Integer, GroupSelection> groupResolver(FormPrefill invitation)
	{
		return idx -> 
		{
			PrefilledEntry<GroupSelection> prefilled = invitation.getGroupSelections().get(idx);
			if (prefilled == null)
				return null;
			return prefilled.getEntry();
		};
	}

	static List<GroupParam> getPrefilledAndHiddenGroups(FormPrefill invitation,
			RegistrationForm invitationRegistrationForm, String profileName)
	{
		List<GroupParam> prefilledGroups = Lists.newArrayList();
		
		if (invitation.getGroupSelections() == null || invitation.getGroupSelections().isEmpty())
			return prefilledGroups;
		
		for (int idx = 0; idx < invitation.getGroupSelections().size(); ++idx)
		{
			PrefilledEntry<GroupSelection> prefilled = invitation.getGroupSelections().get(idx);
			if (prefilled.getMode() ==  PrefilledEntryMode.HIDDEN
					&& !prefilled.getEntry().getSelectedGroups().isEmpty())
			{
				for (String group: prefilled.getEntry().getSelectedGroups())
					prefilledGroups.add(new GroupParam(group, null, profileName));
			}
		}
		return prefilledGroups;
	}
	

	private RegistrationUtil()
	{
	}
}
