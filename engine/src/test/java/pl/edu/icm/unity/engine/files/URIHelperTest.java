/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.files;

import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.files.IllegalURIException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.test.utils.ExceptionsUtils;

/**
 * 
 * @author P.Piernik
 *
 */
public class URIHelperTest
{	
	@Test
	public void shouldParseCorrectURI() throws IllegalURIException
	{
		URIHelper.parseURI("demo.txt");
		URIHelper.parseURI("file:demo.txt");
		URIHelper.parseURI("data:xxx");
		URIHelper.parseURI("https:link");
		URIHelper.parseURI("http:link");
		URIHelper.parseURI( URIAccessService.UNITY_FILE_URI_SCHEMA + ".uuid");
	}
	
	@Test
	public void shouldFailWhenInvalidURI() throws IllegalURIException
	{
		Throwable exception = catchThrowable(() -> URIHelper.parseURI("xxx:xxx"));
		ExceptionsUtils.assertExceptionType(exception, IllegalURIException.class);	
	}
}
