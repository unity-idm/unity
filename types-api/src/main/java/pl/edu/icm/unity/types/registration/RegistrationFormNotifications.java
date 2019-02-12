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
	private String submittedTemplate;
	
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
				+ ((submittedTemplate == null) ? 0 : submittedTemplate.hashCode());
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
		if (submittedTemplate == null)
		{
			if (other.submittedTemplate != null)
				return false;
		} else if (!submittedTemplate.equals(other.submittedTemplate))
			return false;
		return true;
	}
}
