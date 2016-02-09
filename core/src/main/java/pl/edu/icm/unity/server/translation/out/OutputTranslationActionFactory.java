/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Marker interface of factories producing {@link OutputTranslationAction}s, also narrow down returned types.
 * @author K. Benedyczak
 */
public interface OutputTranslationActionFactory extends TranslationActionFactory
{
	@Override
	OutputTranslationAction getInstance(String... parameters);
	
	@Override
	OutputTranslationAction getBlindInstance(String... parameters);

}
