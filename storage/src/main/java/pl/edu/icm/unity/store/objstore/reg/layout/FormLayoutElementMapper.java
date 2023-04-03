/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import pl.edu.icm.unity.store.types.common.I18nStringMapper;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayoutElement;
import pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.types.registration.layout.FormSeparatorElement;

public class FormLayoutElementMapper
{
	private final static List<Function<FormElement, Optional<DBFormElement>>> mappers = List.of(
			BasicFormElementMapper::map, FormCaptionElementMapper::map, FormLocalSignupButtonElementMapper::map,
			FormParameterElementMapper::map, FormSeparatorElementMapper::map);

	private final static List<Function<DBFormElement, Optional<FormElement>>> mappersFrom = List.of(
			BasicFormElementMapper::map, FormCaptionElementMapper::map, FormLocalSignupButtonElementMapper::map,
			FormParameterElementMapper::map, FormSeparatorElementMapper::map);

	public static DBFormElement map(FormElement basicFormElement)
	{
		for (Function<FormElement, Optional<DBFormElement>> mapper : mappers)
		{
			Optional<DBFormElement> apply = mapper.apply(basicFormElement);
			if (apply.isPresent())
				return apply.get();
		}

		throw new IllegalArgumentException("Can not find mapper for type " + basicFormElement.getClass()
				.getName());

	}

	public static FormElement map(DBFormElement restFormElement)
	{

		for (Function<DBFormElement, Optional<FormElement>> mapper : mappersFrom)
		{
			Optional<FormElement> apply = mapper.apply(restFormElement);
			if (apply.isPresent())
				return apply.get();
		}

		throw new IllegalArgumentException("Can not find mapper for type " + restFormElement.getClass()
				.getName());
	}

	public static class BasicFormElementMapper
	{
		public static Optional<FormElement> map(DBFormElement restBasicFormElement)
		{
			if (!(restBasicFormElement instanceof DBBasicFormElement))
				return Optional.empty();

			return Optional.of(new BasicFormElement(FormLayoutElement.valueOf(restBasicFormElement.type)));
		}

		public static Optional<DBFormElement> map(FormElement basicFormElement)
		{
			if (!(basicFormElement instanceof BasicFormElement))
				return Optional.empty();

			return Optional.of(DBBasicFormElement.builder()
					.withFormContentsRelated(basicFormElement.isFormContentsRelated())
					.withType(basicFormElement.getType()
							.name())
					.build());
		}
	}

	public static class FormCaptionElementMapper
	{
		public static Optional<FormElement> map(DBFormElement restFormElement)
		{
			if (!(restFormElement instanceof DBFormCaptionElement))
				return Optional.empty();

			return Optional
					.of(new FormCaptionElement(Optional.ofNullable(((DBFormCaptionElement) restFormElement).value)
							.map(I18nStringMapper::map)
							.orElse(null)));
		}

		public static Optional<DBFormElement> map(FormElement formElement)
		{
			if (!(formElement instanceof FormCaptionElement))
				return Optional.empty();

			return Optional.of(DBFormCaptionElement.builder()
					.withFormContentsRelated(formElement.isFormContentsRelated())
					.withType(formElement.getType()
							.name())
					.withValue(Optional.ofNullable(((FormCaptionElement) formElement).getValue())
							.map(I18nStringMapper::map)
							.orElse(null))
					.build());
		}
	}

	public static class FormLocalSignupButtonElementMapper
	{
		public static Optional<FormElement> map(DBFormElement restFormElement)
		{
			if (!(restFormElement instanceof DBFormLocalSignupButtonElement))
				return Optional.empty();

			return Optional.of(new FormLocalSignupButtonElement());
		}

		public static Optional<DBFormElement> map(FormElement formElement)
		{
			if (!(formElement instanceof FormLocalSignupButtonElement))
				return Optional.empty();

			return Optional.of(DBFormLocalSignupButtonElement.builder()
					.withFormContentsRelated(formElement.isFormContentsRelated())
					.withType(formElement.getType()
							.name())
					.build());
		}
	}

	public static class FormParameterElementMapper
	{
		public static Optional<FormElement> map(DBFormElement restFormElement)
		{
			if (!(restFormElement instanceof DBFormParameterElement))
				return Optional.empty();

			return Optional.of(new FormParameterElement(FormLayoutElement.valueOf(restFormElement.type),
					((DBFormParameterElement) restFormElement).index));
		}

		public static Optional<DBFormElement> map(FormElement formElement)
		{
			if (!(formElement instanceof FormParameterElement))
				return Optional.empty();

			return Optional.of(DBFormParameterElement.builder()
					.withFormContentsRelated(formElement.isFormContentsRelated())
					.withType(formElement.getType()
							.name())
					.withIndex(((FormParameterElement) formElement).getIndex())
					.build());
		}
	}

	public static class FormSeparatorElementMapper
	{
		public static Optional<FormElement> map(DBFormElement restFormElement)
		{
			if (!(restFormElement instanceof DBFormSeparatorElement))
				return Optional.empty();

			return Optional.of(new FormSeparatorElement());
		}

		public static Optional<DBFormElement> map(FormElement formElement)
		{
			if (!(formElement instanceof FormSeparatorElement))
				return Optional.empty();

			return Optional.of(DBFormSeparatorElement.builder()
					.withFormContentsRelated(formElement.isFormContentsRelated())
					.withType(formElement.getType()
							.name())
					.build());
		}
	}

}
