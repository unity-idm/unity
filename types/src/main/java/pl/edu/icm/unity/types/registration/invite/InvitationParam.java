/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Stores pre-filled input for the form which is a base of invitation of a user.
 * Invitation can optionally hold a contact address where the invitation link shall be sent.
 * @author Krzysztof Benedyczak
 */
public class InvitationParam extends InvitationParamBase
{
	private Map<Integer, PrefilledEntry<Attribute<?>>> attributes = new HashMap<>();
	
	public InvitationParam(InvitationParamBase source, Map<Integer, PrefilledEntry<Attribute<?>>> attributes)
	{
		super(source.getFormId(), source.getExpiration(), source.getContactAddress(), 
				source.getChannelId());
		getIdentities().putAll(source.getIdentities());
		getGroupSelections().putAll(source.getGroupSelections());
		this.attributes.putAll(attributes);
	}

	public InvitationParam(String formId, Instant expiration, String contactAddress, String channelId)
	{
		super(formId, expiration, contactAddress, channelId);
	}

	public InvitationParam(String formId, Instant expiration)
	{
		super(formId, expiration);
	}

	public Map<Integer, PrefilledEntry<Attribute<?>>> getAttributes()
	{
		return attributes;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		InvitationParam other = (InvitationParam) obj;
		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		return true;
	}
}








