/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console.v8;

class AttributeEditContext
{
	final boolean disableComplexAndMulti;
	final boolean complexMultiParent;
	final AttributesEditMode attributesEditMode;

	private AttributeEditContext(Builder builder)
	{
		this.disableComplexAndMulti = builder.disableComplexAndMulti;
		this.complexMultiParent = builder.complexMultiParent;
		this.attributesEditMode = builder.attributesEditMode;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean disableComplexAndMulti;
		private boolean complexMultiParent;
		private AttributesEditMode attributesEditMode;

		private Builder()
		{
		}

		public Builder withDisableComplexAndMulti(boolean disableComplexAndMulti)
		{
			this.disableComplexAndMulti = disableComplexAndMulti;
			return this;
		}

		public Builder withComplexMultiParent(boolean complexMultiParent)
		{
			this.complexMultiParent = complexMultiParent;
			return this;
		}

		public Builder withAttributesEditMode(AttributesEditMode attributesEditMode)
		{
			this.attributesEditMode = attributesEditMode;
			return this;
		}

		public AttributeEditContext build()
		{
			return new AttributeEditContext(this);
		}
	}

}
