/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.AbstractTranslationRule;
import pl.edu.icm.unity.server.translation.RuleFactory;
import pl.edu.icm.unity.server.translation.TranslationCondition;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Rule of translation profile.
 * @author K. Benedyczak
 */
public class RegistrationTranslationRule extends AbstractTranslationRule<RegistrationTranslationAction>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, RegistrationTranslationRule.class);

	public static final RuleFactory<RegistrationTranslationAction> FACTORY = new RuleFactory<RegistrationTranslationAction>()
	{
		@Override
		public RegistrationTranslationRule createRule(RegistrationTranslationAction action, TranslationCondition cnd)
		{
			return new RegistrationTranslationRule(action, cnd);
		}
	};
	
	public RegistrationTranslationRule(RegistrationTranslationAction action,
			TranslationCondition condition)
	{
		super(action, condition);
	}
	
	public void invoke(TranslatedRegistrationRequest translationState,
			Object mvelCtx, String profileName) throws EngineException
	{
		if (condition.evaluate(mvelCtx))
		{
			log.debug("Condition OK");
			action.invoke(translationState, mvelCtx, profileName);
		} else
		{
			log.debug("Condition not met");			
		}
	}
}
