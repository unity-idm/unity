/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import org.springframework.util.StringUtils;

/**
 * Intended to be used on top of a Vaadin component.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public interface ComponentWithLabel
{
	/**
	 * Depending upon context, this sets the caption or placeholder of a
	 * component.
	 */
	void setLabel(String label);
	
	static String normalizeLabel(String label)
	{
		if (StringUtils.isEmpty(label))
			return label;
		
		String normalizedLabel = label;
		if (label.endsWith(":"))
			normalizedLabel = label.substring(0, label.length() - 1);
		return normalizedLabel;
	}
}
