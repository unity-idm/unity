/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Base class of registration parameters which can be set as optional.
 * @author K. Benedyczak
 */
public class OptionalRegistrationParam extends RegistrationParam
{
	private boolean optional;

	public boolean isOptional()
	{
		return optional;
	}
	public void setOptional(boolean optional)
	{
		this.optional = optional;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (optional ? 1231 : 1237);
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
		OptionalRegistrationParam other = (OptionalRegistrationParam) obj;
		if (optional != other.optional)
			return false;
		return true;
	}
}
