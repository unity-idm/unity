/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Date;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.engine.api.files.IllegalURIException;
import pl.edu.icm.unity.engine.api.files.URIAccessException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

@RunWith(MockitoJUnitRunner.class)
public class ImageAccessServiceTest
{
	@Mock
	URIAccessService uriAccessService;

	@Test
	public void shouldReturnNullWhenUriIsCorrupted()
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		assertThat(imageAccessService.getEditableImageResourceFromUriOrNull("xx:corrupted", Optional.empty()),
				nullValue());
	}

	@Test
	public void shouldReturnRemoteResource()
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		assertThat(imageAccessService.getEditableImageResourceFromUriOrNull("http:ok", Optional.empty())
				.getRemote(), is("http:ok"));
	}

	@Test
	public void shouldReturnOnlyLocalUriResource() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		when(uriAccessService.readImageURI(any(URI.class), anyString())).thenThrow(new URIAccessException(""));
		LocalOrRemoteResource res = imageAccessService.getEditableImageResourceFromUriOrNull("invalidFilePath",
				Optional.empty());
		assertThat(res.getLocal(), nullValue());
		assertThat(res.getLocalUri(), is("invalidFilePath"));
	}

	@Test
	public void shouldReturnFullLocalResource() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		when(uriAccessService.readImageURI(any(URI.class), anyString()))
				.thenReturn(new FileData("testUri", "test".getBytes(), new Date()));
		LocalOrRemoteResource res = imageAccessService.getEditableImageResourceFromUriOrNull("testUri",
				Optional.empty());
		assertThat(new String(res.getLocal()), is("test"));
		assertThat(res.getLocalUri(), is("testUri"));
	}

	@Test
	public void shouldUseUnknownTheme() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		imageAccessService.getEditableImageResourceFromUriOrNull("testUri", Optional.empty());
		verify(uriAccessService).readImageURI(any(URI.class), eq("UNKNOWN_THEME"));
	}
}
