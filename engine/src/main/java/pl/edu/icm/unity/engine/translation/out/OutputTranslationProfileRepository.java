/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.translation.SystemTranslationProfileProvider;
import pl.edu.icm.unity.engine.translation.TranslationProfileRepositotory;
import pl.edu.icm.unity.store.api.generic.OutputTranslationProfileDB;
import pl.edu.icm.unity.types.translation.ProfileType;

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
			List<SystemTranslationProfileProvider> systemProfileProviders)
	{
		super(dao, systemProfileProviders.stream().filter(p -> p.getSupportedType() == ProfileType.OUTPUT).collect(Collectors.toList()));
	}

}
