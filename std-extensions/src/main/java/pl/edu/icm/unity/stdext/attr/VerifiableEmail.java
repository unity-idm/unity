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

	@Override
	public void setConfirmationInfo(ConfirmationInfo confirmationData)
	{
		this.confirmationInfo = confirmationData;
	}

	@Override
	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	@Override
	public boolean isConfirmed()
	{
		return confirmationInfo.isConfirmed();
	}
	

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		VerifiableEmail other = (VerifiableEmail) obj;
		if (value == null)
		{
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
