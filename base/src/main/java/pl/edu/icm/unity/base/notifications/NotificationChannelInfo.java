/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.notifications;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Information returned about an installed notification channel 
 */
public class NotificationChannelInfo extends NotificationChannel
{
	private boolean supportingTemplates;
	
	public NotificationChannelInfo(NotificationChannel base, boolean supportingTemplates)
	{
		super(base.getName(), base.getDescription(), base.getConfiguration(), base.getFacilityId());
		this.supportingTemplates = supportingTemplates;
	}

	public NotificationChannelInfo()
	{
	}

	@JsonCreator
	public NotificationChannelInfo(ObjectNode root)
	{
		super(root);
		fromJson(root);
	}

	public boolean isSupportingTemplates()
	{
		return supportingTemplates;
	}

	private void fromJson(ObjectNode root)
	{
		supportingTemplates = root.get("supportingTemplates").asBoolean();
	}
	
	@JsonValue
	@Override
	public ObjectNode toJson()
	{
		ObjectNode root = super.toJson();
		root.put("supportingTemplates", isSupportingTemplates());
		return root;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(supportingTemplates);
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
		NotificationChannelInfo other = (NotificationChannelInfo) obj;
		return supportingTemplates == other.supportingTemplates;
	}

	@Override
	public String toString()
	{
		return "NotificationChannelInfo [supportingTemplates=" + supportingTemplates + ", name=" + name
				+ ", description=" + description + "]";
	}
}
