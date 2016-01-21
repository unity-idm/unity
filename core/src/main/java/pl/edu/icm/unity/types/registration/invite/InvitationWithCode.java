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

/**
 * Stores a registration code associated with an {@link InvitationParam}.
 *   
 * @author Krzysztof Benedyczak
 */
public class InvitationWithCode extends InvitationParam
{
	private String registrationCode;

	public InvitationWithCode(String formId, Instant expiration, String contactAddress,
			String facilityId, String registrationCode)
	{
		super(formId, expiration, contactAddress, facilityId);
		this.registrationCode = registrationCode;
	}

	public InvitationWithCode(InvitationParam base, String registrationCode)
	{
		super(base.getFormId(), base.getExpiration(), base.getContactAddress(), base.getFacilityId());
		this.registrationCode = registrationCode;
		this.getAttributes().putAll(base.getAttributes());
		this.getIdentities().putAll(base.getIdentities());
		this.getGroupSelections().putAll(base.getGroupSelections());
	}

	public String getRegistrationCode()
	{
		return registrationCode;
	}

	@Override
	public String toString()
	{
		return super.toString() + " [registrationCode=" + registrationCode + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((registrationCode == null) ? 0 : registrationCode.hashCode());
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
		InvitationWithCode other = (InvitationWithCode) obj;
		if (registrationCode == null)
		{
			if (other.registrationCode != null)
				return false;
		} else if (!registrationCode.equals(other.registrationCode))
			return false;
		return true;
	}
}








