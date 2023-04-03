/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import java.util.function.Function;


import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;

public class FormLayoutSettingsMapperTest extends MapperTestBase<FormLayoutSettings, DBFormLayoutSettings>
{

	@Override
	protected FormLayoutSettings getFullAPIObject()
	{
		return new FormLayoutSettings(true, 10, "EM", true, "logo");
	}

	@Override
	protected DBFormLayoutSettings getFullDBObject()
	{
		return DBFormLayoutSettings.builder()
				.withCompactInputs(true)
				.withShowCancel(true)
				.withColumnWidth(10)
				.withColumnWidthUnit("EM")
				.withLogoURL("logo")
				.build();
	}

	@Override
	protected Pair<Function<FormLayoutSettings, DBFormLayoutSettings>, Function<DBFormLayoutSettings, FormLayoutSettings>> getMapper()
	{
		return Pair.of(FormLayoutSettingsMapper::map, FormLayoutSettingsMapper::map);
	}

}
