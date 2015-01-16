/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import java.io.Serializable;

/**
 * Drag'n'Drop bean - dragged from {@link ProfileStepComponent} to {@link RuleComponent}.
 * 
 * @author Roman Krysinski
 */
public class DragDropBean implements Serializable 
{
	private String expression;
	private String value;

	public DragDropBean(String expression, String value) 
	{
		this.expression = expression;
		this.value = value;
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
	
	@Override
	public String toString() 
	{
		return String.format("%s = %s", expression, value);
	}
}
