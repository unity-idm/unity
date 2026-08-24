/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.files.logo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.RemoteFileData;

public class LogoFilenameUtilsTest
{
	private final LogoCacheKey key = () -> "keyBasename";

	@Test
	public void shouldConcatenateBasenameAndLocaleString()
	{
		String result = LogoFilenameUtils.getLogoFileBasename(key, "en");

		assertThat(result).isEqualTo("keyBasename~en");
	}

	@Test
	public void shouldUseGivenLocaleWhenNotBlank()
	{
		String result = LogoFilenameUtils.getLogoFileBasename(key, Locale.forLanguageTag("de"), "en");

		assertThat(result).isEqualTo("keyBasename~de");
	}

	@Test
	public void shouldFallBackToDefaultLocaleWhenLocaleIsNull()
	{
		String result = LogoFilenameUtils.getLogoFileBasename(key, null, "en");

		assertThat(result).isEqualTo("keyBasename~en");
	}

	@Test
	public void shouldFallBackToDefaultLocaleWhenLocaleIsBlank()
	{
		String result = LogoFilenameUtils.getLogoFileBasename(key, Locale.ROOT, "en");

		assertThat(result).isEqualTo("keyBasename~en");
	}

	@Test
	public void shouldJoinWorkspaceDirectoryWithLogosRootDir()
	{
		UnityServerConfiguration conf = mock(UnityServerConfiguration.class);
		when(conf.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY)).thenReturn("/data/workspace");

		String result = LogoFilenameUtils.getLogosWorkspaceRoot(conf);

		assertThat(result).isEqualTo("/data/workspace/downloadedLogos");
	}

	@Test
	public void shouldReturnMd5HexOfNamespaceId()
	{
		String result = LogoFilenameUtils.namespaceDirName("myFederationId");

		assertThat(result).isEqualTo("82faf1354fbd39455bff4ec14b53998d");
	}

	@Test
	public void shouldReturnSameDirNameForSameNamespaceId()
	{
		assertThat(LogoFilenameUtils.namespaceDirName("sameId"))
				.isEqualTo(LogoFilenameUtils.namespaceDirName("sameId"));
	}

	@Test
	public void shouldDecodeExtensionFromDataURI()
	{
		String extension = LogoFilenameUtils.getExtensionFromDataURI(URI.create("data:image/png;base64,AAAA"));

		assertThat(extension).isEqualTo("png");
	}

	@Test
	public void shouldTranslateJpgMimeTypeToJpegExtension()
	{
		String extension = LogoFilenameUtils.getExtensionFromDataURI(URI.create("data:image/jpg;base64,AAAA"));

		assertThat(extension).isEqualTo("jpeg");
	}

	@Test
	public void shouldTranslateSvgMimeTypeToSvgExtension()
	{
		String extension = LogoFilenameUtils.getExtensionFromDataURI(URI.create("data:image/svg+xml;base64,AAAA"));

		assertThat(extension).isEqualTo("svg");
	}

	@Test
	public void shouldFailToDecodeExtensionWhenDataURIHasNoMimeType()
	{
		assertThatThrownBy(() -> LogoFilenameUtils.getExtensionFromDataURI(URI.create("data:,AAAA")))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void shouldFailToDecodeExtensionWhenMimeTypeIsUnknown()
	{
		assertThatThrownBy(
				() -> LogoFilenameUtils.getExtensionFromDataURI(URI.create("data:application/pdf;base64,AAAA")))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void shouldFailToDecodeExtensionWhenURIIsNotDataURI()
	{
		assertThatThrownBy(() -> LogoFilenameUtils.getExtensionFromDataURI(URI.create("https://example.com/a.png")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void shouldGetExtensionFromMimeTypeOfRemoteFile()
	{
		RemoteFileData remoteFile = new RemoteFileData("https://example.com/logo", null, null, "image/png");

		String extension = LogoFilenameUtils.getExtensionForRemoteFile(remoteFile);

		assertThat(extension).isEqualTo("png");
	}

	@Test
	public void shouldStripParametersFromMimeTypeOfRemoteFile()
	{
		RemoteFileData remoteFile = new RemoteFileData("https://example.com/logo", null, null,
				"image/png; charset=utf-8");

		String extension = LogoFilenameUtils.getExtensionForRemoteFile(remoteFile);

		assertThat(extension).isEqualTo("png");
	}

	@Test
	public void shouldFallBackToPathExtensionWhenMimeTypeIsUnknown()
	{
		RemoteFileData remoteFile = new RemoteFileData("https://example.com/path/logo.gif", null, null,
				"application/octet-stream");

		String extension = LogoFilenameUtils.getExtensionForRemoteFile(remoteFile);

		assertThat(extension).isEqualTo("gif");
	}

	@Test
	public void shouldFallBackToPathExtensionWhenMimeTypeIsNull()
	{
		RemoteFileData remoteFile = new RemoteFileData("https://example.com/path/logo.jpeg", null, null, null);

		String extension = LogoFilenameUtils.getExtensionForRemoteFile(remoteFile);

		assertThat(extension).isEqualTo("jpeg");
	}

	@Test
	public void shouldIgnoreTrailingSlashWhenFallingBackToPathExtension()
	{
		RemoteFileData remoteFile = new RemoteFileData("https://example.com/path/logo.png/", null, null, null);

		String extension = LogoFilenameUtils.getExtensionForRemoteFile(remoteFile);

		assertThat(extension).isEqualTo("png");
	}

	@Test
	public void shouldFailWhenNeitherMimeTypeNorPathGiveExtension()
	{
		RemoteFileData remoteFile = new RemoteFileData("https://example.com/path/logo", null, null,
				"application/octet-stream");

		assertThatThrownBy(() -> LogoFilenameUtils.getExtensionForRemoteFile(remoteFile))
				.isInstanceOf(IllegalStateException.class);
	}
}
