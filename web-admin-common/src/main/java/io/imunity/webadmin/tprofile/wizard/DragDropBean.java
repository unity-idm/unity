/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile.wizard;

import java.io.Serializable;

import io.imunity.webadmin.tprofile.RuleComponent;
import io.imunity.webadmin.tprofile.StringValueBean;

/**
 * Drag'n'Drop bean - dragged from {@link ProfileStepComponent} to {@link RuleComponent}.
 * 
 * @author Roman Krysinski
 */
public class DragDropBean extends StringValueBean implements Serializable 
{
	private String expression;
	
	public DragDropBean(String expression, String value) 
	{
		super(value);
		this.expression = expression;
	}

	/**
	 * @return the expression
	 */
	public String getExpression() 
	{
		return expression;
	}

	/**
	 * @param expression the expression to set
	 */
	public void setExpression(String expression) 
	{
		this.expression = expression;
	}
	
	@Override
	public String toString() 
	{
		return String.format("%s = %s", expression, getValue());
	}
}
