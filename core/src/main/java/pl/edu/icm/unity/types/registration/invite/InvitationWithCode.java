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
}








