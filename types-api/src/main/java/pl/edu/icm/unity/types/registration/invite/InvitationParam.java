/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.Selection;

/**
 * Base data of invitation parameter. It is extracted as we have two ways to represent attributes:
 * one simple for JSON API and one with resolved Attribute for the backend.
 * @author Krzysztof Benedyczak
 */
public class InvitationParam
{
	private static final Logger log = LogManager.getLogger(InvitationParam.class);
	
	private String formId;
	private Instant expiration;
	private String contactAddress;
	
	private Map<Integer, PrefilledEntry<IdentityParam>> identities = new HashMap<>();
	private Map<Integer, PrefilledEntry<Selection>> groupSelections = new HashMap<>();
	private Map<Integer, PrefilledEntry<Attribute>> attributes = new HashMap<>();

	private InvitationParam() {}
	
	public InvitationParam(String formId, Instant expiration, String contactAddress)
	{
		this(formId, expiration);
		this.contactAddress = contactAddress;
	}

	public InvitationParam(String formId, Instant expiration)
	{
		this.formId = formId;
		this.expiration = expiration;
	}

	@JsonCreator
	public InvitationParam(ObjectNode json)
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

	public Map<Integer, PrefilledEntry<IdentityParam>> getIdentities()
	{
		return identities;
	}

	public Map<Integer, PrefilledEntry<Selection>> getGroupSelections()
	{
		return groupSelections;
	}

	public Map<Integer, PrefilledEntry<Attribute>> getAttributes()
	{
		return attributes;
	}

	@JsonIgnore
	public boolean isExpired()
	{
		return Instant.now().isAfter(getExpiration());
	}
	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode json = Constants.MAPPER.createObjectNode();
		
		json.put("formId", getFormId());
		json.put("expiration", getExpiration().toEpochMilli());
		if (getContactAddress() != null)
			json.put("contactAddress", getContactAddress());

		json.putPOJO("identities", getIdentities());
		json.putPOJO("groupSelections", getGroupSelections());
		json.putPOJO("attributes", getAttributes());
		return json;
	}
	
	private void fromJson(ObjectNode json)
	{
		formId = json.get("formId").asText();
		expiration = Instant.ofEpochMilli(json.get("expiration").asLong());
		contactAddress = JsonUtil.getNullable(json, "contactAddress");
		
		JsonNode n;
		
		n = json.get("identities");
		fill((ObjectNode) n, getIdentities(), IdentityParam.class);

		n = json.get("groupSelections");
		fill((ObjectNode) n, getGroupSelections(), Selection.class);

		n = json.get("attributes");
		fill((ObjectNode) n, getAttributes(), Attribute.class);
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
				+ ", contactAddress=" + contactAddress + "]";
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
	
	public static Builder builder()
	{
		return new Builder();
	}
	
	public static class Builder
	{
		private InvitationParam instance = new InvitationParam();
		
		public Builder withForm(String formId)
		{
			instance.formId = formId;
			return this;
		}
		public Builder withExpiration(Instant expiration)
		{
			instance.expiration = expiration;
			return this;
		}
		public Builder withContactAddress(String contactAddress)
		{
			instance.contactAddress = contactAddress;
			return this;
		}
		
		public InvitationParam build()
		{
			return instance;
		}
		public Builder withAttribute(Attribute attribute, PrefilledEntryMode mode)
		{
			int idx = instance.attributes.size();
			instance.attributes.put(idx, new PrefilledEntry<>(attribute, mode));
			return this;
		}
		public Builder withGroup(boolean selection, PrefilledEntryMode mode)
		{
			int idx = instance.groupSelections.size();
			instance.groupSelections.put(idx, new PrefilledEntry<>(new Selection(selection), mode));
			return this;
		}
	}
}
