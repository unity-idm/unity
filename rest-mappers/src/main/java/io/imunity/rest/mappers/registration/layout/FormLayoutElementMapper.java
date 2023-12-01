/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration.layout;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.imunity.rest.api.types.registration.layout.RestBasicFormElement;
import io.imunity.rest.api.types.registration.layout.RestFormCaptionElement;
import io.imunity.rest.api.types.registration.layout.RestFormElement;
import io.imunity.rest.api.types.registration.layout.RestFormLocalSignupButtonElement;
import io.imunity.rest.api.types.registration.layout.RestFormParameterElement;
import io.imunity.rest.api.types.registration.layout.RestFormSeparatorElement;
import io.imunity.rest.mappers.I18nStringMapper;
import pl.edu.icm.unity.base.registration.layout.BasicFormElement;
import pl.edu.icm.unity.base.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.base.registration.layout.FormElement;
import pl.edu.icm.unity.base.registration.layout.FormLayoutElement;
import pl.edu.icm.unity.base.registration.layout.FormLocalSignupButtonElement;
import pl.edu.icm.unity.base.registration.layout.FormParameterElement;
import pl.edu.icm.unity.base.registration.layout.FormSeparatorElement;

public class FormLayoutElementMapper
{
	private final static List<Function<FormElement, Optional<RestFormElement>>> mappers = List.of(
			BasicFormElementMapper::map, FormCaptionElementMapper::map, FormLocalSignupButtonElementMapper::map,
			FormParameterElementMapper::map, FormSeparatorElementMapper::map);

	private final static List<Function<RestFormElement, Optional<FormElement>>> mappersFrom = List.of(
			BasicFormElementMapper::map, FormCaptionElementMapper::map, FormLocalSignupButtonElementMapper::map,
			FormParameterElementMapper::map, FormSeparatorElementMapper::map);

	public static RestFormElement map(FormElement basicFormElement)
	{
		for (Function<FormElement, Optional<RestFormElement>> mapper : mappers)
		{
			Optional<RestFormElement> apply = mapper.apply(basicFormElement);
			if (apply.isPresent())
				return apply.get();
		}

		throw new IllegalArgumentException("Can not find mapper for type " + basicFormElement.getClass()
				.getName());

	}

	public static FormElement map(RestFormElement restFormElement)
	{

		for (Function<RestFormElement, Optional<FormElement>> mapper : mappersFrom)
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
		public static Optional<FormElement> map(RestFormElement restBasicFormElement)
		{
			if (!(restBasicFormElement instanceof RestBasicFormElement))
				return Optional.empty();

			return Optional.of(new BasicFormElement(FormLayoutElement.valueOf(restBasicFormElement.type)));
		}

		public static Optional<RestFormElement> map(FormElement basicFormElement)
		{
			if (!(basicFormElement instanceof BasicFormElement))
				return Optional.empty();

			return Optional.of(RestBasicFormElement.builder()
					.withFormContentsRelated(basicFormElement.isFormContentsRelated())
					.withType(basicFormElement.getType()
							.name())
					.build());
		}
	}

	public static class FormCaptionElementMapper
	{
		public static Optional<FormElement> map(RestFormElement restFormElement)
		{
			if (!(restFormElement instanceof RestFormCaptionElement))
				return Optional.empty();

			return Optional
					.of(new FormCaptionElement(Optional.ofNullable(((RestFormCaptionElement) restFormElement).value)
							.map(I18nStringMapper::map)
							.orElse(null)));
		}

		public static Optional<RestFormElement> map(FormElement formElement)
		{
			if (!(formElement instanceof FormCaptionElement))
				return Optional.empty();

			return Optional.of(RestFormCaptionElement.builder()
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
		public static Optional<FormElement> map(RestFormElement restFormElement)
		{
			if (!(restFormElement instanceof RestFormLocalSignupButtonElement))
				return Optional.empty();

			return Optional.of(new FormLocalSignupButtonElement());
		}

		public static Optional<RestFormElement> map(FormElement formElement)
		{
			if (!(formElement instanceof FormLocalSignupButtonElement))
				return Optional.empty();

			return Optional.of(RestFormLocalSignupButtonElement.builder()
					.withFormContentsRelated(formElement.isFormContentsRelated())
					.withType(formElement.getType()
							.name())
					.build());
		}
	}

	public static class FormParameterElementMapper
	{
		public static Optional<FormElement> map(RestFormElement restFormElement)
		{
			if (!(restFormElement instanceof RestFormParameterElement))
				return Optional.empty();

			return Optional.of(new FormParameterElement(FormLayoutElement.valueOf(restFormElement.type),
					((RestFormParameterElement) restFormElement).index));
		}

		public static Optional<RestFormElement> map(FormElement formElement)
		{
			if (!(formElement instanceof FormParameterElement))
				return Optional.empty();

			return Optional.of(RestFormParameterElement.builder()
					.withFormContentsRelated(formElement.isFormContentsRelated())
					.withType(formElement.getType()
							.name())
					.withIndex(((FormParameterElement) formElement).getIndex())
					.build());
		}
	}

	public static class FormSeparatorElementMapper
	{
		public static Optional<FormElement> map(RestFormElement restFormElement)
		{
			if (!(restFormElement instanceof RestFormSeparatorElement))
				return Optional.empty();

			return Optional.of(new FormSeparatorElement());
		}

		public static Optional<RestFormElement> map(FormElement formElement)
		{
			if (!(formElement instanceof FormSeparatorElement))
				return Optional.empty();

			return Optional.of(RestFormSeparatorElement.builder()
					.withFormContentsRelated(formElement.isFormContentsRelated())
					.withType(formElement.getType()
							.name())
					.build());
		}
	}

}
