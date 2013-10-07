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
}
