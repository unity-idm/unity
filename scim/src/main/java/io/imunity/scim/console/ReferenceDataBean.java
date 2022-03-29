/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import io.imunity.scim.config.ReferenceAttributeMapping.ReferenceType;

public class ReferenceDataBean
{
	private ReferenceType type;
	private String expression;

	public ReferenceDataBean(ReferenceType type, String expression)
	{
		this.type = type;
		this.expression = expression;
	}

	public ReferenceDataBean()
	{
		this(null, null);
	}

	public ReferenceType getType()
	{
		return type;
	}

	public void setType(ReferenceType type)
	{
		this.type = type;
	}

	public String getExpression()
	{
		return expression;
	}

	public void setExpression(String expression)
	{
		this.expression = expression;
	}

}
