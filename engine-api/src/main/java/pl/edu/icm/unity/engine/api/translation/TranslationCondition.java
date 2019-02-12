/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * MVEL condition of translation rule.
 * @author K. Benedyczak
 */
public class TranslationCondition
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, TranslationCondition.class);
	private String condition;
	private Serializable compiled;
	
	public TranslationCondition(String condition)
	{
		setCondition(condition);
	}
	
	/**
	 * Creates an always true condition
	 */
	public TranslationCondition()
	{
		setCondition("true");
	}


	public boolean evaluate(Object input) throws EngineException
	{
		return evaluateCondition(compiled, input, log);
	}

	public boolean evaluate(Object input, Logger log) throws EngineException
	{
		return evaluateCondition(compiled, input, log);
	}

	private boolean evaluateCondition(Serializable compiled, Object input, Logger log) throws EngineException
	{
		Boolean result = null;
		try
		{
			result = (Boolean) MVEL.executeExpression(compiled, input, new HashMap<>());
		} catch (Exception e)
		{
			if (log.isDebugEnabled())
			{
				log.debug("Error during expression execution.", e);
			}
			throw new EngineException(e);
		}
		
		if (result == null)
		{
			log.debug("Condition evaluated to null value, assuming false");
			return false;
		}
		return result.booleanValue();
	}
	
	public String getCondition()
	{
		return condition;
	}

	public void setCondition(String condition)
	{
		this.condition = condition;
		this.compiled = MVEL.compileExpression(condition);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TranslationCondition other = (TranslationCondition) obj;
		if (condition == null)
		{
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		return true;
	}
}
