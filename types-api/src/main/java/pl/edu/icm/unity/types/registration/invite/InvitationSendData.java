/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.GroupSelection;

public class InvitationSendData
{
	public final String form;
	public final FormType formType;
	public final String contactAddress;
	public final Instant expiration;
	public final Map<Integer, PrefilledEntry<GroupSelection>> groupSelections;
	public final Map<String, String> messageParams;

	public InvitationSendData(String form, FormType formType, String contactAddress, Instant expiration,
			Map<Integer, PrefilledEntry<GroupSelection>> groupSelections, Map<String, String> messageParams)
	{
		this.form = form;
		this.formType = formType;
		this.contactAddress = contactAddress;
		this.groupSelections = groupSelections == null ? null : Collections.unmodifiableMap(groupSelections);
		this.messageParams = messageParams == null ? null : Collections.unmodifiableMap(messageParams);
		this.expiration = expiration;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(contactAddress, expiration, form, formType, groupSelections, messageParams);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InvitationSendData other = (InvitationSendData) obj;
		return Objects.equals(contactAddress, other.contactAddress) && Objects.equals(expiration, other.expiration)
				&& Objects.equals(form, other.form) && formType == other.formType
				&& Objects.equals(groupSelections, other.groupSelections)
				&& Objects.equals(messageParams, other.messageParams);
	}

}
