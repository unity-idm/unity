/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import java.util.List;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;

class RegistrationUtil
{
	static List<Attribute> getPrefilledAndHiddenAttributes(InvitationWithCode invitation)
	{
		List<Attribute> attributes = Lists.newArrayList();

		if (invitation.getAttributes() == null || invitation.getAttributes().isEmpty())
			return attributes;

		for (PrefilledEntry<Attribute> prefilled : invitation.getAttributes().values())
		{
			List<String> values = prefilled.getEntry().getValues();
			if (prefilled.getMode() ==  PrefilledEntryMode.HIDDEN
					&& values != null && !values.isEmpty())
			{
				attributes.add(prefilled.getEntry());
			}
		}
		return attributes;
	}

	static List<GroupParam> getPrefilledAndHiddenGroups(InvitationWithCode invitation,
			RegistrationForm invitationRegistrationForm)
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
					prefilledGroups.add(new GroupParam(group, null, null));
			}
		}
		return prefilledGroups;
	}
	

	private RegistrationUtil()
	{
	}
}
