/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.files.logo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

@ExtendWith(MockitoExtension.class)
public class CachedLogoFileLoaderTest
{
	@Mock
	private UnityServerConfiguration conf;
	@Mock
	private MessageSource msg;

	@TempDir
	private Path workspaceRoot;

	private final LogoCacheKey key = () -> "provider1";

	private CachedLogoFileLoader tested;
	private Path catalogDir;

	@BeforeEach
	public void setup()
	{
		when(conf.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY)).thenReturn(workspaceRoot.toString());
		lenient().when(msg.getLocale()).thenReturn(Locale.forLanguageTag("en"));

		tested = new CachedLogoFileLoader(conf, msg);
		catalogDir = workspaceRoot.resolve("downloadedLogos")
				.resolve("samlIdpLogos")
				.resolve(LogoFilenameUtils.namespaceDirName("federation1"));
	}

	@Test
	public void shouldReturnEmptyWhenCatalogDirDoesNotExist() throws IOException
	{
		Optional<java.io.File> result = tested.getFile("samlIdpLogos", "federation1", key, Locale.forLanguageTag("en"));

		assertThat(result).isEmpty();
	}

	@Test
	public void shouldReturnEmptyWhenPointerFileIsMissing() throws IOException
	{
		Files.createDirectories(catalogDir);

		Optional<java.io.File> result = tested.getFile("samlIdpLogos", "federation1", key, Locale.forLanguageTag("en"));

		assertThat(result).isEmpty();
	}

	@Test
	public void shouldReturnLocalizedFileWhenPointerForRequestedLocaleExists() throws IOException
	{
		Files.createDirectories(catalogDir);
		writePointerFile("provider1~de", "png");

		Optional<java.io.File> result = tested.getFile("samlIdpLogos", "federation1", key, Locale.forLanguageTag("de"));

		assertThat(result).isPresent();
		assertThat(result.get().toPath()).isEqualTo(catalogDir.resolve("provider1~de.png"));
	}

	@Test
	public void shouldFallBackToDefaultLocaleWhenLocalizedPointerIsMissing() throws IOException
	{
		Files.createDirectories(catalogDir);
		writePointerFile("provider1~en", "jpeg");

		Optional<java.io.File> result = tested.getFile("samlIdpLogos", "federation1", key, Locale.forLanguageTag("de"));

		assertThat(result).isPresent();
		assertThat(result.get().toPath()).isEqualTo(catalogDir.resolve("provider1~en.jpeg"));
	}

	@Test
	public void shouldFallBackToDefaultLocaleWhenRequestedLocaleIsNull() throws IOException
	{
		Files.createDirectories(catalogDir);
		writePointerFile("provider1~en", "gif");

		Optional<java.io.File> result = tested.getFile("samlIdpLogos", "federation1", key, null);

		assertThat(result).isPresent();
		assertThat(result.get().toPath()).isEqualTo(catalogDir.resolve("provider1~en.gif"));
	}

	@Test
	public void shouldReturnEmptyWhenNeitherLocalizedNorDefaultPointerExists() throws IOException
	{
		Files.createDirectories(catalogDir);
		writePointerFile("provider1~fr", "png");

		Optional<java.io.File> result = tested.getFile("samlIdpLogos", "federation1", key, Locale.forLanguageTag("de"));

		assertThat(result).isEmpty();
	}

	private void writePointerFile(String basename, String extension) throws IOException
	{
		Files.writeString(catalogDir.resolve(basename), extension, StandardCharsets.UTF_8);
	}
}
