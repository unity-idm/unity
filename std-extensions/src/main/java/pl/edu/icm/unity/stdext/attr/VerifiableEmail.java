/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.attr;

import java.util.Date;

import pl.edu.icm.unity.confirmations.VerifiableElement;


/**
 * Email with verification state and verification date
 * @author P. Piernik
 */
public class VerifiableEmail implements VerifiableElement
{
	private String value;
	private long verificationDate;
	private boolean verified;
	
	public VerifiableEmail() {};
	
	public VerifiableEmail(String value)
	{
		this.value = value;
	}
	
	public String getValue()
	{
		return value;
	}
	public void setValue(String value)
	{
		this.value = value;
	}
	public Long getVerificationDate()
	{
		return verificationDate;
	}
	public void setVerificationDate(Long verificationDate)
	{
		this.verificationDate = verificationDate;
	}
	public boolean isVerified()
	{
		return verified;
	}
	@Override
	public void setVerified(boolean verified)
	{
		this.verified = verified;
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
		
		if (verified != other.isVerified())
			return false;
		
		if (verificationDate != other.getVerificationDate())
			return false;
		
		return true;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + (int) (verificationDate ^ (verificationDate >>> 32));
		result = prime * result + (verified ? 1231 : 1237);
		return result;
	}

//	@Override
//	public void updateVerificationDate(Date verificationDate)
//	{
//		setVerificationDate(verificationDate.getTime());
//		
//	}
}
