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

import org.apache.log4j.Logger;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.Selection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base data of invitation parameter. It is extracted as we have two ways to represent attributes:
 * one simple for JSON API and one with resolved Attribute for the backend.
 * @author Krzysztof Benedyczak
 */
abstract class InvitationParamBase
{
	private static final Logger log = Logger.getLogger(InvitationParamBase.class);
	
	private String formId;
	private Instant expiration;
	private String contactAddress;
	private String channelId;
	
	private Map<Integer, PrefilledEntry<IdentityParam>> identities = new HashMap<>();
	private Map<Integer, PrefilledEntry<Selection>> groupSelections = new HashMap<>();
	
	public InvitationParamBase(String formId, Instant expiration, String contactAddress, String channelId)
	{
		this(formId, expiration);
		this.contactAddress = contactAddress;
		this.channelId = channelId;
	}

	public InvitationParamBase(String formId, Instant expiration)
	{
		this.formId = formId;
		this.expiration = expiration;
	}

	public InvitationParamBase(ObjectNode json)
	{
		fromJson(json);
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

	public String getChannelId()
	{
		return channelId;
	}

	public Map<Integer, PrefilledEntry<IdentityParam>> getIdentities()
	{
		return identities;
	}

	public Map<Integer, PrefilledEntry<Selection>> getGroupSelections()
	{
		return groupSelections;
	}

	@JsonIgnore
	public boolean isExpired()
	{
		return Instant.now().isAfter(getExpiration());
	}
	
	public ObjectNode toJson()
	{
		ObjectNode json = Constants.MAPPER.createObjectNode();
		
		json.put("formId", getFormId());
		json.put("expiration", getExpiration().getEpochSecond());
		if (getContactAddress() != null)
			json.put("contactAddress", getContactAddress());
		if (getChannelId() != null)
			json.put("channelId", getChannelId());

		json.putPOJO("identities", getIdentities());
		json.putPOJO("groupSelections", getGroupSelections());
		return json;
	}
	
	private void fromJson(ObjectNode json)
	{
		formId = json.get("formId").asText();
		expiration = Instant.ofEpochSecond(json.get("expiration").asLong());
		contactAddress = json.has("contactAddress") ? json.get("contactAddress").asText() : null;
		channelId = json.has("channelId") ? json.get("channelId").asText() : null;
		
		JsonNode n;
		
		n = json.get("identities");
		fill((ObjectNode) n, getIdentities(), IdentityParam.class);

		n = json.get("groupSelections");
		fill((ObjectNode) n, getGroupSelections(), Selection.class);
	}

	protected <T> void fill(ObjectNode root, Map<Integer, PrefilledEntry<T>> map, Class<T> clazz)
	{
		root.fields().forEachRemaining(field ->
		{
			ObjectNode el = (ObjectNode) field.getValue();
			try
			{
				map.put(Integer.parseInt(field.getKey()), 
						toPrefilledEntry(el, clazz));
			} catch (Exception e)
			{
				log.warn("Ignoring unparsable prefilled invitation entry", e);
				return;
			}
		});
	}
	
	private <T> PrefilledEntry<T> toPrefilledEntry(ObjectNode el, Class<T> clazz) throws JsonProcessingException
	{
		T value = Constants.MAPPER.treeToValue(el.get("entry"), clazz);
		PrefilledEntryMode mode = PrefilledEntryMode.valueOf(el.get("mode").asText());
		return new PrefilledEntry<>(value, mode);
	}

	
	@Override
	public String toString()
	{
		return "InvitationParam [formId=" + formId + ", expiration=" + expiration
				+ ", contactAddress=" + contactAddress + ", channelId="
				+ channelId + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contactAddress == null) ? 0 : contactAddress.hashCode());
		result = prime * result + ((expiration == null) ? 0 : expiration.hashCode());
		result = prime * result + ((channelId == null) ? 0 : channelId.hashCode());
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
		InvitationParamBase other = (InvitationParamBase) obj;
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
		if (channelId == null)
		{
			if (other.channelId != null)
				return false;
		} else if (!channelId.equals(other.channelId))
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
