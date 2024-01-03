/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.mvel;

import java.io.Serializable;


class DragDropBean implements Serializable
{
	private String value;
	private String expression;

	DragDropBean(String expression, String value)
	{
		this.value = value;
		this.expression = expression;
	}

	public String getExpression() 
	{
		return expression;
	}

	public void setExpression(String expression) 
	{
		this.expression = expression;
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
	public String toString() 
	{
		return String.format("%s = %s", expression, value);
	}
}
