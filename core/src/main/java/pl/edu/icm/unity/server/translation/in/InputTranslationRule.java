/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.AbstractTranslationRule;
import pl.edu.icm.unity.server.translation.RuleFactory;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Invokes {@link InputTranslationAction}.
 *  
 * @author K. Benedyczak
 */
public class InputTranslationRule extends AbstractTranslationRule<InputTranslationAction>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, InputTranslationRule.class);

	public static final RuleFactory<InputTranslationAction> FACTORY = new RuleFactory<InputTranslationAction>()
	{
		@Override
		public InputTranslationRule createRule(InputTranslationAction action, TranslationCondition cnd)
		{
			return new InputTranslationRule(action, cnd);
		}
	};
	
	public InputTranslationRule(InputTranslationAction action, TranslationCondition condition)
	{
		super(action, condition);
	}
	
	public void invoke(RemotelyAuthenticatedInput input, Object mvelCtx, MappingResult translationState,
			String profileName) throws EngineException
	{
		if (condition.evaluate(mvelCtx))
		{
			log.debug("Condition OK");
			MappingResult result = action.invoke(input, mvelCtx, profileName);
			translationState.mergeWith(result);
		} else
		{
			log.debug("Condition not met");			
		}
	}
}
