/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.objstore.reg.layout.DBBasicFormElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormCaptionElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormLayout;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormLocalSignupButtonElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormParameterElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormSeparatorElement;
import pl.edu.icm.unity.store.types.DBI18nString;
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
		extends MapperTestBase<RegistrationFormLayouts, DBRegistrationFormLayouts>
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
	protected DBRegistrationFormLayouts getFullDBObject()
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

	@Override
	protected Pair<Function<RegistrationFormLayouts, DBRegistrationFormLayouts>, Function<DBRegistrationFormLayouts, RegistrationFormLayouts>> getMapper()
	{
		return Pair.of(RegistrationFormLayoutsMapper::map, RegistrationFormLayoutsMapper::map);
	}

}
