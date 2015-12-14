/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ExecutionBreakException;
import pl.edu.icm.unity.server.translation.TranslationAction;

/**
 * Instance operates on a contents of a registration request submitted by a prospective user. 
 * @author K. Benedyczak
 */
public interface RegistrationTranslationAction extends TranslationAction 
{
	/**
	 * Performs the translation.
	 * @param state
	 * @param mvelCtx context which can be used in MVEL expression evaluation
	 * @param currentProfile name of the current profile
	 * @return result of the mapping
	 * @throws EngineException when an error occurs. You can throw {@link ExecutionBreakException}
	 * to gently stop the processing of further rules.
	 */
	public void invoke(TranslatedRegistrationRequest state,
			Object mvelCtx,	String currentProfile) throws EngineException;
}
