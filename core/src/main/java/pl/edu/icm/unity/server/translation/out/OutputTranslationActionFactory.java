/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import pl.edu.icm.unity.server.translation.TranslationActionDescription;

/**
 * Factory of {@link OutputTranslationAction}
 * @author K. Benedyczak
 */
public interface OutputTranslationActionFactory extends TranslationActionDescription
{
	OutputTranslationAction getInstance(String... parameters);
}
