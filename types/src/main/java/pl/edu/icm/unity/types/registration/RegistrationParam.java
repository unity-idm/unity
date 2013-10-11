/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Base class of registration parameters
 * @author K. Benedyczak
 */
public class RegistrationParam
{
	private String label;
	private String description;
	private boolean optional;
	private ParameterRetrievalSettings retrievalSettings;

	public String getLabel()
	{
		return label;
	}
	public void setLabel(String label)
	{
		this.label = label;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public boolean isOptional()
	{
		return optional;
	}
	public void setOptional(boolean optional)
	{
		this.optional = optional;
	}
	public ParameterRetrievalSettings getRetrievalSettings()
	{
		return retrievalSettings;
	}
	public void setRetrievalSettings(ParameterRetrievalSettings retrievalSettings)
	{
		this.retrievalSettings = retrievalSettings;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + (optional ? 1231 : 1237);
		result = prime * result
				+ ((retrievalSettings == null) ? 0 : retrievalSettings.hashCode());
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
		RegistrationParam other = (RegistrationParam) obj;
		if (description == null)
		{
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (label == null)
		{
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (optional != other.optional)
			return false;
		if (retrievalSettings != other.retrievalSettings)
			return false;
		return true;
	}
}
