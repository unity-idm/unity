/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.translation.TranslationRuleInstance;

/**
 * Rule of translation profile.
 * @author K. Benedyczak
 */
public class RegistrationTranslationRule extends TranslationRuleInstance<RegistrationTranslationAction>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, RegistrationTranslationRule.class);

	public RegistrationTranslationRule(RegistrationTranslationAction action,
			TranslationCondition condition)
	{
		super(action, condition);
	}
	
	public void invoke(TranslatedRegistrationRequest translationState,
			Object mvelCtx, RegistrationContext context, String profileName) throws EngineException
	{
		if (conditionInstance.evaluate(mvelCtx))
		{
			log.debug("Condition OK");
			actionInstance.invoke(translationState, mvelCtx, context, profileName);
		} else
		{
			log.debug("Condition not met");			
		}
	}
}
