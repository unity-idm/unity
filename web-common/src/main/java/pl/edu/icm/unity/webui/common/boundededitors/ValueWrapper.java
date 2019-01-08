/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.boundededitors;

public class ValueWrapper
{
	private String value;
	private boolean unlimited;

	ValueWrapper(String value, boolean unlimited)
	{
		this.value = value;
		this.unlimited = unlimited;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public boolean isUnlimited()
	{
		return unlimited;
	}

	public void setUnlimited(boolean unlimited)
	{
		this.unlimited = unlimited;
	}
}