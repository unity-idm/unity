/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class GroupSelectionMapperTest extends MapperTestBase<GroupSelection, DBGroupSelection>
{

	@Override
	protected GroupSelection getFullAPIObject()
	{
		return new GroupSelection(List.of("/g1", "/g2"), "externalIdp", "Profile");
	}

	@Override
	protected DBGroupSelection getFullDBObject()
	{
		return DBGroupSelection.builder()
				.withExternalIdp("externalIdp")
				.withTranslationProfile("Profile")
				.withSelectedGroups(List.of("/g1", "/g2"))
				.build();
	}

	@Override
	protected Pair<Function<GroupSelection, DBGroupSelection>, Function<DBGroupSelection, GroupSelection>> getMapper()
	{
		return Pair.of(GroupSelectionMapper::map, GroupSelectionMapper::map);
	}

}
