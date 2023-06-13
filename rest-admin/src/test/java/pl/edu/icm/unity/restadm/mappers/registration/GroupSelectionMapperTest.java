/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.RestGroupSelection;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;

public class GroupSelectionMapperTest extends MapperTestBase<GroupSelection, RestGroupSelection>
{

	@Override
	protected GroupSelection getFullAPIObject()
	{
		return new GroupSelection(List.of("/g1", "/g2"), "externalIdp", "Profile");
	}

	@Override
	protected RestGroupSelection getFullRestObject()
	{
		return RestGroupSelection.builder()
				.withExternalIdp("externalIdp")
				.withTranslationProfile("Profile")
				.withSelectedGroups(List.of("/g1", "/g2"))
				.build();
	}

	@Override
	protected Pair<Function<GroupSelection, RestGroupSelection>, Function<RestGroupSelection, GroupSelection>> getMapper()
	{
		return Pair.of(GroupSelectionMapper::map, GroupSelectionMapper::map);
	}

}
