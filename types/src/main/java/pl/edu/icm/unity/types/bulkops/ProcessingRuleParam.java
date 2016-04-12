/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.bulkops;

/**
 * Provides a JSON representation of BulkProcessingRule
 * @author Krzysztof Benedyczak
 */
public class ProcessingRuleParam
{
	private String condition;
	private String actionName;
	private String[] params;
	
	public ProcessingRuleParam(String condition, String actionName, String... params)
	{
		this.condition = condition;
		this.actionName = actionName;
		this.params = params;
	}

	public ProcessingRuleParam()
	{
	}


	public String getCondition()
	{
		return condition;
	}
	public void setCondition(String condition)
	{
		this.condition = condition;
	}
	public String getActionName()
	{
		return actionName;
	}
	public void setActionName(String actionName)
	{
		this.actionName = actionName;
	}
	public String[] getParams()
	{
		return params;
	}
	public void setParams(String[] params)
	{
		this.params = params;
	}
}
