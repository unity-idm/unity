/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "clazz")
@JsonSubTypes(
{ @JsonSubTypes.Type(value = DBBasicFormElement.class, name = "pl.edu.icm.unity.types.registration.layout.BasicFormElement"),
		@JsonSubTypes.Type(value = DBFormCaptionElement.class, name = "pl.edu.icm.unity.types.registration.layout.FormCaptionElement"),
		@JsonSubTypes.Type(value = DBFormParameterElement.class, name = "pl.edu.icm.unity.types.registration.layout.FormParameterElement"),
		@JsonSubTypes.Type(value = DBFormLocalSignupButtonElement.class, name = "pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement"),
		@JsonSubTypes.Type(value = DBFormSeparatorElement.class, name = "pl.edu.icm.unity.types.registration.layout.FormSeparatorElement") })

public abstract class DBFormElement
{
	public final String clazz;
	public final String type;
	public final boolean formContentsRelated;

	protected DBFormElement(RestFormElementBuilder<?> builder)
	{
		this.clazz = builder.clazz;
		this.type = builder.type;
		this.formContentsRelated = builder.formContentsRelated;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(clazz, formContentsRelated, type);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBFormElement other = (DBFormElement) obj;
		return Objects.equals(clazz, other.clazz) && formContentsRelated == other.formContentsRelated
				&& Objects.equals(type, other.type);
	}

	public static class RestFormElementBuilder<T extends RestFormElementBuilder<?>>
	{
		private String clazz;
		private String type;
		private boolean formContentsRelated;

		protected RestFormElementBuilder(String clazz)
		{
			this.clazz = clazz;
		}

		@SuppressWarnings("unchecked")
		public T withClazz(String clazz)
		{
			this.clazz = clazz;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withType(String type)
		{
			this.type = type;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withFormContentsRelated(boolean formContentsRelated)
		{
			this.formContentsRelated = formContentsRelated;
			return (T) this;
		}
	}

}
