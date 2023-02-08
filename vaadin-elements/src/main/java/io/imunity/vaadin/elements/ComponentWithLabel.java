/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

public interface ComponentWithLabel
{
	static String normalizeLabel(String label)
	{
		if (!(label != null && !label.isEmpty()))
			return label;
		
		String normalizedLabel = label;
		if (label.endsWith(":"))
			normalizedLabel = label.substring(0, label.length() - 1);
		return normalizedLabel;
	}
}
