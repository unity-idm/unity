/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.binding;

/**
 * Simple A name-value pair parameter. Usefull for vaadin binder
 * 
 * @author P.Piernik
 *
 */
public class NameValuePairBinding
{
	private String name;
	private String value;

	public NameValuePairBinding()
	{
	}
	
	public NameValuePairBinding(String name, String value)
	{
		super();
		this.name = name;
		this.value = value;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

}
