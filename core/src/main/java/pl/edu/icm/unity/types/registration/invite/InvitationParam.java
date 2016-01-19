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
}








