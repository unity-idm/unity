/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import io.imunity.rest.api.types.registration.RestSelection;
import pl.edu.icm.unity.types.registration.Selection;

public class SelectionMapper
{
	public static RestSelection map(Selection selection)
	{
		return RestSelection.builder()
				.withExternalIdp(selection.getExternalIdp())
				.withTranslationProfile(selection.getTranslationProfile())
				.withSelected(selection.isSelected())
				.build();
	}

	public static Selection map(RestSelection restSelection)
	{
		return new Selection(restSelection.selected, restSelection.externalIdp, restSelection.translationProfile);
	}
}
