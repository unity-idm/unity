/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.translation.TranslationProfileRepositotory;
import pl.edu.icm.unity.store.api.generic.InputTranslationProfileDB;

/**
 * Allows read input profiles from DB and @{SystemTranslationProfileProvider}s 
 * @author P.Piernik
 *
 */
@Component
public class InputTranslationProfileRepository extends TranslationProfileRepositotory
{

	@Autowired
	public InputTranslationProfileRepository(InputTranslationProfileDB dao ,
			SystemInputTranslationProfileProvider systemProfileProvider)
	{
		
		super(dao, systemProfileProvider);
	}

}
