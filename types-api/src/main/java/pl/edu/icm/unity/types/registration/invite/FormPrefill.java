
/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.registration.invite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.GroupSelection;

public class FormPrefill
{
	private static final Logger log = LogManager.getLogger(FormPrefill.class);
	
	private String formId;
	private FormType formType;
	private Map<Integer, PrefilledEntry<IdentityParam>> identities = new HashMap<>();
	private Map<Integer, PrefilledEntry<GroupSelection>> groupSelections = new HashMap<>();
	private Map<Integer, GroupSelection> allowedGroups = new HashMap<>();
	private Map<Integer, PrefilledEntry<Attribute>> attributes = new HashMap<>();
	private Map<String, String> messageParams = new HashMap<>();

	public FormPrefill()
	{
	}

	public FormPrefill(String formId, FormType formType)
	{
		this.formId = formId;
		this.formType = formType;
	}
	
	public FormPrefill(FormType formType)
	{
		
		this.formType = formType;
	}
	
	public String getFormId()
	{
		return formId;
	}

	@JsonIgnore
	public void setFormId(String formId)
	{
		this.formId = formId;
	}
	
	@JsonIgnore
	public FormType getFormType()
	{
		return formType;
	}

	public void setFormType(FormType formType)
	{
		this.formType = formType;
	}

	public Map<Integer, PrefilledEntry<IdentityParam>> getIdentities()
	{
		return identities;
	}

	public void setIdentities(Map<Integer, PrefilledEntry<IdentityParam>> identities)
	{
		this.identities = identities;
	}

	public Map<Integer, PrefilledEntry<GroupSelection>> getGroupSelections()
	{
		return groupSelections;
	}

	public void setGroupSelections(Map<Integer, PrefilledEntry<GroupSelection>> groupSelections)
	{
		this.groupSelections = groupSelections;
	}

	public Map<Integer, GroupSelection> getAllowedGroups()
	{
		return allowedGroups;
	}

	public void setAllowedGroups(Map<Integer, GroupSelection> allowedGroups)
	{
		this.allowedGroups = allowedGroups;
	}

	public Map<Integer, PrefilledEntry<Attribute>> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(Map<Integer, PrefilledEntry<Attribute>> attributes)
	{
		this.attributes = attributes;
	}

	public Map<String, String> getMessageParams()
	{
		return messageParams;
	}

	public void setMessageParams(Map<String, String> messageParams)
	{
		this.messageParams = messageParams;
	}

	public void fill(FormPrefill from)
	{
		getGroupSelections().putAll(from.getGroupSelections());
		getAllowedGroups().putAll(from.getAllowedGroups());
		getAttributes().putAll(from.getAttributes());
		getIdentities().putAll(from.getIdentities());
		getMessageParams().putAll(from.getMessageParams());	
	}
	
	
	@Override
	public int hashCode()
	{
		return Objects.hash(allowedGroups, attributes, formId, formType, groupSelections, identities, messageParams);
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
		FormPrefill other = (FormPrefill) obj;
		return Objects.equals(allowedGroups, other.allowedGroups) && Objects.equals(attributes, other.attributes)
				&& Objects.equals(formId, other.formId) && formType == other.formType
				&& Objects.equals(groupSelections, other.groupSelections)
				&& Objects.equals(identities, other.identities) && Objects.equals(messageParams, other.messageParams);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public Map<String, Object> getMessageParamsWithCustomVarObject(String customVarPrefix)
	{
		Map<String, Object> ret = new HashMap<String, Object>();
		Map<String, String> custom = new HashMap<String, String>();

		for (Map.Entry<String, String> entry : messageParams.entrySet())
		{
			if (entry.getKey().startsWith(customVarPrefix))
			{
				custom.put(entry.getKey().substring(customVarPrefix.length()), entry.getValue());
			}
		}

		ret.put(customVarPrefix.substring(0, customVarPrefix.length() - 1), custom);

		return ret;

	}

	void toJson(ObjectNode json)
	{
		json.put("formId", getFormId());
		json.putPOJO("identities", getIdentities());
		json.putPOJO("groupSelections", getGroupSelections());
		json.putPOJO("allowedGroups", getAllowedGroups());
		json.putPOJO("attributes", getAttributes());
		json.putPOJO("messageParams", getMessageParams());
	}

	void fromJson(ObjectNode json)
	{

		JsonNode n;

		formId = json.get("formId").asText();

		n = json.get("identities");
		fill((ObjectNode) n, getIdentities(), IdentityParam.class);

		n = json.get("groupSelections");
		fill((ObjectNode) n, getGroupSelections(), GroupSelection.class);

		n = json.get("allowedGroups");
		if (n != null && !n.isNull())
			fill((ObjectNode) n, getAllowedGroups());

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
	}

	private void fill(ObjectNode root, Map<Integer, GroupSelection> allowedGroups)
	{
		root.fields().forEachRemaining(field ->
		{
			try
			{
				allowedGroups.put(Integer.parseInt(field.getKey()),
						Constants.MAPPER.treeToValue(field.getValue(), GroupSelection.class));
			} catch (Exception e)
			{
				log.warn("Ignoring unparsable prefilled invitation entry", e);
				return;
			}
		});

	}

	protected <T> void fill(ObjectNode root, Map<Integer, PrefilledEntry<T>> map, Class<T> clazz)
	{
		root.fields().forEachRemaining(field ->
		{
			ObjectNode el = (ObjectNode) field.getValue();
			try
			{
				map.put(Integer.parseInt(field.getKey()), toPrefilledEntry(el, clazz));
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

	public static class Builder
	{

		private FormPrefill instance;
		
		public Builder()
		{
			instance = new FormPrefill();
		}

		public FormPrefill build()
		{
			return instance;
		}

		
		public FormPrefill.Builder withForm(String formId)
		{
			instance.formId = formId;
			return this;
		}

		public FormPrefill.Builder withAttribute(Attribute attribute, PrefilledEntryMode mode)
		{
			int idx = instance.attributes.size();
			instance.attributes.put(idx, new PrefilledEntry<>(attribute, mode));
			return this;
		}

		public FormPrefill.Builder withGroup(String group, PrefilledEntryMode mode)
		{
			int idx = instance.groupSelections.size();
			instance.groupSelections.put(idx, new PrefilledEntry<>(new GroupSelection(group), mode));
			return this;
		}

		public FormPrefill.Builder withGroups(List<String> groups, PrefilledEntryMode mode)
		{
			int idx = instance.groupSelections.size();
			instance.groupSelections.put(idx, new PrefilledEntry<>(new GroupSelection(groups), mode));
			return this;
		}

		public FormPrefill.Builder withAllowedGroups(List<String> groups)
		{
			int idx = instance.allowedGroups.size();
			instance.allowedGroups.put(idx, new GroupSelection(groups));
			return this;
		}

		public FormPrefill.Builder withIdentity(IdentityParam identity, PrefilledEntryMode mode)
		{
			int idx = instance.identities.size();
			instance.identities.put(idx, new PrefilledEntry<>(identity, mode));
			return this;
		}
	}
}
