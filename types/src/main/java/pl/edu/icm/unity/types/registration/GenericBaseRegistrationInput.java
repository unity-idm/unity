/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Base of enquiry and registration requests. Used for both engine and rest variants
 * 
 * @author K. Benedyczak
 */
public class GenericBaseRegistrationInput
{
	private String formId;
	private List<IdentityParam> identities = new ArrayList<>();
	private List<CredentialParamValue> credentials = new ArrayList<>();
	private List<Selection> groupSelections = new ArrayList<>();
	private List<Selection> agreements = new ArrayList<>();
	private String comments;
	private String userLocale;
	
	public void validate()
	{
		if (formId == null)
			throw new IllegalStateException("Form id must be set");
	}
	
	public String getFormId()
	{
		return formId;
	}

	public void setFormId(String formId)
	{
		this.formId = formId;
	}
	public List<IdentityParam> getIdentities()
	{
		return identities;
	}
	public void setIdentities(List<IdentityParam> identities)
	{
		this.identities = identities;
	}

	public List<CredentialParamValue> getCredentials()
	{
		return credentials;
	}

	public void setCredentials(List<CredentialParamValue> credentials)
	{
		this.credentials = credentials;
	}

	public List<Selection> getGroupSelections()
	{
		return groupSelections;
	}

	public void setGroupSelections(List<Selection> groupSelections)
	{
		this.groupSelections = groupSelections;
	}

	public List<Selection> getAgreements()
	{
		return agreements;
	}

	public void setAgreements(List<Selection> agreements)
	{
		this.agreements = agreements;
	}

	public String getComments()
	{
		return comments;
	}

	public void setComments(String comments)
	{
		this.comments = comments;
	}

	public String getUserLocale()
	{
		return userLocale;
	}

	public void setUserLocale(String userLocale)
	{
		this.userLocale = userLocale;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((agreements == null) ? 0 : agreements.hashCode());
		result = prime * result + ((comments == null) ? 0 : comments.hashCode());
		result = prime * result + ((credentials == null) ? 0 : credentials.hashCode());
		result = prime * result + ((formId == null) ? 0 : formId.hashCode());
		result = prime * result
				+ ((groupSelections == null) ? 0 : groupSelections.hashCode());
		result = prime * result + ((identities == null) ? 0 : identities.hashCode());
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
		GenericBaseRegistrationInput other = (GenericBaseRegistrationInput) obj;
		if (agreements == null)
		{
			if (other.agreements != null)
				return false;
		} else if (!agreements.equals(other.agreements))
			return false;
		if (comments == null)
		{
			if (other.comments != null)
				return false;
		} else if (!comments.equals(other.comments))
			return false;
		if (credentials == null)
		{
			if (other.credentials != null)
				return false;
		} else if (!credentials.equals(other.credentials))
			return false;
		if (formId == null)
		{
			if (other.formId != null)
				return false;
		} else if (!formId.equals(other.formId))
			return false;
		if (groupSelections == null)
		{
			if (other.groupSelections != null)
				return false;
		} else if (!groupSelections.equals(other.groupSelections))
			return false;
		if (identities == null)
		{
			if (other.identities != null)
				return false;
		} else if (!identities.equals(other.identities))
			return false;
		return true;
	}
}
