/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration.invite;

import java.util.List;
import java.util.function.Function;

import io.imunity.rest.api.types.registration.RestGroupSelection;
import io.imunity.rest.api.types.registration.invite.RestPrefilledEntry;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import io.imunity.rest.mappers.registration.GroupSelectionMapper;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;

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
