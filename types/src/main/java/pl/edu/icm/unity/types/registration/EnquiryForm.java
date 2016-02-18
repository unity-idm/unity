/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.Arrays;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Configuration of an enquiry form. Enquiry form is used to retrieve information
 * from an existing user.
 * Instances of this class can be built either from JSON or manually.
 * 
 * @author K. Benedyczak
 */
public class EnquiryForm extends BaseForm
{
	public enum EnquiryType {REQUESTED_MANDATORY, REQUESTED_OPTIONAL}
	
	private EnquiryType type;
	private String[] targetGroups;
	private EnquiryFormNotifications notificationsConfiguration;
	
	@JsonCreator
	public EnquiryForm(ObjectNode json)
	{
		super(json);
		fromJson(json);
	}
	
	public EnquiryForm()
	{
	}
	
	
	@Override
	public String toString()
	{
		return "EnquiryForm [name=" + name + "]";
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = super.toJson();
		ObjectMapper jsonMapper = Constants.MAPPER;
		root.put("type", type.name());
		ArrayNode targetGroupsA = root.putArray("targetGroups");
		for (String targetGroup: targetGroups)
			targetGroupsA.add(targetGroup);
		root.set("NotificationsConfiguration", jsonMapper.valueToTree(getNotificationsConfiguration()));
		return root;
	}

	private void fromJson(ObjectNode root)
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		try
		{
			JsonNode n = root.get("type");
			this.type = EnquiryType.valueOf(n.asText());
			
			ArrayNode targetGroupsA = root.withArray("targetGroups");
			this.targetGroups = new String[targetGroupsA.size()];
			for (int i=0; i<targetGroups.length; i++)
				targetGroups[i] = targetGroupsA.get(i).asText();
			
			n = root.get("NotificationsConfiguration");
			if (n != null)
			{
				String v = jsonMapper.writeValueAsString(n);
				EnquiryFormNotifications r = jsonMapper.readValue(v, EnquiryFormNotifications.class);
				setNotificationsConfiguration(r);
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize enquiry form from JSON", e);
		}
	}

	public EnquiryFormNotifications getNotificationsConfiguration()
	{
		return notificationsConfiguration;
	}


	public void setNotificationsConfiguration(EnquiryFormNotifications notificationsConfiguration)
	{
		this.notificationsConfiguration = notificationsConfiguration;
	}

	public EnquiryType getType()
	{
		return type;
	}

	public void setType(EnquiryType type)
	{
		this.type = type;
	}

	public String[] getTargetGroups()
	{
		return targetGroups;
	}

	public void setTargetGroups(String[] targetGroups)
	{
		this.targetGroups = targetGroups;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((notificationsConfiguration == null) ? 0
						: notificationsConfiguration.hashCode());
		result = prime * result + Arrays.hashCode(targetGroups);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		EnquiryForm other = (EnquiryForm) obj;
		if (notificationsConfiguration == null)
		{
			if (other.notificationsConfiguration != null)
				return false;
		} else if (!notificationsConfiguration.equals(other.notificationsConfiguration))
			return false;
		if (!Arrays.equals(targetGroups, other.targetGroups))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
}
