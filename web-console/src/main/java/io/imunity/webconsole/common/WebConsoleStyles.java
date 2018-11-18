/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.common;

/**
 * General purpose styles defined for WebConsole components
 * @author P.Piernik
 *
 */
public enum WebConsoleStyles
{
	breadcrumbs("u-breadcrumbs");

	private String value;

	private WebConsoleStyles(String value)
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
