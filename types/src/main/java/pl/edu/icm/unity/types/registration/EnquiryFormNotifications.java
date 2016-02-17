/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Configuration of notifications related to enquiry forms.
 * 
 * @author K. Benedyczak
 */
public class EnquiryFormNotifications
{
	private String enquiryToFillTemplate;
	private String enquiryFilledTemplate;
	
	private String channel;
	private String adminsNotificationGroup;
	
	
	public String getEnquiryToFillTemplate()
	{
		return enquiryToFillTemplate;
	}
	public void setEnquiryToFillTemplate(String enquiryToFillTemplate)
	{
		this.enquiryToFillTemplate = enquiryToFillTemplate;
	}
	public String getEnquiryFilledTemplate()
	{
		return enquiryFilledTemplate;
	}
	public void setEnquiryFilledTemplate(String enquiryFilledTemplate)
	{
		this.enquiryFilledTemplate = enquiryFilledTemplate;
	}
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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((adminsNotificationGroup == null) ? 0 : adminsNotificationGroup
						.hashCode());
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		result = prime
				* result
				+ ((enquiryFilledTemplate == null) ? 0 : enquiryFilledTemplate
						.hashCode());
		result = prime
				* result
				+ ((enquiryToFillTemplate == null) ? 0 : enquiryToFillTemplate
						.hashCode());
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
		EnquiryFormNotifications other = (EnquiryFormNotifications) obj;
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
		if (enquiryFilledTemplate == null)
		{
			if (other.enquiryFilledTemplate != null)
				return false;
		} else if (!enquiryFilledTemplate.equals(other.enquiryFilledTemplate))
			return false;
		if (enquiryToFillTemplate == null)
		{
			if (other.enquiryToFillTemplate != null)
				return false;
		} else if (!enquiryToFillTemplate.equals(other.enquiryToFillTemplate))
			return false;
		return true;
	}
}
