/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.translation.SystemTranslationProfileProvider;
import pl.edu.icm.unity.engine.translation.TranslationProfileRepositotory;
import pl.edu.icm.unity.store.api.generic.InputTranslationProfileDB;
import pl.edu.icm.unity.types.translation.ProfileType;

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
			List<SystemTranslationProfileProvider> systemProfileProviders)
	{
		
		super(dao, systemProfileProviders.stream().filter(p -> p.getSupportedType() == ProfileType.INPUT).collect(Collectors.toList()));
	}

}
