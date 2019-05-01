/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.files;

import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.exceptions.IllegalURIException;

/**
 * 
 * @author P.Piernik
 *
 */
public class URIHelperTest
{
	@Test
	public void testParse() throws IllegalURIException
	{
		URIHelper.parseURI("demo.txt");
		URIHelper.parseURI("file:demo.txt");
		URIHelper.parseURI("data:xxx");
		URIHelper.parseURI("https:link");
		URIHelper.parseURI("http:link");
		URIHelper.parseURI( FileStorageServiceImpl.UNITY_FILE_URI_SCHEMA + ".uuid");
		Throwable exception = catchThrowable(() -> URIHelper.parseURI("xxx:xxx"));
		assertExceptionType(exception, IllegalURIException.class);	
	}


	private void assertExceptionType(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
	}
}
