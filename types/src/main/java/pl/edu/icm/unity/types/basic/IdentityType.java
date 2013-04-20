/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.HashMap;
import java.util.Map;

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
	private Map<String, String> extractedAttributes;

	public IdentityType(IdentityTypeDefinition identityTypeProvider)
	{
		this.identityTypeProvider = identityTypeProvider;
		this.description = identityTypeProvider.getDefaultDescription();
		setExtractedAttributes(new HashMap<String, String>());
	}
	
	public IdentityType(IdentityTypeDefinition identityTypeProvider, String description,
			Map<String, String> extractedAttributes)
	{
		this.identityTypeProvider = identityTypeProvider;
		this.description = description;
		setExtractedAttributes(extractedAttributes);
	}

	public IdentityTypeDefinition getIdentityTypeProvider()
	{
		return identityTypeProvider;
	}

	public String getDescription()
	{
		return description;
	}

	public Map<String, String> getExtractedAttributes()
	{
		return extractedAttributes;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public void setExtractedAttributes(Map<String, String> extractedAttributes)
	{
		this.extractedAttributes = new HashMap<String, String>();
		this.extractedAttributes.putAll(extractedAttributes);
	}
	
	public String toString()
	{
		return "[" + getIdentityTypeProvider().toString() + "] " + description;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((identityTypeProvider == null) ? 0 : identityTypeProvider
						.hashCode());
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
		IdentityType other = (IdentityType) obj;
		if (identityTypeProvider == null)
		{
			if (other.identityTypeProvider != null)
				return false;
		} else if (!identityTypeProvider.equals(other.identityTypeProvider))
			return false;
		return true;
	}
}

