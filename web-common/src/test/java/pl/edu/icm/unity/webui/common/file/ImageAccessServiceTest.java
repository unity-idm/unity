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
		assertThat(imageAccessService.getEditableImageResourceFromUriWithUnknownTheme("xx:corrupted"), is(Optional.empty()));
	}

	@Test
	public void shouldReturnRemoteResourceIntact()
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		assertThat(imageAccessService.getEditableImageResourceFromUriWithUnknownTheme("http:ok").get().getRemote(),
				is("http:ok"));
	}

	@Test
	public void shouldReturnOnlyLocalUriResource() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		when(uriAccessService.readImageURI(any(URI.class), anyString())).thenThrow(new URIAccessException(""));
		Optional<LocalOrRemoteResource> res = imageAccessService
				.getEditableImageResourceFromUriWithUnknownTheme("invalidFilePath");
		assertThat(res.get().getLocal(), nullValue());
		assertThat(res.get().getLocalUri(), is("invalidFilePath"));
	}

	@Test
	public void shouldReturnFullLocalResource() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		when(uriAccessService.readImageURI(any(URI.class), anyString()))
				.thenReturn(new FileData("testUri", "test".getBytes(), new Date()));
		Optional<LocalOrRemoteResource> res = imageAccessService.getEditableImageResourceFromUriWithUnknownTheme("testUri");
		assertThat(new String(res.get().getLocal()), is("test"));
		assertThat(res.get().getLocalUri(), is("testUri"));
	}

	@Test
	public void shouldUseUnknownTheme() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		imageAccessService.getEditableImageResourceFromUriWithUnknownTheme("testUri");
		verify(uriAccessService).readImageURI(any(URI.class), eq("UNKNOWN_THEME"));
	}

	@Test
	public void shouldReturnEmptyForEmptyLogoUri() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		Optional<LocalOrRemoteResource> res = imageAccessService.getEditableImageResourceFromUriWithUnknownTheme("");
		assertThat(res.isPresent(), is(false));
	}

	@Test
	public void shouldReturnEmptyForNulledLogoUri() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		Optional<LocalOrRemoteResource> res = imageAccessService.getEditableImageResourceFromUriWithUnknownTheme(null);
		assertThat(res.isPresent(), is(false));
	}
}
