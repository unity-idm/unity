/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.files.logo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.RemoteFileData;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

@ExtendWith(MockitoExtension.class)
public class RemoteLogoCacheDownloaderTest
{
	@Mock
	private UnityServerConfiguration conf;
	@Mock
	private MessageSource msg;
	@Mock
	private URIAccessService uriAccessService;
	@Mock
	private ExecutorsService executorsService;

	@TempDir
	private Path workspaceRoot;

	private ExecutorService realExecutor;
	private RemoteLogoCacheDownloader tested;

	@BeforeEach
	public void setup()
	{
		when(conf.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY)).thenReturn(workspaceRoot.toString());
		when(conf.getIntValue(UnityServerConfiguration.BULK_FILES_DOWNLOAD_TIMEOUT)).thenReturn(5000);
		when(conf.getIntValue(UnityServerConfiguration.BULK_FILES_CONNECTION_TIMEOUT)).thenReturn(3000);
		when(msg.getLocale()).thenReturn(Locale.forLanguageTag("en"));

		realExecutor = Executors.newFixedThreadPool(2);
		when(executorsService.getExecutionService()).thenReturn(realExecutor);

		tested = new RemoteLogoCacheDownloader(conf, msg, uriAccessService, executorsService);
	}

	@AfterEach
	public void tearDown()
	{
		realExecutor.shutdownNow();
	}

	@Test
	public void shouldSaveLogoFromDataURI()
	{
		LogoCacheKey key = () -> "provider1";
		String dataURI = "data:image/png;base64," + base64("imageBytes");

		tested.downloadLogoFilesAsync("samlIdpLogos", "federation1", Map.of(key, Map.of("", dataURI)), "MAIN")
				.join();

		Path catalog = catalogDir("samlIdpLogos", "federation1");
		assertThat(catalog.resolve("provider1en")).exists();
		assertThat(readString(catalog.resolve("provider1en"))).isEqualTo("png");
		assertThat(catalog.resolve("provider1en.png")).exists();
		assertThat(readBytes(catalog.resolve("provider1en.png"))).isEqualTo("imageBytes".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void shouldSaveLogoDownloadedThroughURIAccessService() throws IOException
	{
		LogoCacheKey key = () -> "provider1";
		RemoteFileData remoteFileData = new RemoteFileData("https://example.com/logo", "content".getBytes(StandardCharsets.UTF_8),
				null, "image/jpeg");
		when(uriAccessService.readURL(any(), eq("MAIN"), any(Duration.class), any(Duration.class), anyInt()))
				.thenReturn(remoteFileData);

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
				Map.of(key, Map.of("", "https://example.com/logo")), "MAIN").join();

		Path catalog = catalogDir("oauthFederationLogos", "federation1");
		assertThat(catalog.resolve("provider1en.jpeg")).exists();
		assertThat(readBytes(catalog.resolve("provider1en.jpeg"))).isEqualTo("content".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void shouldPassGivenTruststoreNameToURIAccessService() throws IOException
	{
		LogoCacheKey key = () -> "provider1";
		when(uriAccessService.readURL(any(), eq("customTruststore"), any(Duration.class), any(Duration.class), anyInt()))
				.thenReturn(new RemoteFileData("https://example.com/logo", "c".getBytes(StandardCharsets.UTF_8), null,
						"image/png"));

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
				Map.of(key, Map.of("", "https://example.com/logo")), "customTruststore").join();

		verify(uriAccessService).readURL(any(), eq("customTruststore"), any(Duration.class), any(Duration.class), anyInt());
		assertThat(catalogDir("oauthFederationLogos", "federation1").resolve("provider1en.png")).exists();
	}

	@Test
	public void shouldNotPropagateExceptionWhenSingleLogoFailsToDownload() throws IOException
	{
		LogoCacheKey key = () -> "provider1";
		when(uriAccessService.readURL(any(), any(), any(Duration.class), any(Duration.class), anyInt()))
				.thenThrow(new RuntimeException("simulated network failure"));

		assertThatCode(() -> tested
				.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
						Map.of(key, Map.of("", "https://example.com/logo")), "MAIN")
				.join())
				.doesNotThrowAnyException();

		assertThat(catalogDir("oauthFederationLogos", "federation1").resolve("provider1en.png")).doesNotExist();
	}

	@Test
	public void shouldNotRemoveExistingCachedFileWhenRedownloadFails() throws IOException
	{
		LogoCacheKey key = () -> "provider1";
		Path catalog = catalogDir("oauthFederationLogos", "federation1");
		Files.createDirectories(catalog);
		Files.writeString(catalog.resolve("provider1en"), "png", StandardCharsets.UTF_8);
		Files.write(catalog.resolve("provider1en.png"), "oldContent".getBytes(StandardCharsets.UTF_8));

		when(uriAccessService.readURL(any(), any(), any(Duration.class), any(Duration.class), anyInt()))
				.thenThrow(new RuntimeException("simulated network failure"));

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
				Map.of(key, Map.of("", "https://example.com/logo")), "MAIN").join();

		assertThat(catalog.resolve("provider1en.png")).exists();
		assertThat(readBytes(catalog.resolve("provider1en.png"))).isEqualTo("oldContent".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void shouldRemoveLogosNoLongerReferencedInNewRun() throws IOException
	{
		Path catalog = catalogDir("oauthFederationLogos", "federation1");
		Files.createDirectories(catalog);
		Files.writeString(catalog.resolve("removedProviderEn"), "png", StandardCharsets.UTF_8);
		Files.write(catalog.resolve("removedProviderEn.png"), "stale".getBytes(StandardCharsets.UTF_8));

		LogoCacheKey survivingKey = () -> "provider1";
		String dataURI = "data:image/png;base64," + base64("fresh");

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1", Map.of(survivingKey, Map.of("", dataURI)),
				"MAIN").join();

		assertThat(catalog.resolve("removedProviderEn")).doesNotExist();
		assertThat(catalog.resolve("removedProviderEn.png")).doesNotExist();
		assertThat(catalog.resolve("provider1en.png")).exists();
	}

	@Test
	public void shouldReplaceOldExtensionWhenLogoExtensionChanges() throws IOException
	{
		Path catalog = catalogDir("oauthFederationLogos", "federation1");
		Files.createDirectories(catalog);
		Files.writeString(catalog.resolve("provider1en"), "jpeg", StandardCharsets.UTF_8);
		Files.write(catalog.resolve("provider1en.jpeg"), "oldPng".getBytes(StandardCharsets.UTF_8));

		LogoCacheKey key = () -> "provider1";
		String dataURI = "data:image/png;base64," + base64("newPng");

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1", Map.of(key, Map.of("", dataURI)), "MAIN")
				.join();

		assertThat(catalog.resolve("provider1en.jpeg")).doesNotExist();
		assertThat(catalog.resolve("provider1en.png")).exists();
		assertThat(readString(catalog.resolve("provider1en"))).isEqualTo("png");
	}

	private Path catalogDir(String cacheGroup, String namespaceId)
	{
		return workspaceRoot.resolve("downloadedLogos")
				.resolve(cacheGroup)
				.resolve(LogoFilenameUtils.namespaceDirName(namespaceId));
	}

	private static String base64(String content)
	{
		return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
	}

	private static String readString(Path path)
	{
		try
		{
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static byte[] readBytes(Path path)
	{
		try
		{
			return Files.readAllBytes(path);
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
