/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.base.registration.Selection;

public class SelectionMapper
{
	public static DBSelection map(Selection selection)
	{
		return DBSelection.builder()
				.withExternalIdp(selection.getExternalIdp())
				.withTranslationProfile(selection.getTranslationProfile())
				.withSelected(selection.isSelected())
				.build();
	}

	public static Selection map(DBSelection restSelection)
	{
		return new Selection(restSelection.selected, restSelection.externalIdp, restSelection.translationProfile);
	}
}
