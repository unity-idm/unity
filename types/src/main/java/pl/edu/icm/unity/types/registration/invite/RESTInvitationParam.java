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
 * Stores pre-filled input for the form which is a base of invitation of a user.
 * Invitation can optionally hold a contact address where the invitation link shall be sent.
 * <p>
 * This variant is intended for usage with REST API and is serializable to/from JSON.
 * 
 * @author Krzysztof Benedyczak
 */
public class RESTInvitationParam extends InvitationParamBase
{
	private Map<Integer, PrefilledEntry<AttributeParamRepresentation>> attributes = new HashMap<>();
	
	public RESTInvitationParam(String formId, Instant expiration, String contactAddress, String channelId)
	{
		super(formId, expiration, contactAddress, channelId);
	}

	public RESTInvitationParam(String formId, Instant expiration)
	{
		super(formId, expiration);
	}

	@JsonCreator
	public RESTInvitationParam(ObjectNode json)
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
		RESTInvitationParam other = (RESTInvitationParam) obj;
		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		return true;
	}
}








