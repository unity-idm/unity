/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common.grid;

import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Grid entry which can be filtered
 * @author P.Piernik
 *
 */
public interface FilterableEntry
{
	 boolean anyFieldContains(String searched, MessageSource msg);
}
