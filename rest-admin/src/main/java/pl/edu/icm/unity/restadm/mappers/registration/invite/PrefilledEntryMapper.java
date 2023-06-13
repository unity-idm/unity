/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration.invite;

import java.util.function.Function;

import io.imunity.rest.api.types.registration.invite.RestPrefilledEntry;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntryMode;

public class PrefilledEntryMapper
{
	public static <S, T> RestPrefilledEntry<T> map(PrefilledEntry<S> prefilledEntry, Function<S, T> entryMapper)
	{
		return new RestPrefilledEntry.Builder<T>().withEntry(entryMapper.apply(prefilledEntry.getEntry()))
				.withMode(prefilledEntry.getMode()
						.name())
				.build();
	}

	
	public static <S, T> RestPrefilledEntry<T> map(PrefilledEntry<S> prefilledEntry, T entry)
	{
		return new RestPrefilledEntry.Builder<T>().withEntry(entry)
				.withMode(prefilledEntry.getMode()
						.name())
				.build();
	}
	
	public static <T, S> PrefilledEntry<S> map(RestPrefilledEntry<T> prefilledEntry, Function<T, S> entryMapper)
	{
		return new PrefilledEntry<S>(entryMapper.apply(prefilledEntry.entry),
				PrefilledEntryMode.valueOf(prefilledEntry.mode));

	}

}
