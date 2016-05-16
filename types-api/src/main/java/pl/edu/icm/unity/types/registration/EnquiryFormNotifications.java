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
public class EnquiryFormNotifications extends BaseFormNotifications
{
	private String enquiryToFillTemplate;
	private String submittedTemplate;
	
	public String getEnquiryToFillTemplate()
	{
		return enquiryToFillTemplate;
	}
	public void setEnquiryToFillTemplate(String enquiryToFillTemplate)
	{
		this.enquiryToFillTemplate = enquiryToFillTemplate;
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
				+ ((enquiryToFillTemplate == null) ? 0 : enquiryToFillTemplate
						.hashCode());
		result = prime * result
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
		EnquiryFormNotifications other = (EnquiryFormNotifications) obj;
		if (enquiryToFillTemplate == null)
		{
			if (other.enquiryToFillTemplate != null)
				return false;
		} else if (!enquiryToFillTemplate.equals(other.enquiryToFillTemplate))
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
