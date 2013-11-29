/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

import java.io.Serializable;

import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;

/**
 * MVEL condition of translation rule.
 * @author K. Benedyczak
 */
public class TranslationCondition
{
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


	public boolean evaluate(RemotelyAuthenticatedInput input) throws EngineException
	{
		Boolean result = (Boolean) MVEL.executeExpression(compiled, input);
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
