/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.translation.TranslationProfileRepositotory;
import pl.edu.icm.unity.store.api.generic.OutputTranslationProfileDB;

/**
 * Allows read output profiles from DB and @{SystemTranslationProfileProvider}s 
 * @author P.Piernik
 *
 */
@Component
public class OutputTranslationProfileRepository extends TranslationProfileRepositotory
{

	@Autowired
	public OutputTranslationProfileRepository(OutputTranslationProfileDB dao ,
			SystemOutputTranslationProfileProvider systemProfileProvider)
	{
		super(dao, systemProfileProvider);
	}

}
