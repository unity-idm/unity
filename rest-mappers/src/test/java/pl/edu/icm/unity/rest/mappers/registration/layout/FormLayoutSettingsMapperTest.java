/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.registration.layout;

import java.util.function.Function;

import io.imunity.rest.api.types.registration.layout.RestFormLayoutSettings;
import pl.edu.icm.unity.rest.mappers.MapperTestBase;
import pl.edu.icm.unity.rest.mappers.Pair;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;

public class FormLayoutSettingsMapperTest extends MapperTestBase<FormLayoutSettings, RestFormLayoutSettings>
{

	@Override
	protected FormLayoutSettings getFullAPIObject()
	{
		return new FormLayoutSettings(true, 10, "EM", true, "logo");
	}

	@Override
	protected RestFormLayoutSettings getFullRestObject()
	{
		return RestFormLayoutSettings.builder()
				.withCompactInputs(true)
				.withShowCancel(true)
				.withColumnWidth(10)
				.withColumnWidthUnit("EM")
				.withLogoURL("logo")
				.build();
	}

	@Override
	protected Pair<Function<FormLayoutSettings, RestFormLayoutSettings>, Function<RestFormLayoutSettings, FormLayoutSettings>> getMapper()
	{
		return Pair.of(FormLayoutSettingsMapper::map, FormLayoutSettingsMapper::map);
	}

}
