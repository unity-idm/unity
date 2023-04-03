/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.List;


import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.objstore.reg.layout.DBBasicFormElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormCaptionElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormLayout;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormLocalSignupButtonElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormParameterElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormSeparatorElement;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBRegistrationFormLayoutsTest extends DBTypeTestBase<DBRegistrationFormLayouts>
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
	protected DBRegistrationFormLayouts getObject()
	{
		return DBRegistrationFormLayouts.builder()
				.withLocalSignupEmbeddedAsButton(true)
				.withPrimaryLayout(DBFormLayout.builder()
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
						.build())
				.withSecondaryLayout(DBFormLayout.builder()
						.withElements(List.of(DBBasicFormElement.builder()
								.withType("ATTRIBUTE")
								.build(),
								DBFormCaptionElement.builder()
										.withValue(DBI18nString.builder()
												.withDefaultValue("value2")
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
						.build())
				.build();
	}

}
