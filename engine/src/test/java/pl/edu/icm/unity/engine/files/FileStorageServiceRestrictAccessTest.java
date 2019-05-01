/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.files;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.exceptions.EngineException;

@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServerFileRestrict.conf" })
public class FileStorageServiceRestrictAccessTest
{

	@Autowired
	private FileStorageService storageService;

	@Autowired
	private UnityServerConfiguration config;

	@Test
	public void shouldRestrictAccessToFileBeyondRoot()
	{
		String uri = "file:/../pom.xml";
		Throwable exception = catchThrowable(() -> storageService.readURI(URIHelper.parseURI(uri)));
		assertExceptionType(exception, EngineException.class);
	}

	@Test
	public void shouldRestrictAccessToImageFileBeyondRoot()
	{
		String uri = "file:/../pom.xml";
		Throwable exception = catchThrowable(
				() -> storageService.readImageURI(URIHelper.parseURI(uri), "theme1"));
		assertExceptionType(exception, EngineException.class);
	}

	@Test
	public void shouldReadFileInRootPath()
	{
		String webContent = config.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		Path toTest = Paths.get(webContent, "text.txt");
		if (!toTest.toFile().exists())
		{
			try
			{
				toTest.toFile().createNewFile();
			} catch (IOException e)
			{
				fail("Can not create test file");
			}
		}

		String uri = "text.txt";
		Throwable exception = catchThrowable(() -> storageService.readURI(URIHelper.parseURI(uri)));
		Assertions.assertThat(exception).isNull();
	}

	private void assertExceptionType(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
	}

}
