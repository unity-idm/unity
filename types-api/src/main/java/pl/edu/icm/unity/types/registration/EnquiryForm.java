/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;

/**
 * Configuration of an enquiry form. Enquiry form is used to retrieve information
 * from an existing user.
 * Instances of this class can be built either from JSON or manually.
 * 
 * @author K. Benedyczak
 */
public class EnquiryForm extends BaseForm
{
	public enum EnquiryType {REQUESTED_MANDATORY, REQUESTED_OPTIONAL, STICKY}
	
	private EnquiryType type;
	private String[] targetGroups;
	private String targetCondition;
	private EnquiryFormNotifications notificationsConfiguration = new EnquiryFormNotifications();
	private FormLayout layout;
	
	@JsonCreator
	public EnquiryForm(ObjectNode json)
	{
		super(json);
		fromJson(json);
		validateLayout();
	}
	
	public void validateLayout()
	{
		if (getLayout() != null)
			FormLayoutUtils.validateEnquiryLayout(this);
	}
	
	public EnquiryForm()
	{
	}
	
	
	@Override
	public String toString()
	{
		return "EnquiryForm [name=" + name + "]";
	}

	@Override
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
		if (layout != null)
		{
			root.set("FormLayout", getLayout().toJson());
		}
		
		if (targetCondition != null)
		{
			root.put("targetCondition", targetCondition);
		} 
		
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
			
			n = root.get("FormLayout");
			if (n != null)
			{
				setLayout(new FormLayout((ObjectNode) n));
			}
		
			n = root.get("targetCondition");
			if (n != null)
			{
				this.targetCondition = n.asText();
			}
			
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize enquiry form from JSON", e);
		}
	}

	@Override
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
	
	public String getTargetCondition()
	{
		return targetCondition;
	}

	public void setTargetCondition(String targetCondition)
	{
		this.targetCondition = targetCondition;
	}
	
	public FormLayout getLayout()
	{
		return layout;
	}

	public void setLayout(FormLayout layout)
	{
		this.layout = layout;
	}

	public FormLayout getEffectiveFormLayout(MessageSource msg)
	{
		if (layout == null)
			return getDefaultFormLayout(msg);
		return layout;
	}
	
	public FormLayout getDefaultFormLayout(MessageSource msg)
	{
		List<FormElement> elements = FormLayoutUtils.getDefaultFormLayoutElements(this, msg);
		return new FormLayout(elements);
	}
	
	protected void validateEnquiry()
	{
		super.validate();
		if (type == null)
			throw new IllegalStateException("Enquiry type must be not-null");
		if (targetGroups == null)
			throw new IllegalStateException("Enquiry target groups can not be null");
	}

	@Override
	public boolean equals(final Object other)
	{
		if (this == other)
			return true;
		if (!(other instanceof EnquiryForm))
			return false;
		if (!super.equals(other))
			return false;
		EnquiryForm castOther = (EnquiryForm) other;
		return Objects.equals(type, castOther.type) && Arrays.equals(targetGroups, castOther.targetGroups)
				&& Objects.equals(notificationsConfiguration, castOther.notificationsConfiguration)
				&& Objects.equals(layout, castOther.layout)&& Objects.equals(targetCondition, castOther.targetCondition);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), type, targetGroups, notificationsConfiguration, layout, targetCondition);
	}
}
