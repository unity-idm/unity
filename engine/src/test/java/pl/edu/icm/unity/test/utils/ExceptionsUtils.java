/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.test.utils;

import org.assertj.core.api.Assertions;

/**
 * 
 * @author P.Piernik
 *
 */
public class ExceptionsUtils
{
	public static void assertExceptionType(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
	}
}
