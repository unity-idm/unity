/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.files;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.nio.charset.Charset;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalURIException;

/**
 * 
 * @author P.Piernik
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServerFileRestrict.conf" })
public class FileStorageServiceBaseTest
{
	@Autowired
	private FileStorageService storageService;

	@Test
	public void testSaveAndRead() throws EngineException
	{
		URI uri = storageService.storageFile("demo".getBytes(), "oType", "oId");
		FileData fileData = storageService.readURI(uri);
		assertThat(fileData, is(notNullValue()));
		String s = new String(fileData.getContents(), Charset.defaultCharset());
		assertThat(s, is("demo"));
	}

	@Test
	public void shouldThrowExceptionWhenFileNotExists()
	{
		String uri = "file:notExists.txt";
		Throwable exception = catchThrowable(() -> storageService.readURI(URIHelper.parseURI(uri)));
		assertExceptionType(exception, EngineException.class);
	}

	@Test
	public void shouldReadDataUri() throws IllegalURIException, EngineException
	{
		String uri = "data:xx";
		
		FileData fdata = storageService.readURI(URIHelper.parseURI(uri));
		assertThat(fdata.getContents(), is(notNullValue()));
	}

	@Test
	public void shouldReadUrl() throws IllegalURIException, EngineException
	{
		String uri = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_92x30dp.png";
		FileData fdata = storageService.readURI(URIHelper.parseURI(uri));
		assertThat(fdata.getContents(), is(notNullValue()));
	}

	private void assertExceptionType(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
	}
}
