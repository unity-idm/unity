/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.layout;

import java.util.List;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestFormLayoutTest extends RestTypeBase<RestFormLayout>
{

	@Override
	protected String getJson()
	{
		return "{\"elements\":[{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.BasicFormElement\",\"type\":\"REG_CODE\","
				+ "\"formContentsRelated\":true},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormCaptionElement\","
				+ "\"type\":\"CAPTION\",\"formContentsRelated\":false,\"value\":{\"DefaultValue\":\"value\",\"Map\":{}}},"
				+ "{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormParameterElement\",\"type\":\"ATTRIBUTE\","
				+ "\"formContentsRelated\":true,\"index\":1},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormSeparatorElement\""
				+ ",\"type\":\"SEPARATOR\",\"formContentsRelated\":false},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement\","
				+ "\"type\":\"LOCAL_SIGNUP\",\"formContentsRelated\":true}]}\n";
	}

	@Override
	protected RestFormLayout getObject()
	{
		return RestFormLayout.builder()
				.withElements(List.of(RestBasicFormElement.builder()
						.withType("REG_CODE")
						.build(),
						RestFormCaptionElement.builder()
								.withValue(RestI18nString.builder()
										.withDefaultValue("value")
										.build())
								.build(),
						RestFormParameterElement.builder()
								.withIndex(1)
								.withType("ATTRIBUTE")
								.build(),
						RestFormSeparatorElement.builder()
								.build(),
						RestFormLocalSignupButtonElement.builder()
								.build()))
				.build();
	}

}
