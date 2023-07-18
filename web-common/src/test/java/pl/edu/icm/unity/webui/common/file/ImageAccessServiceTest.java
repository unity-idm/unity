/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.engine.api.files.IllegalURIException;
import pl.edu.icm.unity.engine.api.files.URIAccessException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

@ExtendWith(MockitoExtension.class)
public class ImageAccessServiceTest
{
	@Mock
	URIAccessService uriAccessService;

	@Test
	public void shouldReturnNullWhenUriIsCorrupted()
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		assertThat(imageAccessService.getEditableImageResourceFromUriWithUnknownTheme("xx:corrupted")).isEqualTo(Optional.empty());
	}

	@Test
	public void shouldReturnRemoteResourceIntact()
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		assertThat(imageAccessService.getEditableImageResourceFromUriWithUnknownTheme("http:ok").get().getRemote()).
				isEqualTo("http:ok");
	}

	@Test
	public void shouldReturnOnlyLocalUriResource() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		when(uriAccessService.readImageURI(any(URI.class), anyString())).thenThrow(new URIAccessException(""));
		Optional<LocalOrRemoteResource> res = imageAccessService
				.getEditableImageResourceFromUriWithUnknownTheme("invalidFilePath");
		assertThat(res.get().getLocal()).isNull();
		assertThat(res.get().getLocalUri()).isEqualTo("invalidFilePath");
	}

	@Test
	public void shouldReturnFullLocalResource() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		when(uriAccessService.readImageURI(any(URI.class), anyString()))
				.thenReturn(new FileData("testUri", "test".getBytes(), new Date()));
		Optional<LocalOrRemoteResource> res = imageAccessService.getEditableImageResourceFromUriWithUnknownTheme("testUri");
		assertThat(new String(res.get().getLocal())).isEqualTo("test");
		assertThat(res.get().getLocalUri()).isEqualTo("testUri");
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
		assertThat(res.isPresent()).isEqualTo(false);
	}

	@Test
	public void shouldReturnEmptyForNulledLogoUri() throws IllegalURIException
	{
		ImageAccessService imageAccessService = new ImageAccessService(uriAccessService);
		Optional<LocalOrRemoteResource> res = imageAccessService.getEditableImageResourceFromUriWithUnknownTheme(null);
		assertThat(res.isPresent()).isEqualTo(false);
	}
}
