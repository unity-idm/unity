/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.registration.invite;

import java.util.List;
import java.util.function.Function;

import io.imunity.rest.api.types.registration.RestGroupSelection;
import io.imunity.rest.api.types.registration.invite.RestPrefilledEntry;
import pl.edu.icm.unity.rest.mappers.MapperTestBase;
import pl.edu.icm.unity.rest.mappers.Pair;
import pl.edu.icm.unity.rest.mappers.registration.GroupSelectionMapper;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;

public class PrefilledEntryMapperTest
		extends MapperTestBase<PrefilledEntry<GroupSelection>, RestPrefilledEntry<RestGroupSelection>>
{

	@Override
	protected PrefilledEntry<GroupSelection> getFullAPIObject()
	{
		return new PrefilledEntry<GroupSelection>(new GroupSelection(List.of("/g1")), PrefilledEntryMode.HIDDEN);
	}

	@Override
	protected RestPrefilledEntry<RestGroupSelection> getFullRestObject()
	{
		return new RestPrefilledEntry.Builder<RestGroupSelection>().withEntry(RestGroupSelection.builder()
				.withSelectedGroups(List.of("/g1"))
				.build())
				.withMode("HIDDEN")
				.build();
	}

	@Override
	protected Pair<Function<PrefilledEntry<GroupSelection>, RestPrefilledEntry<RestGroupSelection>>, Function<RestPrefilledEntry<RestGroupSelection>, PrefilledEntry<GroupSelection>>> getMapper()
	{
		return Pair.of(s -> PrefilledEntryMapper.map(s, t -> GroupSelectionMapper.map(t)),
				s -> PrefilledEntryMapper.map(s, t -> GroupSelectionMapper.map(t)));
	}

}
