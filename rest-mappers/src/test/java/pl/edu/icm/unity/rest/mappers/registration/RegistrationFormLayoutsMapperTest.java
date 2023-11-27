/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.registration;

import java.util.List;
import java.util.function.Function;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.registration.RestRegistrationFormLayouts;
import io.imunity.rest.api.types.registration.layout.RestBasicFormElement;
import io.imunity.rest.api.types.registration.layout.RestFormCaptionElement;
import io.imunity.rest.api.types.registration.layout.RestFormLayout;
import io.imunity.rest.api.types.registration.layout.RestFormLocalSignupButtonElement;
import io.imunity.rest.api.types.registration.layout.RestFormParameterElement;
import io.imunity.rest.api.types.registration.layout.RestFormSeparatorElement;
import pl.edu.icm.unity.rest.mappers.MapperTestBase;
import pl.edu.icm.unity.rest.mappers.Pair;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.RegistrationFormLayouts;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLayoutElement;
import pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.types.registration.layout.FormSeparatorElement;

public class RegistrationFormLayoutsMapperTest
		extends MapperTestBase<RegistrationFormLayouts, RestRegistrationFormLayouts>
{

	@Override
	protected RegistrationFormLayouts getFullAPIObject()
	{
		FormLayout formLayout = new FormLayout(List.of(new BasicFormElement(FormLayoutElement.REG_CODE),
				new FormCaptionElement(new I18nString("value")),
				new FormParameterElement(FormLayoutElement.ATTRIBUTE, 1), new FormSeparatorElement(),
				new FormLocalSignupButtonElement()));

		FormLayout formLayout2 = new FormLayout(List.of(new BasicFormElement(FormLayoutElement.ATTRIBUTE),
				new FormCaptionElement(new I18nString("value2")),
				new FormParameterElement(FormLayoutElement.ATTRIBUTE, 1), new FormSeparatorElement(),
				new FormLocalSignupButtonElement()));

		RegistrationFormLayouts registrationFormLayouts = new RegistrationFormLayouts();
		registrationFormLayouts.setLocalSignupEmbeddedAsButton(true);
		registrationFormLayouts.setPrimaryLayout(formLayout);
		registrationFormLayouts.setSecondaryLayout(formLayout2);
		return registrationFormLayouts;
	}

	@Override
	protected RestRegistrationFormLayouts getFullRestObject()
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

	@Override
	protected Pair<Function<RegistrationFormLayouts, RestRegistrationFormLayouts>, Function<RestRegistrationFormLayouts, RegistrationFormLayouts>> getMapper()
	{
		return Pair.of(RegistrationFormLayoutsMapper::map, RegistrationFormLayoutsMapper::map);
	}

}
