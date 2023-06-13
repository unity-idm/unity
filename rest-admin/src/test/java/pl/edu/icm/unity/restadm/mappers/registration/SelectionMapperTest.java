/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.RestSelection;
import pl.edu.icm.unity.base.registration.Selection;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;

public class SelectionMapperTest extends MapperTestBase<Selection, RestSelection>
{
	@Override
	protected Selection getFullAPIObject()
	{
		return new Selection(true, "externalIdp", "Profile");
	}

	@Override
	protected RestSelection getFullRestObject()
	{
		return RestSelection.builder()
				.withExternalIdp("externalIdp")
				.withTranslationProfile("Profile")
				.withSelected(true)
				.build();
	}

	@Override
	protected Pair<Function<Selection, RestSelection>, Function<RestSelection, Selection>> getMapper()
	{
		return Pair.of(SelectionMapper::map, SelectionMapper::map);
	}

}
