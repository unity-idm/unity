/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.tprofile;

import java.util.List;

import pl.edu.icm.unity.types.translation.TranslationAction;

public class TranslationActionMapper
{
	public static DBTranslationAction map(TranslationAction translationAction)
	{
		return DBTranslationAction.builder()
				.withName(translationAction.getName())
				.withParameters(List.of(translationAction.getParameters()))
				.build();
	}

	public static TranslationAction map(DBTranslationAction restTranslationAction)
	{
		return new TranslationAction(restTranslationAction.name, restTranslationAction.parameters.toArray(String[]::new));
	}

}
