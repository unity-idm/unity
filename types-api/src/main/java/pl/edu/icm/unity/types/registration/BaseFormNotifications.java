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
	private String updatedTemplate;
	private String invitationTemplate;
	
	private String adminsNotificationGroup;
	private boolean sendUserNotificationCopyToAdmin;
	
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
	public String getUpdatedTemplate()
	{
		return updatedTemplate;
	}
	public void setUpdatedTemplate(String updatedTemplate)
	{
		this.updatedTemplate = updatedTemplate;
	}
	public boolean isSendUserNotificationCopyToAdmin()
	{
		return sendUserNotificationCopyToAdmin;
	}
	public void setSendUserNotificationCopyToAdmin(boolean sendUserNotificationCopyToAdmin)
	{
		this.sendUserNotificationCopyToAdmin = sendUserNotificationCopyToAdmin;
	}
	public String getInvitationTemplate()
	{
		return invitationTemplate;
	}
	public void setInvitationTemplate(String invitationTemplate)
	{
		this.invitationTemplate = invitationTemplate;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((acceptedTemplate == null) ? 0 : acceptedTemplate.hashCode());
		result = prime * result + ((adminsNotificationGroup == null) ? 0
				: adminsNotificationGroup.hashCode());
		result = prime * result
				+ ((rejectedTemplate == null) ? 0 : rejectedTemplate.hashCode());
		result = prime * result + (sendUserNotificationCopyToAdmin ? 1231 : 1237);
		result = prime * result
				+ ((updatedTemplate == null) ? 0 : updatedTemplate.hashCode());
		result = prime * result
				+ ((invitationTemplate == null) ? 0 : invitationTemplate.hashCode());
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
		if (rejectedTemplate == null)
		{
			if (other.rejectedTemplate != null)
				return false;
		} else if (!rejectedTemplate.equals(other.rejectedTemplate))
			return false;
		if (sendUserNotificationCopyToAdmin != other.sendUserNotificationCopyToAdmin)
			return false;
		if (updatedTemplate == null)
		{
			if (other.updatedTemplate != null)
				return false;
		} else if (!updatedTemplate.equals(other.updatedTemplate))
			return false;
		if (invitationTemplate == null)
		{
			if (other.invitationTemplate != null)
				return false;
		} else if (!invitationTemplate.equals(other.invitationTemplate))
			return false;
		
		return true;
	}
}
