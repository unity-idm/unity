/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.files;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.store.api.FileDAO;

/**
 * 
 * @author P.Piernik
 *
 */
@ExtendWith(MockitoExtension.class)
public class URIAccessServiceUnRestrictAccessTest
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
		when(conf.getBooleanValue(eq(UnityServerConfiguration.RESTRICT_FILE_SYSTEM_ACCESS))).thenReturn(false);
		when(conf.getValue(eq(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH))).thenReturn("target");
		uriService = new URIAccessServiceImpl(conf, dao, pkiMan);
	}

	@Test
	public void shouldReadFileBeyondRoot()
	{
		String uri = "file:/../pom.xml";
		Throwable exception = catchThrowable(() -> uriService.readURI(URIHelper.parseURI(uri), null));
		Assertions.assertThat(exception).isNull();
	}
}
