/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = DBFormLocalSignupButtonElement.Builder.class)
public class DBFormLocalSignupButtonElement extends DBFormElement
{

	private DBFormLocalSignupButtonElement(Builder builder)
	{
		super(builder);
	}

	protected DBFormLocalSignupButtonElement(RestFormElementBuilder<?> builder)
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

		public DBFormLocalSignupButtonElement build()
		{
			return new DBFormLocalSignupButtonElement(this);
		}
	}

}
