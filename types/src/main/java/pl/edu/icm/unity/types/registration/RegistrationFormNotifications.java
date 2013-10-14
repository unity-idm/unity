/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Configuration of the notifications which should be sent when form's requests are submitted or processed.
 * 
 * @author K. Benedyczak
 */
public class RegistrationFormNotifications
{
	private String submittedTemplate;
	private String adminCommentTemplate;
	private String publicCommentTemplate;
	private String rejectedTemplate;
	private String acceptedTemplate;
	
	private String channel;
	private String adminsNotificationGroup;
	
	public String getSubmittedTemplate()
	{
		return submittedTemplate;
	}
	public void setSubmittedTemplate(String submittedTemplate)
	{
		this.submittedTemplate = submittedTemplate;
	}
	public String getAdminCommentTemplate()
	{
		return adminCommentTemplate;
	}
	public void setAdminCommentTemplate(String adminCommentTemplate)
	{
		this.adminCommentTemplate = adminCommentTemplate;
	}
	public String getPublicCommentTemplate()
	{
		return publicCommentTemplate;
	}
	public void setPublicCommentTemplate(String publicCommentTemplate)
	{
		this.publicCommentTemplate = publicCommentTemplate;
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
		result = prime * result
				+ ((acceptedTemplate == null) ? 0 : acceptedTemplate.hashCode());
		result = prime
				* result
				+ ((adminCommentTemplate == null) ? 0 : adminCommentTemplate
						.hashCode());
		result = prime
				* result
				+ ((adminsNotificationGroup == null) ? 0 : adminsNotificationGroup
						.hashCode());
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		result = prime
				* result
				+ ((publicCommentTemplate == null) ? 0 : publicCommentTemplate
						.hashCode());
		result = prime * result
				+ ((rejectedTemplate == null) ? 0 : rejectedTemplate.hashCode());
		result = prime * result
				+ ((submittedTemplate == null) ? 0 : submittedTemplate.hashCode());
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
		RegistrationFormNotifications other = (RegistrationFormNotifications) obj;
		if (acceptedTemplate == null)
		{
			if (other.acceptedTemplate != null)
				return false;
		} else if (!acceptedTemplate.equals(other.acceptedTemplate))
			return false;
		if (adminCommentTemplate == null)
		{
			if (other.adminCommentTemplate != null)
				return false;
		} else if (!adminCommentTemplate.equals(other.adminCommentTemplate))
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
		if (publicCommentTemplate == null)
		{
			if (other.publicCommentTemplate != null)
				return false;
		} else if (!publicCommentTemplate.equals(other.publicCommentTemplate))
			return false;
		if (rejectedTemplate == null)
		{
			if (other.rejectedTemplate != null)
				return false;
		} else if (!rejectedTemplate.equals(other.rejectedTemplate))
			return false;
		if (submittedTemplate == null)
		{
			if (other.submittedTemplate != null)
				return false;
		} else if (!submittedTemplate.equals(other.submittedTemplate))
			return false;
		return true;
	}
}
