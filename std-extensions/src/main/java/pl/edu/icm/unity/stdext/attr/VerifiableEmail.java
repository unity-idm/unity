/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.attr;

import pl.edu.icm.unity.types.VerifiableElement;
import pl.edu.icm.unity.types.basic.ConfirmationData;

/**
 * Email with verification state and verification date
 * 
 * @author P. Piernik
 */
public class VerifiableEmail implements VerifiableElement
{
	private String value;
	private ConfirmationData confirmationData;

	public VerifiableEmail()
	{
		this.confirmationData = new ConfirmationData();
	}

	public VerifiableEmail(String value)
	{
		this.value = value;
		this.confirmationData = new ConfirmationData();
	}

	public VerifiableEmail(String value, ConfirmationData confirmationData)
	{
		this.value = value;
		this.confirmationData = confirmationData;
	}
	@Override
	public ConfirmationData getConfirmationData()
	{
		return confirmationData;
	}

	public void setConfirmationData(ConfirmationData confirmationData)
	{
		this.confirmationData = confirmationData;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof VerifiableEmail))
			return false;
		VerifiableEmail other = (VerifiableEmail) obj;
		if (value == null)
		{
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;

		if (confirmationData == null)
		{
			if (other.getConfirmationData() != null)
				return false;
		} else if (!confirmationData.equals(other.getConfirmationData()))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + confirmationData.hashCode();
		return result;
	}
}
