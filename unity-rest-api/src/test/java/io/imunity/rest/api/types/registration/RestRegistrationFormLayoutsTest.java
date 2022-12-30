/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.List;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.basic.RestTypeBase;
import io.imunity.rest.api.types.registration.layout.RestBasicFormElement;
import io.imunity.rest.api.types.registration.layout.RestFormCaptionElement;
import io.imunity.rest.api.types.registration.layout.RestFormLayout;
import io.imunity.rest.api.types.registration.layout.RestFormLocalSignupButtonElement;
import io.imunity.rest.api.types.registration.layout.RestFormParameterElement;
import io.imunity.rest.api.types.registration.layout.RestFormSeparatorElement;

public class RestRegistrationFormLayoutsTest extends RestTypeBase<RestRegistrationFormLayouts>
{

	@Override
	protected String getJson()
	{
		return "{\"primaryLayout\":{\"elements\":[{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.BasicFormElement\","
				+ "\"type\":\"REG_CODE\",\"formContentsRelated\":true},"
				+ "{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormCaptionElement\",\"type\":\"CAPTION\","
				+ "\"formContentsRelated\":false,\"value\":{\"DefaultValue\":\"value\",\"Map\":{}}},"
				+ "{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormParameterElement\",\"type\":\"ATTRIBUTE\","
				+ "\"formContentsRelated\":true,\"index\":1},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormSeparatorElement\","
				+ "\"type\":\"SEPARATOR\",\"formContentsRelated\":false},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement\""
				+ ",\"type\":\"LOCAL_SIGNUP\",\"formContentsRelated\":true}]},\"secondaryLayout\":"
				+ "{\"elements\":[{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.BasicFormElement\","
				+ "\"type\":\"ATTRIBUTE\",\"formContentsRelated\":true},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormCaptionElement\""
				+ ",\"type\":\"CAPTION\",\"formContentsRelated\":false,\"value\":{\"DefaultValue\":\"value2\","
				+ "\"Map\":{}}},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormParameterElement\",\"type\":\"ATTRIBUTE\","
				+ "\"formContentsRelated\":true,\"index\":1},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormSeparatorElement\","
				+ "\"type\":\"SEPARATOR\",\"formContentsRelated\":false},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement\","
				+ "\"type\":\"LOCAL_SIGNUP\",\"formContentsRelated\":true}]},\"localSignupEmbeddedAsButton\":true}\n";
	}

	@Override
	protected RestRegistrationFormLayouts getObject()
	{
		return RestRegistrationFormLayouts.builder()
				.withLocalSignupEmbeddedAsButton(true)
				.withPrimaryLayout(RestFormLayout.builder()
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
						.build())
				.withSecondaryLayout(RestFormLayout.builder()
						.withElements(List.of(RestBasicFormElement.builder()
								.withType("ATTRIBUTE")
								.build(),
								RestFormCaptionElement.builder()
										.withValue(RestI18nString.builder()
												.withDefaultValue("value2")
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
						.build())
				.build();
	}

}
