/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBBasicFormElement.Builder.class)
public class DBBasicFormElement extends DBFormElement
{

	private DBBasicFormElement(Builder builder)
	{
		super(builder);
	}

	protected DBBasicFormElement(RestFormElementBuilder<?> builder)
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

		public DBBasicFormElement build()
		{
			return new DBBasicFormElement(this);
		}
	}

}
