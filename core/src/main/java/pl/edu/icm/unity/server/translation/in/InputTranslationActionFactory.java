/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;

import pl.edu.icm.unity.server.translation.TranslationActionDescription;

/**
 * Factory of {@link InputTranslationAction}
 * @author K. Benedyczak
 */
public interface InputTranslationActionFactory extends TranslationActionDescription
{
	InputTranslationAction getInstance(String... parameters);
}
