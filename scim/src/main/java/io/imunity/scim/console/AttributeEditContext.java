/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

class AttributeEditContext
{
	final boolean disableComplexAndMulti;
	final AttributesEditMode attributesEditMode;

	private AttributeEditContext(Builder builder)
	{
		this.disableComplexAndMulti = builder.disableComplexAndMulti;
		this.attributesEditMode = builder.attributesEditMode;
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private boolean disableComplexAndMulti;
		private AttributesEditMode attributesEditMode;

		private Builder()
		{
		}

		Builder withDisableComplexAndMulti(boolean disableComplexAndMulti)
		{
			this.disableComplexAndMulti = disableComplexAndMulti;
			return this;
		}

		Builder withAttributesEditMode(AttributesEditMode attributesEditMode )
		{
			this.attributesEditMode = attributesEditMode;
			return this;
		}

		AttributeEditContext build()
		{
			return new AttributeEditContext(this);
		}
	}

}
