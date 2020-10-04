/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webadmin.tprofile;

/**
 * Trivial bean for simply vaadin bindings
 * @author P.Piernik
 *
 */
public class StringValueBean
{
	private String value;
	
	public StringValueBean()
	{

	}
	
	public StringValueBean(String value)
	{
		this.value = value;
	}
	
	/**
	 * @return the value
	 */
	public String getValue() 
	{
		return value;
	}
	
	/**
	 * @param value the value to set
	 */
	public void setValue(String value) 
	{
		this.value = value;
	}
}
