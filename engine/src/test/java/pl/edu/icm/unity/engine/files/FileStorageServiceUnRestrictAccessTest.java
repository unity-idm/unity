/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.files;

import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIHelper;

/**
 * 
 * @author P.Piernik
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServerFileFree.conf" })
public class FileStorageServiceUnRestrictAccessTest
{

	@Autowired
	private FileStorageService storageService;

	@Test
	public void shouldReadFileBeyondRoot()
	{
		String uri = "file:/../pom.xml";
		Throwable exception = catchThrowable(() -> storageService.readURI(URIHelper.parseURI(uri), null));
		Assertions.assertThat(exception).isNull();
	}
}
