/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.DBI18nString;

public class DBFormLayoutTest extends DBTypeTestBase<DBFormLayout>
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
	protected DBFormLayout getObject()
	{
		return DBFormLayout.builder()
				.withElements(List.of(DBBasicFormElement.builder()
						.withType("REG_CODE")
						.build(),
						DBFormCaptionElement.builder()
								.withValue(DBI18nString.builder()
										.withDefaultValue("value")
										.build())
								.build(),
						DBFormParameterElement.builder()
								.withIndex(1)
								.withType("ATTRIBUTE")
								.build(),
						DBFormSeparatorElement.builder()
								.build(),
						DBFormLocalSignupButtonElement.builder()
								.build()))
				.build();
	}

}
