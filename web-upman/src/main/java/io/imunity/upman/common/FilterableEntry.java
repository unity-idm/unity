/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.upman.common;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Grid entry which can be filtered
 * @author P.Piernik
 *
 */
public interface FilterableEntry
{
	 boolean anyFieldContains(String searched, UnityMessageSource msg);
}
