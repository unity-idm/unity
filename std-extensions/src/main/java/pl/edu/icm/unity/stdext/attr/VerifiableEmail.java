/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.attr;

import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;

/**
 * Email which can be confirmed by user. 
 * 
 * @author P. Piernik
 */
public class VerifiableEmail implements VerifiableElement
{
	private String value;
	private ConfirmationInfo confirmationInfo;

	public VerifiableEmail()
	{
		this.confirmationInfo = new ConfirmationInfo();
	}

	public VerifiableEmail(String value)
	{
		this.value = value;
		this.confirmationInfo = new ConfirmationInfo();
	}

	public VerifiableEmail(String value, ConfirmationInfo confirmationData)
	{
		this.value = value;
		this.confirmationInfo = confirmationData;
	}
	@Override
	public ConfirmationInfo getConfirmationInfo()
	{
		return confirmationInfo;
	}

	public void setConfirmationInfo(ConfirmationInfo confirmationData)
	{
		this.confirmationInfo = confirmationData;
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

		if (confirmationInfo == null)
		{
			if (other.getConfirmationInfo() != null)
				return false;
		} else if (!confirmationInfo.equals(other.getConfirmationInfo()))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + confirmationInfo.hashCode();
		return result;
	}
}
