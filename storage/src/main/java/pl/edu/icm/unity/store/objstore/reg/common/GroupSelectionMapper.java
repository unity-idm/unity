/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.types.registration.GroupSelection;


public class GroupSelectionMapper
{
	public static DBGroupSelection map(GroupSelection groupSelection)
	{
		return DBGroupSelection.builder()
				.withExternalIdp(groupSelection.getExternalIdp())
				.withTranslationProfile(groupSelection.getTranslationProfile())
				.withSelectedGroups(groupSelection.getSelectedGroups())
				.build();
	}

	public static GroupSelection map(DBGroupSelection restGroupSelection)
	{
		return new GroupSelection(restGroupSelection.selectedGroups, restGroupSelection.externalIdp,
				restGroupSelection.translationProfile);

	}
}
