/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Configuration of notifications related to all kinds of forms.
 * 
 * @author K. Benedyczak
 */
public class BaseFormNotifications
{
	private String rejectedTemplate;
	private String acceptedTemplate;
	
	private String channel;
	private String adminsNotificationGroup;
	
	public String getChannel()
	{
		return channel;
	}
	public void setChannel(String channel)
	{
		this.channel = channel;
	}
	public String getAdminsNotificationGroup()
	{
		return adminsNotificationGroup;
	}
	public void setAdminsNotificationGroup(String adminsNotificationGroup)
	{
		this.adminsNotificationGroup = adminsNotificationGroup;
	}
	public String getRejectedTemplate()
	{
		return rejectedTemplate;
	}
	public void setRejectedTemplate(String rejectedTemplate)
	{
		this.rejectedTemplate = rejectedTemplate;
	}
	public String getAcceptedTemplate()
	{
		return acceptedTemplate;
	}
	public void setAcceptedTemplate(String acceptedTemplate)
	{
		this.acceptedTemplate = acceptedTemplate;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((acceptedTemplate == null) ? 0 : acceptedTemplate.hashCode());
		result = prime
				* result
				+ ((adminsNotificationGroup == null) ? 0 : adminsNotificationGroup
						.hashCode());
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		result = prime * result
				+ ((rejectedTemplate == null) ? 0 : rejectedTemplate.hashCode());
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
		BaseFormNotifications other = (BaseFormNotifications) obj;
		if (acceptedTemplate == null)
		{
			if (other.acceptedTemplate != null)
				return false;
		} else if (!acceptedTemplate.equals(other.acceptedTemplate))
			return false;
		if (adminsNotificationGroup == null)
		{
			if (other.adminsNotificationGroup != null)
				return false;
		} else if (!adminsNotificationGroup.equals(other.adminsNotificationGroup))
			return false;
		if (channel == null)
		{
			if (other.channel != null)
				return false;
		} else if (!channel.equals(other.channel))
			return false;
		if (rejectedTemplate == null)
		{
			if (other.rejectedTemplate != null)
				return false;
		} else if (!rejectedTemplate.equals(other.rejectedTemplate))
			return false;
		return true;
	}
}
