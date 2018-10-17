/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.translation.SystemTranslationProfileProviderBase;
import pl.edu.icm.unity.types.translation.ProfileType;

/**
 * Provides system output translation profiles
 * @author P.Piernik
 *
 */
@Component
public class SystemOutputTranslationProfileProvider extends SystemTranslationProfileProviderBase
{
	@Autowired
	public SystemOutputTranslationProfileProvider(ApplicationContext applicationContext)
	{
		super(applicationContext);
	}

	@Override
	protected ProfileType getType()
	{
		return ProfileType.OUTPUT;
	}

}
