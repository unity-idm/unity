/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;

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
		Boolean result = null;
		try
		{
			result = (Boolean) MVEL.executeExpression(compiled, input);
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
}
