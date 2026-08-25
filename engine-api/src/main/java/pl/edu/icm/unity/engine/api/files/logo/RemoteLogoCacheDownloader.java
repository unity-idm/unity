/*
 * Copyright (c) 2018-2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.files.logo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.RemoteFileData;
import pl.edu.icm.unity.engine.api.files.SsrfProtection;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

import static java.util.Base64.getDecoder;

/**
 * Downloads and caches on local disk logo files referenced by a remote source (a SAML federation's
 * IdPs, an OpenID federation's OPs, ...), so that they can later be served locally without a browser
 * or per-request server side fetch to the (untrusted, remote-controlled) original URI. Downloads for
 * a given cache group + namespace (e.g. a single federation) are coalesced: while a run is in progress,
 * further requests don't start a parallel run - the latest one supersedes any not-yet-started request
 * queued behind it, and runs as soon as the current run finishes. See {@link CachedLogoFileLoader} for
 * the read side.
 */
@Component
public class RemoteLogoCacheDownloader implements DisposableBean
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, RemoteLogoCacheDownloader.class);
	private static final String STAGING = "staging";

	private final ExecutorService executorService;
	private final URIAccessService uriAccessService;
	private final String workspaceRoot;
	private final String defaultLocale;
	private final Duration socketReadTimeout;
	private final Duration connectionTimeout;
	private final long maxFileSizeBytes;
	private final ExecutorService downloadExecutorService;
	private final boolean restrictInternalDestinations;

	private final Map<String, RefreshCoordinator> coordinatorsByKey = new ConcurrentHashMap<>();

	public RemoteLogoCacheDownloader(UnityServerConfiguration conf, MessageSource msg,
			URIAccessService uriAccessService, ExecutorsService executorsService, Environment environment)
	{
		workspaceRoot = LogoFilenameUtils.getLogosWorkspaceRoot(conf);
		executorService = executorsService.getExecutionService();
		defaultLocale = msg.getLocale().toString();
		this.uriAccessService = uriAccessService;
		this.socketReadTimeout = Duration.ofMillis(conf.getIntValue(UnityServerConfiguration.BULK_FILES_DOWNLOAD_TIMEOUT));
		this.connectionTimeout = Duration.ofMillis(conf.getIntValue(UnityServerConfiguration.BULK_FILES_CONNECTION_TIMEOUT));
		this.maxFileSizeBytes = conf.getIntValue(UnityServerConfiguration.BULK_FILES_MAX_SIZE);
		this.downloadExecutorService = Executors.newFixedThreadPool(
				conf.getIntValue(UnityServerConfiguration.BULK_FILES_MAX_CONCURRENT_DOWNLOADS),
				RemoteLogoCacheDownloader::newDaemonThread);
		this.restrictInternalDestinations = !environment.acceptsProfiles(Profiles.of(UnityServerConfiguration.PROFILE_TEST));
	}

	@Override
	public void destroy()
	{
		downloadExecutorService.shutdownNow();
	}

	private static Thread newDaemonThread(Runnable runnable)
	{
		Thread thread = new Thread(runnable, "logo-downloader");
		thread.setDaemon(true);
		return thread;
	}

	/**
	 * @param cacheGroup logical owner of the cache (e.g. "samlIdpLogos", "oauthFederationLogos") - keeps
	 * different modules' caches from colliding on disk.
	 * @param namespaceId id of the remote source the logos come from (e.g. a federation id) - used to scope
	 * cleanup of no-longer-referenced logos to a single refresh run.
	 * @param logosByKeyAndLocale for every cached entity, its logo URI(s) keyed by locale code (empty string
	 * for the default/unlocalized one).
	 */
	public CompletableFuture<Void> downloadLogoFilesAsync(String cacheGroup, String namespaceId,
			Map<? extends LogoCacheKey, Map<String, String>> logosByKeyAndLocale, String httpsTruststore)
	{
		String dedupKey = cacheGroup + " [" + namespaceId + "]";
		RefreshCoordinator coordinator = coordinatorsByKey.computeIfAbsent(dedupKey,
				k -> new RefreshCoordinator(dedupKey, catalog(cacheGroup, namespaceId)));
		return coordinator.submit(logosByKeyAndLocale, httpsTruststore);
	}

	private String catalog(String cacheGroup, String namespaceId)
	{
		return Path.of(cacheGroup, LogoFilenameUtils.namespaceDirName(namespaceId)).toString();
	}

	/**
	 * Coalesces concurrent refresh requests for a single cache group + namespace: while a run is in
	 * progress, at most one further request is kept pending - a newer one replaces it - and is run,
	 * with the latest data, as soon as the current run completes. Every caller superseded this way still
	 * gets a future that completes with the run that ends up actually reflecting its (or newer) data.
	 */
	private final class RefreshCoordinator
	{
		private final String dedupKey;
		private final String catalog;
		private boolean running;
		private PendingRefresh pending;

		RefreshCoordinator(String dedupKey, String catalog)
		{
			this.dedupKey = dedupKey;
			this.catalog = catalog;
		}

		synchronized CompletableFuture<Void> submit(Map<? extends LogoCacheKey, Map<String, String>> logosByKeyAndLocale,
				String httpsTruststore)
		{
			if (!running)
			{
				running = true;
				return runAndChain(logosByKeyAndLocale, httpsTruststore, null);
			}
			log.info("Logos of {} are being downloaded, queuing the newest request to run next", dedupKey);
			if (pending == null)
				pending = new PendingRefresh();
			pending.logosByKeyAndLocale = logosByKeyAndLocale;
			pending.httpsTruststore = httpsTruststore;
			CompletableFuture<Void> waiter = new CompletableFuture<>();
			pending.waiters.add(waiter);
			return waiter;
		}

		private CompletableFuture<Void> runAndChain(Map<? extends LogoCacheKey, Map<String, String>> logosByKeyAndLocale,
				String httpsTruststore, List<CompletableFuture<Void>> waitersToComplete)
		{
			CompletableFuture<Void> run;
			try
			{
				run = doDownload(dedupKey, catalog, logosByKeyAndLocale, httpsTruststore);
			}
			catch (RuntimeException e)
			{
				log.error("Failed to start logo refresh for {}", dedupKey, e);
				if (waitersToComplete != null)
					completeAll(waitersToComplete, null, e);
				onRunFinished();
				return CompletableFuture.failedFuture(e);
			}
			run.whenComplete((result, error) ->
			{
				if (waitersToComplete != null)
					completeAll(waitersToComplete, result, error);
				onRunFinished();
			});
			return run;
		}

		private synchronized void onRunFinished()
		{
			PendingRefresh next = pending;
			pending = null;
			if (next == null)
			{
				running = false;
				return;
			}
			runAndChain(next.logosByKeyAndLocale, next.httpsTruststore, next.waiters);
		}

		private void completeAll(List<CompletableFuture<Void>> waiters, Void result, Throwable error)
		{
			for (CompletableFuture<Void> waiter : waiters)
			{
				if (error != null)
					waiter.completeExceptionally(error);
				else
					waiter.complete(result);
			}
		}
	}

	private static final class PendingRefresh
	{
		private Map<? extends LogoCacheKey, Map<String, String>> logosByKeyAndLocale;
		private String httpsTruststore;
		private final List<CompletableFuture<Void>> waiters = new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	private CompletableFuture<Void> doDownload(String dedupKey, String catalog,
			Map<? extends LogoCacheKey, Map<String, String>> logosByKeyAndLocale, String httpsTruststore)
	{
		CompletableFuture<Set<String>>[] savedFilesNamesFutures = logosByKeyAndLocale.entrySet().stream()
				.map(entry -> CompletableFuture.supplyAsync(
						() -> downloadFiles(catalog, entry.getKey(), entry.getValue(), httpsTruststore),
						downloadExecutorService))
				.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(savedFilesNamesFutures)
			.thenRunAsync(() -> cleanUp(catalog, savedFilesNamesFutures), executorService)
			.whenComplete((result, error) -> log.info("Prefetched logos of {}", dedupKey));
	}

	private void cleanUp(String catalog, CompletableFuture<Set<String>>[] savedFilesNamesFutures)
	{
		Set<String> downloadedFilesName = Arrays.stream(savedFilesNamesFutures)
				.filter(future -> !future.isCompletedExceptionally())
				.flatMap(this::getFileNamesAfterJobCompletion)
				.collect(Collectors.toSet());
		try
		{
			deleteDirectoryRecursively(Paths.get(workspaceRoot, STAGING, catalog));
		}
		catch (IOException e)
		{
			log.warn("Failed to clean up staging directory of {}", catalog, e);
		}
		try
		{
			Path finalDir = Paths.get(workspaceRoot, catalog);
			if (!finalDir.toFile().exists())
				return;
			removeFilesFromFinalDestinationWhichAreNotReplacedByNewOne(downloadedFilesName, finalDir);
			log.debug("Not used logos from {} has been cleaned from {}", catalog, finalDir);
		}
		catch (IOException e)
		{
			log.error("Failed while cleaning images from final destination", e);
		}
	}

	private static void deleteDirectoryRecursively(Path dir) throws IOException
	{
		if (!Files.exists(dir))
			return;
		try (Stream<Path> paths = Files.walk(dir))
		{
			paths.sorted(Comparator.reverseOrder()).forEach(RemoteLogoCacheDownloader::deleteCachedLogoFileIfExists);
		}
	}

	private Stream<String> getFileNamesAfterJobCompletion(CompletableFuture<Set<String>> completableFuture)
	{
		try
		{
			return completableFuture.get().stream();
		} catch (InterruptedException | ExecutionException e)
		{
			throw new IllegalStateException("This shouldn't happen, only completed future should be processed ", e);
		}
	}

	private static void removeFilesFromFinalDestinationWhichAreNotReplacedByNewOne(Set<String> savedFilesBasedNames,
	                                                                               Path finalDir) throws IOException
	{
		try (Stream<Path> paths = Files.walk(finalDir))
		{
			paths.filter(Files::isRegularFile)
					.filter(path -> savedFilesBasedNames.stream().noneMatch(name -> isFileOfBasename(path, name)))
					.forEach(RemoteLogoCacheDownloader::deleteCachedLogoFileIfExists);
		}
	}

	private static boolean isFileOfBasename(Path path, String basename)
	{
		String fileName = path.getFileName().toString();
		return fileName.equals(basename) || fileName.startsWith(basename + ".");
	}

	private Set<String> downloadFiles(String catalog, LogoCacheKey key, Map<String, String> logosByLocale, String httpsTruststore)
	{
		return logosByLocale.entrySet().stream()
				.map(entry ->
					{
						String logoFileBasename = LogoFilenameUtils.getLogoFileBasename(
								key, Locale.forLanguageTag(entry.getKey()), defaultLocale);
						fetchAndSaveFileOnDisk(catalog, logoFileBasename, entry.getValue(), httpsTruststore);
						return logoFileBasename;
					}
				).collect(Collectors.toSet());
	}

	private void fetchAndSaveFileOnDisk(String catalog, String name, String logoURI, String httpsTruststore)
	{
		try
		{
			URI uri = URI.create(logoURI);
			if(uri.getScheme().equals("data"))
				saveFileBasedOnDataURI(catalog, name, uri);
			else
				downloadFile(catalog, name, uri, httpsTruststore);

			log.trace("Logo file with uri {} was downloaded to {}", logoURI, name);
		} catch (SsrfProtection.BlockedDestinationException e)
		{
			log.warn("Blocked fetching logo {} ({}) - SSRF protection: {}", name, logoURI, e.getMessage());
		} catch (Exception e)
		{
			String cause = e.getCause() != null ? e.getCause().getMessage() : "-";
			if (e.getCause() == null || !knownException(e.getCause()))
				log.debug("Details of fetching logo {} error", logoURI, e);
			else if (log.isTraceEnabled())
				log.trace("Details of fetching logo {} error", logoURI, e);
			else
				log.debug("Logo file with uri {} cannot be downloaded: {}, cause: {}", logoURI, e.getMessage(), cause);
		}
	}

	private boolean knownException(Throwable exception)
	{
		return exception instanceof IOException;
	}

	private void downloadFile(String catalog, String name, URI uri, String httpsTruststore) throws IOException
	{
		log.trace("Downloading from {}", uri);
		if (restrictInternalDestinations)
			SsrfProtection.assertNoInternalDestination(uri);
		RemoteFileData fileData = uriAccessService.readURL(uri, httpsTruststore, connectionTimeout, socketReadTimeout,
				0, maxFileSizeBytes);
		String extension = LogoFilenameUtils.getExtensionForRemoteFile(fileData);
		saveImageFileAndItsPointer(catalog, name, fileData.getContents(), extension);
	}

	private void saveFileBasedOnDataURI(String catalog, String name, URI logoURI) throws IOException
	{
		String logoURIStr = logoURI.toString();
		int dataStartIndex = logoURIStr.indexOf(",") + 1;
		String data = logoURIStr.substring(dataStartIndex);
		byte[] decoded = getDecoder().decode(data);
		if (decoded.length > maxFileSizeBytes)
			throw new IOException("Data URI logo " + name + " is " + decoded.length
					+ " bytes, which exceeds the allowed maximum of " + maxFileSizeBytes + " bytes");
		String extension = LogoFilenameUtils.getExtensionFromDataURI(logoURI);
		saveImageFileAndItsPointer(catalog, name, decoded, extension);
	}

	private void saveImageFileAndItsPointer(String catalog, String name, byte[] decoded, String extension) throws IOException
	{
		if (!SupportedImageContentValidator.isSupportedImage(decoded, extension))
			throw new IOException("Logo " + name + " content does not match the expected " + extension + " image format");
		File imageFile = createFile(catalog, name + "." + extension);
		Files.write(imageFile.toPath(), decoded);
		File pointerFile = createFile(catalog, name);
		Files.write(pointerFile.toPath(), extension.getBytes(StandardCharsets.UTF_8));
		Path finalImagePath = Path.of(workspaceRoot, catalog, name + "." + extension);
		Path finalPointerPath = Path.of(workspaceRoot, catalog, name);
		try
		{
			Files.createDirectories(finalImagePath.getParent());
			Files.move(imageFile.toPath(), finalImagePath, StandardCopyOption.REPLACE_EXISTING);
			Files.move(pointerFile.toPath(), finalPointerPath, StandardCopyOption.REPLACE_EXISTING);
			removeObsoleteLogoFiles(catalog, name, extension);
		}
		finally
		{
			imageFile.delete();
			pointerFile.delete();
		}
	}

	private void removeObsoleteLogoFiles(String catalog, String name, String currentExtension) throws IOException
	{
		Path finalDir = Path.of(workspaceRoot, catalog);
		if(!Files.exists(finalDir))
			return;
		String currentFileName = name + "." + currentExtension;
		try (Stream<Path> paths = Files.list(finalDir))
		{
			paths.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().startsWith(name + "."))
					.filter(path -> !path.getFileName().toString().equals(currentFileName))
					.forEach(RemoteLogoCacheDownloader::deleteCachedLogoFileIfExists);
		}
	}

	private static void deleteCachedLogoFileIfExists(Path path)
	{
		try
		{
			Files.deleteIfExists(path);
		} catch (IOException e)
		{
			log.warn("Failed to delete cached logo file {}", path, e);
		}
	}

	private File createFile(String catalog, String name) throws IOException
	{
		new File(Path.of(workspaceRoot, STAGING, catalog).toUri()).mkdirs();
		File file = new File(Path.of(workspaceRoot, STAGING, catalog, name).toUri());
		file.createNewFile();
		return file;
	}
}
