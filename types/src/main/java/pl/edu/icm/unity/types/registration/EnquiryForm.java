/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
	private boolean optional;
	private String targetUsersExpression;
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
		root.put("optional", optional);
		root.put("targetUsersExpression", targetUsersExpression);
		root.set("NotificationsConfiguration", jsonMapper.valueToTree(getNotificationsConfiguration()));
		return root;
	}

	private void fromJson(ObjectNode root)
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		try
		{
			JsonNode n = root.get("optional");
			this.optional = n.asBoolean();
			
			this.targetUsersExpression = root.get("targetUsersExpression").asText();
			
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

	public boolean isOptional()
	{
		return optional;
	}

	public String getTargetUsersExpression()
	{
		return targetUsersExpression;
	}

	public EnquiryFormNotifications getNotificationsConfiguration()
	{
		return notificationsConfiguration;
	}

	public void setOptional(boolean optional)
	{
		this.optional = optional;
	}

	public void setTargetUsersExpression(String targetUsersExpression)
	{
		this.targetUsersExpression = targetUsersExpression;
	}

	public void setNotificationsConfiguration(EnquiryFormNotifications notificationsConfiguration)
	{
		this.notificationsConfiguration = notificationsConfiguration;
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
		result = prime * result + (optional ? 1231 : 1237);
		result = prime
				* result
				+ ((targetUsersExpression == null) ? 0 : targetUsersExpression
						.hashCode());
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
		if (optional != other.optional)
			return false;
		if (targetUsersExpression == null)
		{
			if (other.targetUsersExpression != null)
				return false;
		} else if (!targetUsersExpression.equals(other.targetUsersExpression))
			return false;
		return true;
	}
}
