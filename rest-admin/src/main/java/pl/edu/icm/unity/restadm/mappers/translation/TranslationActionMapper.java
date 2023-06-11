/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.translation;

import java.util.List;

import io.imunity.rest.api.types.translation.RestTranslationAction;
import pl.edu.icm.unity.base.translation.TranslationAction;

public class TranslationActionMapper
{
	public static RestTranslationAction map(TranslationAction translationAction)
	{
		return RestTranslationAction.builder()
				.withName(translationAction.getName())
				.withParameters(List.of(translationAction.getParameters()))
				.build();
	}

	public static TranslationAction map(RestTranslationAction restTranslationAction)
	{
		return new TranslationAction(restTranslationAction.name, restTranslationAction.parameters.toArray(String[]::new));
	}

}
