/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.List;

/**
 * Type of identity. This class uses {@link IdentityTypeDefinition} implementation and there is always 
 * 1-1 relationship between them. This class adds stateful configuration which can be freely 
 * modified by administrator.
 * @author K. Benedyczak
 */
public class IdentityType
{
	private IdentityTypeDefinition identityTypeProvider;
	private String description;
	private List<String> extractedAttributes;

	public IdentityType(IdentityTypeDefinition identityTypeProvider)
	{
		this.identityTypeProvider = identityTypeProvider;
		this.description = identityTypeProvider.getDefaultDescription();
		this.extractedAttributes = identityTypeProvider.getAttributesSupportedForExtraction();
	}
	
	public IdentityType(IdentityTypeDefinition identityTypeProvider, String description,
			List<String> extractedAttributes)
	{
		this.identityTypeProvider = identityTypeProvider;
		this.description = description;
		this.extractedAttributes = extractedAttributes;
	}

	public IdentityTypeDefinition getIdentityTypeProvider()
	{
		return identityTypeProvider;
	}

	public String getDescription()
	{
		return description;
	}

	public List<String> getExtractedAttributes()
	{
		return extractedAttributes;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public void setExtractedAttributes(List<String> extractedAttributes)
	{
		this.extractedAttributes = extractedAttributes;
	}
	
	public String toString()
	{
		return "[" + getIdentityTypeProvider().toString() + "] " + description;
	}
}

