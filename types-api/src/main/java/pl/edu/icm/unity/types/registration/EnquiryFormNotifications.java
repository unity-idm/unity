/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.Objects;

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
		return Objects.hash(this.enquiryToFillTemplate, this.submittedTemplate);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final EnquiryFormNotifications other = (EnquiryFormNotifications) obj;
		return Objects.equals(this.enquiryToFillTemplate, other.enquiryToFillTemplate)
				&& Objects.equals(this.submittedTemplate, other.submittedTemplate);
	}
}
