/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.bulkops;

import java.io.Serializable;

import org.mvel2.MVEL;

/**
 * Processing rule is a pair: condition and action.
 * @author K. Benedyczak
 */
public class ProcessingRule
{
	protected String condition;
	protected Serializable compiledCondition;
	protected EntityAction action;
	
	public ProcessingRule(String condition, EntityAction action)
	{
		this.condition = condition;
		this.action = action;
		this.compiledCondition = MVEL.compileExpression(condition);
	}

	protected ProcessingRule()
	{
	}
	
	public String getCondition()
	{
		return condition;
	}

	public Serializable getCompiledCondition()
	{
		return compiledCondition;
	}

	public EntityAction getAction()
	{
		return action;
	}

	@Override
	public String toString()
	{
		return "ProcessingRule [condition=" + condition + ", action=" + action + "]";
	}
}
