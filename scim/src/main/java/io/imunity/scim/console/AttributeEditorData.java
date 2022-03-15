/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.List;
import java.util.Collections;

class AttributeEditorData
{
	final List<String> attributeTypes;
	final List<String> identityTypes;

	private AttributeEditorData(Builder builder)
	{
		this.attributeTypes = builder.attributeTypes;
		this.identityTypes = builder.identityTypes;
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private List<String> attributeTypes = Collections.emptyList();
		private List<String> identityTypes = Collections.emptyList();

		private Builder()
		{
		}

		Builder withAttributeTypes(List<String> attributeTypes)
		{
			this.attributeTypes = attributeTypes;
			return this;
		}

		Builder withIdentityTypes(List<String> identityTypes)
		{
			this.identityTypes = identityTypes;
			return this;
		}

		AttributeEditorData build()
		{
			return new AttributeEditorData(this);
		}
	}

}
