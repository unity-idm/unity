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
 * Complete invitation as stored in the system. 
 * In the first place contains a registration code associated with an {@link InvitationParam}. 
 * What's more information on sent invitations is maintained.
 *   
 * @author Krzysztof Benedyczak
 */
public class InvitationWithCode extends InvitationParam
{
	private String registrationCode;
	private Instant lastSentTime;
	private int numberOfSends;

	public InvitationWithCode(String formId, Instant expiration, String contactAddress,
			String facilityId, String registrationCode)
	{
		super(formId, expiration, contactAddress, facilityId);
		this.registrationCode = registrationCode;
	}

	public InvitationWithCode(InvitationParam base, String registrationCode)
	{
		super(base.getFormId(), base.getExpiration(), base.getContactAddress(), base.getChannelId());
		this.registrationCode = registrationCode;
		this.getAttributes().putAll(base.getAttributes());
		this.getIdentities().putAll(base.getIdentities());
		this.getGroupSelections().putAll(base.getGroupSelections());
	}

	public String getRegistrationCode()
	{
		return registrationCode;
	}

	public Instant getLastSentTime()
	{
		return lastSentTime;
	}

	public int getNumberOfSends()
	{
		return numberOfSends;
	}
	
	public void setLastSentTime(Instant lastSentTime)
	{
		this.lastSentTime = lastSentTime;
	}

	public void setNumberOfSends(int numberOfSends)
	{
		this.numberOfSends = numberOfSends;
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
		result = prime * result + ((lastSentTime == null) ? 0 : lastSentTime.hashCode());
		result = prime * result + numberOfSends;
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
		if (lastSentTime == null)
		{
			if (other.lastSentTime != null)
				return false;
		} else if (!lastSentTime.equals(other.lastSentTime))
			return false;
		if (numberOfSends != other.numberOfSends)
			return false;
		if (registrationCode == null)
		{
			if (other.registrationCode != null)
				return false;
		} else if (!registrationCode.equals(other.registrationCode))
			return false;
		return true;
	}
}








