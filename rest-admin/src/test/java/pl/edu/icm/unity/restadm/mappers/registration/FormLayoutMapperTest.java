/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.layout.RestBasicFormElement;
import io.imunity.rest.api.types.registration.layout.RestFormLayout;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.restadm.mappers.registration.layout.FormLayoutMapper;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLayoutElement;

public class FormLayoutMapperTest extends MapperTestBase<FormLayout, RestFormLayout>

{

	@Override
	protected FormLayout getFullAPIObject()
	{
		return new FormLayout(List.of(new BasicFormElement(FormLayoutElement.REG_CODE)));
	}

	@Override
	protected RestFormLayout getFullRestObject()
	{
		return RestFormLayout.builder()
				.withElements(List.of(RestBasicFormElement.builder()
						.withType("REG_CODE")
						.withFormContentsRelated(true)
						.build()))
				.build();
	}

	@Override
	protected Pair<Function<FormLayout, RestFormLayout>, Function<RestFormLayout, FormLayout>> getMapper()
	{
		return Pair.of(FormLayoutMapper::map, FormLayoutMapper::map);
	}

}
