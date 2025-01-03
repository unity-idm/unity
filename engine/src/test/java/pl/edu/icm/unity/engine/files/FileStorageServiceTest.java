/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.test.utils.ExceptionsUtils;

/**
 * 
 * @author P.Piernik
 *
 */

@ExtendWith(MockitoExtension.class)
public class FileStorageServiceTest
{
	@Mock
	private UnityServerConfiguration conf;
	@Mock
	private FileDAO dao;
	@Mock
	private PKIManagement pkiMan;
	@Mock
	private RemoteFileNetworkClient networkClient;

	private FileStorageService fileService;

	@BeforeEach
	public void init()
	{
		when(conf.getValue(eq(UnityServerConfiguration.WORKSPACE_DIRECTORY))).thenReturn("target/workspace");
		when(conf.getIntValue(eq(UnityServerConfiguration.FILE_SIZE_LIMIT))).thenReturn(4);
		fileService = new FileStorageServiceImpl(conf, dao, pkiMan);
	}

	@Test
	public void shouldSaveAndReadFile() throws EngineException
	{
		fileService.storeFile("demo".getBytes(), "o", "i");
		
		ArgumentCaptor<FileData> argument = ArgumentCaptor.forClass(FileData.class);
		verify(dao).create(argument.capture());
		assertThat(argument.getValue().getContents()).isEqualTo("demo".getBytes());
		assertThat(argument.getValue().getOwnerType()).isEqualTo("o");
		assertThat(argument.getValue().getOwnerId()).isEqualTo("i");
	}

	@Test
	public void shouldThrowExceptionWhenFileIsTooBig() throws EngineException
	{
		Throwable exception = catchThrowable(
				() -> fileService.storeFile("demodemodemodemo".getBytes(), "oType", "oId"));
		ExceptionsUtils.assertExceptionType(exception, EngineException.class);
	}

	@Test
	public void shouldSaveAndReadFromWorkspace() throws EngineException
	{

		fileService.storeFileInWorkspace(new String("test").getBytes(), "demo/demo/test.txt");
		FileData fileFromWorkspace = fileService.readFileFromWorkspace("demo/demo/test.txt");
		assertThat(new String(fileFromWorkspace.getContents())).isEqualTo("test");
	}

	@Test
	public void shouldBlockSaveBeyondWorkspace() throws EngineException
	{
		Throwable exception = catchThrowable(
				() -> fileService.storeFileInWorkspace(new String("test").getBytes(), "../test2.txt"));
		ExceptionsUtils.assertExceptionType(exception, EngineException.class);
	}
}
