/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.common;

/**
 * Styles defined for UpMan
 * 
 * @author P.Piernik
 *
 */
public enum UpManStyles
{

	indentSmall("u-indentSmall"),
	viewHeader("u-viewHeader"),
	indentComboBox("u-indentComboBox");

	private String value;

	private UpManStyles(String value)
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
