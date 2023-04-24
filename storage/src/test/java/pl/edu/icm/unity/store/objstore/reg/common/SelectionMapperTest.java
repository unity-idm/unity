/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.registration.Selection;

public class SelectionMapperTest extends MapperTestBase<Selection, DBSelection>
{
	@Override
	protected Selection getFullAPIObject()
	{
		return new Selection(true, "externalIdp", "Profile");
	}

	@Override
	protected DBSelection getFullDBObject()
	{
		return DBSelection.builder()
				.withExternalIdp("externalIdp")
				.withTranslationProfile("Profile")
				.withSelected(true)
				.build();
	}

	@Override
	protected Pair<Function<Selection, DBSelection>, Function<DBSelection, Selection>> getMapper()
	{
		return Pair.of(SelectionMapper::map, SelectionMapper::map);
	}

}
