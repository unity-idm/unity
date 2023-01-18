/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.layout;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestBasicFormElement.Builder.class)
public class RestBasicFormElement extends RestFormElement
{

	private RestBasicFormElement(Builder builder)
	{
		super(builder);
	}

	protected RestBasicFormElement(RestFormElementBuilder<?> builder)
	{
		super(builder);
	}

	@Override
	public int hashCode()
	{
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestFormElementBuilder<Builder>
	{

		private Builder()
		{
			super("pl.edu.icm.unity.types.registration.layout.BasicFormElement");
			withFormContentsRelated(true);
		}

		public RestBasicFormElement build()
		{
			return new RestBasicFormElement(this);
		}
	}

}
