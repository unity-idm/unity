/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;

import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Marker interface of factories producing {@link InputTranslationAction}, besides marking narrows down
 * returned types.
 *   
 * @author K. Benedyczak
 */
public interface InputTranslationActionFactory extends TranslationActionFactory
{
	@Override
	InputTranslationAction getInstance(String... parameters);
	
	@Override
	InputTranslationAction getBlindInstance(String... parameters);
}
