/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.Selection;

/**
 * Stores pre-filled input for the form which is a base of invitation of a user.
 * Invitation can optionally hold a contact address where the invitation link shall be sent.
 *   
 * @author Krzysztof Benedyczak
 */
public class InvitationParam
{
	private String formId;
	private Instant expiration;
	private String contactAddress;
	private String facilityId;
	
	private Map<Integer, PrefilledEntry<IdentityParam>> identities = new HashMap<>();
	private Map<Integer, PrefilledEntry<Attribute<?>>> attributes = new HashMap<>();
	private Map<Integer, PrefilledEntry<Selection>> groupSelections = new HashMap<>();
	
	public InvitationParam(String formId, Instant expiration, String contactAddress, String facilityId)
	{
		this(formId, expiration);
		this.contactAddress = contactAddress;
		this.facilityId = facilityId;
	}

	public InvitationParam(String formId, Instant expiration)
	{
		this.formId = formId;
		this.expiration = expiration;
	}

	public String getFormId()
	{
		return formId;
	}

	public Instant getExpiration()
	{
		return expiration;
	}

	public String getContactAddress()
	{
		return contactAddress;
	}

	public String getFacilityId()
	{
		return facilityId;
	}

	public Map<Integer, PrefilledEntry<IdentityParam>> getIdentities()
	{
		return identities;
	}

	public Map<Integer, PrefilledEntry<Attribute<?>>> getAttributes()
	{
		return attributes;
	}

	public Map<Integer, PrefilledEntry<Selection>> getGroupSelections()
	{
		return groupSelections;
	}

	public boolean isExpired()
	{
		return Instant.now().isAfter(getExpiration());
	}
	
	@Override
	public String toString()
	{
		return "InvitationParam [formId=" + formId + ", expiration=" + expiration
				+ ", contactAddress=" + contactAddress + ", facilityId="
				+ facilityId + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result
				+ ((contactAddress == null) ? 0 : contactAddress.hashCode());
		result = prime * result + ((expiration == null) ? 0 : expiration.hashCode());
		result = prime * result + ((facilityId == null) ? 0 : facilityId.hashCode());
		result = prime * result + ((formId == null) ? 0 : formId.hashCode());
		result = prime * result
				+ ((groupSelections == null) ? 0 : groupSelections.hashCode());
		result = prime * result + ((identities == null) ? 0 : identities.hashCode());
		return result;
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
		InvitationParam other = (InvitationParam) obj;
		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (contactAddress == null)
		{
			if (other.contactAddress != null)
				return false;
		} else if (!contactAddress.equals(other.contactAddress))
			return false;
		if (expiration == null)
		{
			if (other.expiration != null)
				return false;
		} else if (!expiration.equals(other.expiration))
			return false;
		if (facilityId == null)
		{
			if (other.facilityId != null)
				return false;
		} else if (!facilityId.equals(other.facilityId))
			return false;
		if (formId == null)
		{
			if (other.formId != null)
				return false;
		} else if (!formId.equals(other.formId))
			return false;
		if (groupSelections == null)
		{
			if (other.groupSelections != null)
				return false;
		} else if (!groupSelections.equals(other.groupSelections))
			return false;
		if (identities == null)
		{
			if (other.identities != null)
				return false;
		} else if (!identities.equals(other.identities))
			return false;
		return true;
	}
}








