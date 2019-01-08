/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.confirmation;
/**
 * Contains code with validity time and mobileNumber related with this code.
 * @author P.Piernik
 *
 */
public class SMSCode
{
	private long validTo;
	private String value;
	private String mobileNumber;
	
	public SMSCode(long validTo, String value, String mobileNumber)
	{
	
		setValidTo(validTo);
		setValue(value);
		setMobileNumber(mobileNumber);
	}

	public long getValidTo()
	{
		return validTo;
	}

	public void setValidTo(long validTo)
	{
		this.validTo = validTo;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getMobileNumber()
	{
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber)
	{
		this.mobileNumber = mobileNumber;
	}
}
