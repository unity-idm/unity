/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.files;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.test.utils.ExceptionsUtils;

/**
 * 
 * @author P.Piernik
 *
 */
@ExtendWith(MockitoExtension.class)
public class URIAccessServiceRestrictAccessTest
{
	@Mock
	private UnityServerConfiguration conf;
	@Mock
	private FileDAO dao;
	@Mock
	private PKIManagement pkiMan;
	
	private URIAccessService uriService;
	
	@BeforeEach
	public void init()
	{
		when(conf.getBooleanValue(eq(UnityServerConfiguration.RESTRICT_FILE_SYSTEM_ACCESS))).thenReturn(true);
		when(conf.getValue(eq(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH))).thenReturn("target");
		uriService = new URIAccessServiceImpl(conf, dao, pkiMan);
	}
	
	@Test
	public void shouldRestrictAccessToFileBeyondRoot()
	{
		String uri = "file:/../pom.xml";
		Throwable exception = catchThrowable(() -> uriService.readURI(URIHelper.parseURI(uri), null));
		ExceptionsUtils.assertExceptionType(exception, URIAccessException.class);
	}

	@Test
	public void shouldRestrictAccessToImageFileBeyondRoot()
	{
		String uri = "file:/../pom.xml";
		Throwable exception = catchThrowable(
				() -> uriService.readImageURI(URIHelper.parseURI(uri), "theme1"));
		ExceptionsUtils.assertExceptionType(exception, URIAccessException.class);
	}

	@Test
	public void shouldReadFileInRootPath()
	{
		String webContent = "target";
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

		Throwable exception = catchThrowable(() -> uriService.readURI(URIHelper.parseURI(URLEncoder.encode(toTest.toString(), "UTF-8")), null));
		Assertions.assertThat(exception).isNull();
	}
}
