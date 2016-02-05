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

import pl.edu.icm.unity.types.basic.AttributeParamRepresentation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Complete invitation as stored in the system. 
 * In the first place contains a registration code associated with an {@link InvitationParam}. 
 * What's more information on sent invitations is maintained.
 * <p>
 * This variant is intended for use with REST API and is JSON serializable
 * @author Krzysztof Benedyczak
 */
public class RESTInvitationWithCode extends InvitationWithCodeBase
{
	private Map<Integer, PrefilledEntry<AttributeParamRepresentation>> attributes = new HashMap<>();

	public RESTInvitationWithCode(String formId, Instant expiration, String contactAddress,
			String facilityId, String registrationCode)
	{
		super(formId, expiration, contactAddress, facilityId, registrationCode);
	}

	public RESTInvitationWithCode(RESTInvitationParam base, String registrationCode,
			Instant lastSentTime, int numberOfSends)
	{
		super(base, registrationCode, lastSentTime, numberOfSends);
		this.getAttributes().putAll(base.getAttributes());
	}

	public RESTInvitationWithCode(InvitationWithCodeBase base,
			Map<Integer, PrefilledEntry<AttributeParamRepresentation>> attributes)
	{
		super(base, base.getRegistrationCode(), base.getLastSentTime(), base.getNumberOfSends());
		this.getAttributes().putAll(attributes);
	}
	
	@JsonCreator
	public RESTInvitationWithCode(ObjectNode json)
	{
		super(json);
		fromJson(json);
	}
	
	
	public Map<Integer, PrefilledEntry<AttributeParamRepresentation>> getAttributes()
	{
		return attributes;
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode json = super.toJson();
		json.putPOJO("attributes", getAttributes());
		return json;
	}

	private void fromJson(ObjectNode json)
	{
		JsonNode n = json.get("attributes");
		fill((ObjectNode) n, getAttributes(), AttributeParamRepresentation.class);
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
		RESTInvitationWithCode other = (RESTInvitationWithCode) obj;
		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		return true;
	}
}








