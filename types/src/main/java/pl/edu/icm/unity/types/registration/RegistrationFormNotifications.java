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
public class RegistrationFormNotifications extends BaseFormNotifications
{
	private String updatedTemplate;
	private String invitationTemplate;
	
	public String getUpdatedTemplate()
	{
		return updatedTemplate;
	}
	public void setUpdatedTemplate(String updatedTemplate)
	{
		this.updatedTemplate = updatedTemplate;
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
		int result = super.hashCode();
		result = prime
				* result
				+ ((invitationTemplate == null) ? 0 : invitationTemplate.hashCode());
		result = prime * result
				+ ((updatedTemplate == null) ? 0 : updatedTemplate.hashCode());
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
		RegistrationFormNotifications other = (RegistrationFormNotifications) obj;
		if (invitationTemplate == null)
		{
			if (other.invitationTemplate != null)
				return false;
		} else if (!invitationTemplate.equals(other.invitationTemplate))
			return false;
		if (updatedTemplate == null)
		{
			if (other.updatedTemplate != null)
				return false;
		} else if (!updatedTemplate.equals(other.updatedTemplate))
			return false;
		return true;
	}
}
