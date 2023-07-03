/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.objstore.reg.common.DBGroupSelection;
import pl.edu.icm.unity.store.objstore.reg.common.GroupSelectionMapper;

public class PrefilledEntryMapperTest
		extends MapperTestBase<PrefilledEntry<GroupSelection>, DBPrefilledEntry<DBGroupSelection>>
{

	@Override
	protected PrefilledEntry<GroupSelection> getFullAPIObject()
	{
		return new PrefilledEntry<GroupSelection>(new GroupSelection(List.of("/g1")), PrefilledEntryMode.HIDDEN);
	}

	@Override
	protected DBPrefilledEntry<DBGroupSelection> getFullDBObject()
	{
		return new DBPrefilledEntry.Builder<DBGroupSelection>().withEntry(DBGroupSelection.builder()
				.withSelectedGroups(List.of("/g1"))
				.build())
				.withMode("HIDDEN")
				.build();
	}

	@Override
	protected Pair<Function<PrefilledEntry<GroupSelection>, DBPrefilledEntry<DBGroupSelection>>, Function<DBPrefilledEntry<DBGroupSelection>, PrefilledEntry<GroupSelection>>> getMapper()
	{
		return Pair.of(s -> PrefilledEntryMapper.map(s, t -> GroupSelectionMapper.map(t)),
				s -> PrefilledEntryMapper.map(s, t -> GroupSelectionMapper.map(t)));
	}

}
