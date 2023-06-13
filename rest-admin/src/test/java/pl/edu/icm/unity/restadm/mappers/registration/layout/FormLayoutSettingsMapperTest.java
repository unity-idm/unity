/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration.layout;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.layout.RestFormLayoutSettings;
import pl.edu.icm.unity.base.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;

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
