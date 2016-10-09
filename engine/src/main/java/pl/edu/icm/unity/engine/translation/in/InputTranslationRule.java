/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.translation.TranslationCondition;
import pl.edu.icm.unity.engine.translation.TranslationRuleInstance;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Invokes {@link InputTranslationAction}.
 *  
 * @author K. Benedyczak
 */
public class InputTranslationRule extends TranslationRuleInstance<InputTranslationAction>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, InputTranslationRule.class);

	public InputTranslationRule(InputTranslationAction action, TranslationCondition condition)
	{
		super(action, condition);
	}
	
	public void invoke(RemotelyAuthenticatedInput input, Object mvelCtx, MappingResult translationState,
			String profileName) throws EngineException
	{
		if (conditionInstance.evaluate(mvelCtx))
		{
			log.debug("Condition OK");
			MappingResult result = actionInstance.invoke(input, mvelCtx, profileName);
			translationState.mergeWith(result);
		} else
		{
			log.debug("Condition not met");			
		}
	}
}
