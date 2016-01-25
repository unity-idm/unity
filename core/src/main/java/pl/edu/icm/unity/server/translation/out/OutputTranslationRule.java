/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.AbstractTranslationRule;
import pl.edu.icm.unity.server.translation.RuleFactory;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Invokes {@link OutputTranslationAction}.
 *  
 * @author K. Benedyczak
 */
public class OutputTranslationRule extends AbstractTranslationRule<OutputTranslationAction>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, OutputTranslationRule.class);
	
	public static final RuleFactory<OutputTranslationAction> FACTORY = new RuleFactory<OutputTranslationAction>()
	{
		@Override
		public OutputTranslationRule createRule(OutputTranslationAction action, TranslationCondition cnd)
		{
			return new OutputTranslationRule(action, cnd);
		}
	};
	
	public OutputTranslationRule(OutputTranslationAction action, TranslationCondition condition)
	{
		super(action, condition);
	}
	
	public void invoke(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException
	{
		if (condition.evaluate(mvelCtx))
		{
			log.debug("Condition OK");
			action.invoke(input, mvelCtx, currentProfile, result);
		} else
		{
			log.debug("Condition not met");			
		}
	}
}
