/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.layout;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestFormLocalSignupButtonElement.Builder.class)
public class RestFormLocalSignupButtonElement extends RestFormElement
{

	private RestFormLocalSignupButtonElement(Builder builder)
	{
		super(builder);
	}

	protected RestFormLocalSignupButtonElement(RestFormElementBuilder<?> builder)
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

		private static final String TYPE = "LOCAL_SIGNUP";
		private static final String CLASS_TYPE = "pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement";

		private Builder()
		{
			super(CLASS_TYPE);
			withType(TYPE);
			withFormContentsRelated(true);

		}

		public RestFormLocalSignupButtonElement build()
		{
			return new RestFormLocalSignupButtonElement(this);
		}
	}

}
