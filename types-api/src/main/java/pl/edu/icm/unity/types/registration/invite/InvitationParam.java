/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.invite;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.GroupSelection;

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
	private Map<Integer, PrefilledEntry<GroupSelection>> groupSelections = new HashMap<>();
	private Map<Integer, PrefilledEntry<Attribute>> attributes = new HashMap<>();
	private Map<String, String> messageParams = new HashMap<>();
	private ExpectedIdentity expectedIdentity;

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

	public Map<Integer, PrefilledEntry<GroupSelection>> getGroupSelections()
	{
		return groupSelections;
	}

	public Map<Integer, PrefilledEntry<Attribute>> getAttributes()
	{
		return attributes;
	}

	public Map<String, String> getMessageParams()
	{
		return messageParams;
	}

	public ExpectedIdentity getExpectedIdentity()
	{
		return expectedIdentity;
	}

	public void setExpectedIdentity(ExpectedIdentity expectedIdentity)
	{
		this.expectedIdentity = expectedIdentity;
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
		if (getExpectedIdentity() != null)
			json.putPOJO("expectedIdentity", expectedIdentity);
		
		json.putPOJO("identities", getIdentities());
		json.putPOJO("groupSelections", getGroupSelections());
		json.putPOJO("attributes", getAttributes());
		json.putPOJO("messageParams", getMessageParams());
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
		fill((ObjectNode) n, getGroupSelections(), GroupSelection.class);

		n = json.get("attributes");
		fill((ObjectNode) n, getAttributes(), Attribute.class);
		
		n = json.get("messageParams");
		if (n != null)
		{
			n.fields().forEachRemaining(field ->
			{
				try
				{
					messageParams.put(field.getKey(), field.getValue().asText());
				} catch (Exception e)
				{
					log.warn("Ignoring unparsable message parameter", e);
				}
			});
		}
		
		n = json.get("expectedIdentity");
		if (n != null)
			expectedIdentity = Constants.MAPPER.convertValue(n, ExpectedIdentity.class);
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
	public boolean equals(final Object other)
	{
		if (!(other instanceof InvitationParam))
			return false;
		InvitationParam castOther = (InvitationParam) other;
		return Objects.equals(formId, castOther.formId) && Objects.equals(expiration, castOther.expiration)
				&& Objects.equals(contactAddress, castOther.contactAddress)
				&& Objects.equals(identities, castOther.identities)
				&& Objects.equals(groupSelections, castOther.groupSelections)
				&& Objects.equals(attributes, castOther.attributes)
				&& Objects.equals(messageParams, castOther.messageParams)
				&& Objects.equals(expectedIdentity, castOther.expectedIdentity);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(formId, expiration, contactAddress, identities, groupSelections, attributes,
				messageParams, expectedIdentity);
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
		public Builder withGroup(String group, PrefilledEntryMode mode)
		{
			int idx = instance.groupSelections.size();
			instance.groupSelections.put(idx, new PrefilledEntry<>(new GroupSelection(group), mode));
			return this;
		}
		public Builder withGroups(List<String> groups, PrefilledEntryMode mode)
		{
			int idx = instance.groupSelections.size();
			instance.groupSelections.put(idx, new PrefilledEntry<>(new GroupSelection(groups), mode));
			return this;
		}
		public Builder withIdentity(IdentityParam identity, PrefilledEntryMode mode)
		{
			int idx = instance.identities.size();
			instance.identities.put(idx, new PrefilledEntry<>(identity, mode));
			return this;
		}
	}
}
