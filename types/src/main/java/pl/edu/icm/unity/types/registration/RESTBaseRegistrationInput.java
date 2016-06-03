/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.types.basic.AttributeParamRepresentation;

/**
 * Base of enquiry and registration requests, useful for client side (attributes are not resolved).
 * 
 * @author K. Benedyczak
 */
public class RESTBaseRegistrationInput extends GenericBaseRegistrationInput
{
	private List<AttributeParamRepresentation> attributes = new ArrayList<>();

	public List<AttributeParamRepresentation> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(List<AttributeParamRepresentation> attributes)
	{
		this.attributes = attributes;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
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
		RESTBaseRegistrationInput other = (RESTBaseRegistrationInput) obj;
		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		return true;
	}
}
