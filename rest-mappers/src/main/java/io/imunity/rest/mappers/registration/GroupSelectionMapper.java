/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import pl.edu.icm.unity.types.registration.GroupSelection;

import io.imunity.rest.api.types.registration.RestGroupSelection;

public class GroupSelectionMapper
{
	public static RestGroupSelection map(GroupSelection groupSelection)
	{
		return RestGroupSelection.builder()
				.withExternalIdp(groupSelection.getExternalIdp())
				.withTranslationProfile(groupSelection.getTranslationProfile())
				.withSelectedGroups(groupSelection.getSelectedGroups())
				.build();
	}

	public static GroupSelection map(RestGroupSelection restGroupSelection)
	{
		return new GroupSelection(restGroupSelection.selectedGroups, restGroupSelection.externalIdp,
				restGroupSelection.translationProfile);

	}
}
