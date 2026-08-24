/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.files.logo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

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
	@Mock
	private Environment environment;

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
		when(conf.getIntValue(UnityServerConfiguration.BULK_FILES_MAX_SIZE)).thenReturn(1_000_000);
		when(conf.getIntValue(UnityServerConfiguration.BULK_FILES_MAX_CONCURRENT_DOWNLOADS)).thenReturn(4);
		when(msg.getLocale()).thenReturn(Locale.forLanguageTag("en"));
		when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

		realExecutor = Executors.newFixedThreadPool(2);
		when(executorsService.getExecutionService()).thenReturn(realExecutor);

		tested = new RemoteLogoCacheDownloader(conf, msg, uriAccessService, executorsService, environment);
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
		String dataURI = "data:image/png;base64," + base64(pngBytes("imageBytes"));

		tested.downloadLogoFilesAsync("samlIdpLogos", "federation1", Map.of(key, Map.of("", dataURI)), "MAIN")
				.join();

		Path catalog = catalogDir("samlIdpLogos", "federation1");
		assertThat(catalog.resolve("provider1~en")).exists();
		assertThat(readString(catalog.resolve("provider1~en"))).isEqualTo("png");
		assertThat(catalog.resolve("provider1~en.png")).exists();
		assertThat(readBytes(catalog.resolve("provider1~en.png"))).isEqualTo(pngBytes("imageBytes"));
	}

	@Test
	public void shouldRejectDataUriLogoWithContentNotMatchingDeclaredImageType()
	{
		LogoCacheKey key = () -> "provider1";
		String dataURI = "data:image/png;base64," + base64("notAnImage");

		tested.downloadLogoFilesAsync("samlIdpLogos", "federation1", Map.of(key, Map.of("", dataURI)), "MAIN")
				.join();

		assertThat(catalogDir("samlIdpLogos", "federation1").resolve("provider1~en.png")).doesNotExist();
	}

	@Test
	public void shouldRejectDataUriLogoExceedingMaxSize() throws IOException
	{
		when(conf.getIntValue(UnityServerConfiguration.BULK_FILES_MAX_SIZE)).thenReturn(4);
		RemoteLogoCacheDownloader smallLimitTested = new RemoteLogoCacheDownloader(conf, msg, uriAccessService, executorsService,
				environment);
		LogoCacheKey key = () -> "provider1";
		String dataURI = "data:image/png;base64," + base64(pngBytes("imageBytes"));

		smallLimitTested.downloadLogoFilesAsync("samlIdpLogos", "federation1", Map.of(key, Map.of("", dataURI)), "MAIN")
				.join();

		assertThat(catalogDir("samlIdpLogos", "federation1").resolve("provider1~en.png")).doesNotExist();
	}

	@Test
	public void shouldSaveLogoDownloadedThroughURIAccessService() throws IOException
	{
		LogoCacheKey key = () -> "provider1";
		RemoteFileData remoteFileData = new RemoteFileData("https://example.com/logo", jpegBytes("content"),
				null, "image/jpeg");
		when(uriAccessService.readURL(any(), eq("MAIN"), any(Duration.class), any(Duration.class), anyInt(), anyLong()))
				.thenReturn(remoteFileData);

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
				Map.of(key, Map.of("", "https://example.com/logo")), "MAIN").join();

		Path catalog = catalogDir("oauthFederationLogos", "federation1");
		assertThat(catalog.resolve("provider1~en.jpeg")).exists();
		assertThat(readBytes(catalog.resolve("provider1~en.jpeg"))).isEqualTo(jpegBytes("content"));
	}

	@Test
	public void shouldPassConfiguredMaxSizeToURIAccessService() throws IOException
	{
		LogoCacheKey key = () -> "provider1";
		when(uriAccessService.readURL(any(), any(), any(Duration.class), any(Duration.class), anyInt(), anyLong()))
				.thenReturn(new RemoteFileData("https://example.com/logo", pngBytes("c"), null, "image/png"));

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
				Map.of(key, Map.of("", "https://example.com/logo")), "MAIN").join();

		verify(uriAccessService).readURL(any(), any(), any(Duration.class), any(Duration.class), anyInt(), eq(1_000_000L));
	}

	@Test
	public void shouldRejectInternalDestinationWhenNotInTestProfile() throws IOException
	{
		when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
		RemoteLogoCacheDownloader prodTested = new RemoteLogoCacheDownloader(conf, msg, uriAccessService, executorsService,
				environment);
		LogoCacheKey key = () -> "provider1";

		prodTested.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
				Map.of(key, Map.of("", "http://127.0.0.1/logo.png")), "MAIN").join();

		verify(uriAccessService, never()).readURL(any(), any(), any(), any(), anyInt(), anyLong());
		assertThat(catalogDir("oauthFederationLogos", "federation1").resolve("provider1~en.png")).doesNotExist();
	}

	@Test
	public void shouldAllowInternalDestinationWhenInTestProfile() throws IOException
	{
		when(uriAccessService.readURL(any(), any(), any(Duration.class), any(Duration.class), anyInt(), anyLong()))
				.thenReturn(new RemoteFileData("http://127.0.0.1/logo.png", pngBytes("c"), null, "image/png"));
		LogoCacheKey key = () -> "provider1";

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
				Map.of(key, Map.of("", "http://127.0.0.1/logo.png")), "MAIN").join();

		assertThat(catalogDir("oauthFederationLogos", "federation1").resolve("provider1~en.png")).exists();
	}

	@Test
	public void shouldPassGivenTruststoreNameToURIAccessService() throws IOException
	{
		LogoCacheKey key = () -> "provider1";
		when(uriAccessService.readURL(any(), eq("customTruststore"), any(Duration.class), any(Duration.class), anyInt(), anyLong()))
				.thenReturn(new RemoteFileData("https://example.com/logo", pngBytes("c"), null,
						"image/png"));

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
				Map.of(key, Map.of("", "https://example.com/logo")), "customTruststore").join();

		verify(uriAccessService).readURL(any(), eq("customTruststore"), any(Duration.class), any(Duration.class), anyInt(), anyLong());
		assertThat(catalogDir("oauthFederationLogos", "federation1").resolve("provider1~en.png")).exists();
	}

	@Test
	public void shouldNotPropagateExceptionWhenSingleLogoFailsToDownload() throws IOException
	{
		LogoCacheKey key = () -> "provider1";
		when(uriAccessService.readURL(any(), any(), any(Duration.class), any(Duration.class), anyInt(), anyLong()))
				.thenThrow(new RuntimeException("simulated network failure"));

		assertThatCode(() -> tested
				.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
						Map.of(key, Map.of("", "https://example.com/logo")), "MAIN")
				.join())
				.doesNotThrowAnyException();

		assertThat(catalogDir("oauthFederationLogos", "federation1").resolve("provider1~en.png")).doesNotExist();
	}

	@Test
	public void shouldNotRemoveExistingCachedFileWhenRedownloadFails() throws IOException
	{
		LogoCacheKey key = () -> "provider1";
		Path catalog = catalogDir("oauthFederationLogos", "federation1");
		Files.createDirectories(catalog);
		Files.writeString(catalog.resolve("provider1~en"), "png", StandardCharsets.UTF_8);
		Files.write(catalog.resolve("provider1~en.png"), "oldContent".getBytes(StandardCharsets.UTF_8));

		when(uriAccessService.readURL(any(), any(), any(Duration.class), any(Duration.class), anyInt(), anyLong()))
				.thenThrow(new RuntimeException("simulated network failure"));

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1",
				Map.of(key, Map.of("", "https://example.com/logo")), "MAIN").join();

		assertThat(catalog.resolve("provider1~en.png")).exists();
		assertThat(readBytes(catalog.resolve("provider1~en.png"))).isEqualTo("oldContent".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void shouldRemoveLogosNoLongerReferencedInNewRun() throws IOException
	{
		Path catalog = catalogDir("oauthFederationLogos", "federation1");
		Files.createDirectories(catalog);
		Files.writeString(catalog.resolve("removedProviderEn"), "png", StandardCharsets.UTF_8);
		Files.write(catalog.resolve("removedProviderEn.png"), "stale".getBytes(StandardCharsets.UTF_8));

		LogoCacheKey survivingKey = () -> "provider1";
		String dataURI = "data:image/png;base64," + base64(pngBytes("fresh"));

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1", Map.of(survivingKey, Map.of("", dataURI)),
				"MAIN").join();

		assertThat(catalog.resolve("removedProviderEn")).doesNotExist();
		assertThat(catalog.resolve("removedProviderEn.png")).doesNotExist();
		assertThat(catalog.resolve("provider1~en.png")).exists();
	}

	@Test
	public void shouldReplaceOldExtensionWhenLogoExtensionChanges() throws IOException
	{
		Path catalog = catalogDir("oauthFederationLogos", "federation1");
		Files.createDirectories(catalog);
		Files.writeString(catalog.resolve("provider1~en"), "jpeg", StandardCharsets.UTF_8);
		Files.write(catalog.resolve("provider1~en.jpeg"), "oldPng".getBytes(StandardCharsets.UTF_8));

		LogoCacheKey key = () -> "provider1";
		String dataURI = "data:image/png;base64," + base64(pngBytes("newPng"));

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1", Map.of(key, Map.of("", dataURI)), "MAIN")
				.join();

		assertThat(catalog.resolve("provider1~en.jpeg")).doesNotExist();
		assertThat(catalog.resolve("provider1~en.png")).exists();
		assertThat(readString(catalog.resolve("provider1~en"))).isEqualTo("png");
	}

	@Test
	public void shouldCleanUpWholeNamespaceWhenCalledWithEmptyLogoMap() throws IOException
	{
		Path catalog = catalogDir("oauthFederationLogos", "federation1");
		Files.createDirectories(catalog);
		Files.writeString(catalog.resolve("providerXen"), "png", StandardCharsets.UTF_8);
		Files.write(catalog.resolve("providerXen.png"), pngBytes("stale"));

		tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1", Map.of(), "MAIN").join();

		assertThat(catalog.resolve("providerXen")).doesNotExist();
		assertThat(catalog.resolve("providerXen.png")).doesNotExist();
	}

	@Test
	public void shouldRunLatestRequestAfterActiveRunFinishesInsteadOfDiscardingIt() throws Exception
	{
		CountDownLatch releaseSlowFetch = new CountDownLatch(1);
		when(uriAccessService.readURL(any(), any(), any(Duration.class), any(Duration.class), anyInt(), anyLong()))
				.thenAnswer(invocation ->
				{
					releaseSlowFetch.await(5, TimeUnit.SECONDS);
					return new RemoteFileData("https://example.com/slow", pngBytes("fromA-slow"), null, "image/png");
				});

		LogoCacheKey slowKey = () -> "slow";
		LogoCacheKey stableKey = () -> "stable";
		Map<LogoCacheKey, Map<String, String>> refreshA = Map.of(
				slowKey, Map.of("", "https://example.com/slow"),
				stableKey, Map.of("", "data:image/png;base64," + base64(pngBytes("fromA"))));
		Map<LogoCacheKey, Map<String, String>> refreshB = Map.of(
				stableKey, Map.of("", "data:image/png;base64," + base64(pngBytes("fromB"))));

		CompletableFuture<Void> futureA = tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1", refreshA, "MAIN");
		CompletableFuture<Void> futureB = tested.downloadLogoFilesAsync("oauthFederationLogos", "federation1", refreshB, "MAIN");

		assertThat(futureB.isDone()).isFalse();

		releaseSlowFetch.countDown();
		futureA.get(5, TimeUnit.SECONDS);
		futureB.get(5, TimeUnit.SECONDS);

		Path catalog = catalogDir("oauthFederationLogos", "federation1");
		assertThat(readBytes(catalog.resolve("stable~en.png"))).isEqualTo(pngBytes("fromB"));
		assertThat(catalog.resolve("slow~en.png")).doesNotExist();
	}

	private Path catalogDir(String cacheGroup, String namespaceId)
	{
		return workspaceRoot.resolve("downloadedLogos")
				.resolve(cacheGroup)
				.resolve(LogoFilenameUtils.namespaceDirName(namespaceId));
	}

	private static String base64(String content)
	{
		return base64(content.getBytes(StandardCharsets.UTF_8));
	}

	private static String base64(byte[] content)
	{
		return Base64.getEncoder().encodeToString(content);
	}

	private static byte[] pngBytes(String suffix)
	{
		return concat(new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' }, suffix);
	}

	private static byte[] jpegBytes(String suffix)
	{
		return concat(new byte[] { (byte) 0xFF, (byte) 0xD8 }, suffix);
	}

	private static byte[] concat(byte[] prefix, String suffix)
	{
		byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);
		byte[] result = new byte[prefix.length + suffixBytes.length];
		System.arraycopy(prefix, 0, result, 0, prefix.length);
		System.arraycopy(suffixBytes, 0, result, prefix.length, suffixBytes.length);
		return result;
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
