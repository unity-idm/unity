/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeParamRepresentation;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Complete invitation as stored in the system. 
 * In the first place contains a registration code associated with an {@link InvitationParam}. 
 * What's more information on sent invitations is maintained.
 *   
 * @author Krzysztof Benedyczak
 */
public class InvitationWithCode extends InvitationWithCodeBase
{
	private Map<Integer, PrefilledEntry<Attribute<?>>> attributes = new HashMap<>();

	public InvitationWithCode(String formId, Instant expiration, String contactAddress,
			String facilityId, String registrationCode)
	{
		super(formId, expiration, contactAddress, facilityId, registrationCode);
	}

	public InvitationWithCode(InvitationParam base, String registrationCode,
			Instant lastSentTime, int numberOfSends)
	{
		super(base, registrationCode, lastSentTime, numberOfSends);
		this.getAttributes().putAll(base.getAttributes());
	}

	public InvitationWithCode(ObjectNode json, Map<Integer, PrefilledEntry<Attribute<?>>> attributes)
	{
		super(json);
		this.attributes.putAll(attributes);
	}
	
	public Map<Integer, PrefilledEntry<Attribute<?>>> getAttributes()
	{
		return attributes;
	}

	public RESTInvitationWithCode toRESTVariant()
	{
		Map<Integer, PrefilledEntry<AttributeParamRepresentation>> attributes = new HashMap<>(this.attributes.size());
		for (Map.Entry<Integer, PrefilledEntry<Attribute<?>>> entry: this.attributes.entrySet())
		{
			PrefilledEntry<Attribute<?>> value = entry.getValue();
			AttributeParamRepresentation convertedAttribute = 
					new AttributeParamRepresentation(value.getEntry()); 
			attributes.put(entry.getKey(), new PrefilledEntry<AttributeParamRepresentation>(
					convertedAttribute, value.getMode()));
		}
		return new RESTInvitationWithCode(this, attributes);
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
		InvitationWithCode other = (InvitationWithCode) obj;
		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		return true;
	}
}








