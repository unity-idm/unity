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
	private String invitationTemplate;
	private String submittedTemplate;
	
	public String getInvitationTemplate()
	{
		return invitationTemplate;
	}
	public void setInvitationTemplate(String invitationTemplate)
	{
		this.invitationTemplate = invitationTemplate;
	}
	public String getSubmittedTemplate()
	{
		return submittedTemplate;
	}
	public void setSubmittedTemplate(String submittedTemplate)
	{
		this.submittedTemplate = submittedTemplate;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((invitationTemplate == null) ? 0 : invitationTemplate.hashCode());
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
		return true;
	}
}
