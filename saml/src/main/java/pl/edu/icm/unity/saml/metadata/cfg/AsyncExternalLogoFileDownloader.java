/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.RemoteFileData;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Base64.getDecoder;

@Component
public class AsyncExternalLogoFileDownloader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, AsyncExternalLogoFileDownloader.class);
	private static final String STAGING = "staging";
	private final ExecutorService executorService;
	private final URIAccessService uriAccessService;

	private final MetadataToSPConfigConverter converter;
	private final String workspaceDir;
	private final String defaultLocale;
	private final Duration socketReadTimeout;
	private final Duration connectionTimeout;

	private final FederationSet currentlyDownloadingFederation = new FederationSet();

	public AsyncExternalLogoFileDownloader(UnityServerConfiguration conf, MessageSource msg, URIAccessService uriAccessService,
	                                       ExecutorsService executorsService, MetadataToSPConfigConverter converter)
	{
		workspaceDir = LogoFilenameUtils.getLogosWorkspace(conf);
		executorService = executorsService.getService();
		defaultLocale = msg.getLocale().toString();
		this.uriAccessService = uriAccessService;
		this.converter = converter;
		this.socketReadTimeout = Duration.ofMillis(conf.getIntValue(UnityServerConfiguration.BULK_FILES_DOWNLOAD_TIMEOUT));
		this.connectionTimeout = Duration.ofMillis(conf.getIntValue(UnityServerConfiguration.BULK_FILES_CONNECTION_TIMEOUT));
	}

	public void downloadLogoFilesAsync(EntitiesDescriptorDocument entitiesDescriptorDocument, String httpsTruststore)
	{
		String federationId = entitiesDescriptorDocument.getEntitiesDescriptor().getID();
		if(currentlyDownloadingFederation.blockIfNotProcess(federationId))
			return;
		RemoteMetadataSource metadataSource = RemoteMetadataSource.builder()
				.withTranslationProfile(new TranslationProfile("mock", "description", ProfileType.INPUT, List.of()))
				.withUrl("url")
				.withRefreshInterval(Duration.ZERO)
				.build();
		TrustedIdPs trustedIdPs = converter.convertToTrustedIdPs(entitiesDescriptorDocument, metadataSource);
		log.debug("Will download logos for {} IdPs of federation {}", trustedIdPs.getKeys().size(),
				entitiesDescriptorDocument.getEntitiesDescriptor().getName());
		CompletableFuture<Set<String>>[] savedFilesNamesFutures = trustedIdPs.getEntrySet().stream()
				.map(entry -> CompletableFuture.supplyAsync(() -> downloadFiles(entry, httpsTruststore), executorService))
				.toArray(CompletableFuture[]::new);
		CompletableFuture.allOf(savedFilesNamesFutures).thenRunAsync(
				() -> cleanFinalDir(entitiesDescriptorDocument, savedFilesNamesFutures),
				executorService
		).whenComplete((ignore, ignore1) -> currentlyDownloadingFederation.remove(federationId));
	}

	private void cleanFinalDir(EntitiesDescriptorDocument entitiesDescriptorDocument, CompletableFuture<Set<String>>[] savedFilesNamesFutures)
	{
		Set<String> downloadedFilesName = Arrays.stream(savedFilesNamesFutures)
				.filter(future -> !future.isCompletedExceptionally())
				.flatMap(this::getFuture)
				.collect(Collectors.toSet());
		cleanFinalDir(entitiesDescriptorDocument.getEntitiesDescriptor().getID(), downloadedFilesName);
	}

	private Stream<String> getFuture(CompletableFuture<Set<String>> completableFuture)
	{
		try
		{
			return completableFuture.get().stream();
		} catch (InterruptedException | ExecutionException e)
		{
			throw new IllegalStateException("This shouldn't happen, only completed future should be process " ,e);
		}
	}

	private void cleanFinalDir(String federationId, Set<String> downloadedFilesName)
	{
		String catalog = LogoFilenameUtils.federationDirName(federationId);
		try
		{
			Path finalDir = Paths.get(workspaceDir, catalog);
			Paths.get(workspaceDir, STAGING, catalog).toFile().deleteOnExit();
			removeFilesFromFinalDestinationWhichAreNotReplaceByNewOne(downloadedFilesName, finalDir);
			log.info("Not used logos from federation id {} has been clean from {}", federationId, finalDir);
		}
		catch (IOException e)
		{
			log.error("Failed while cleaning images from final destination", e);
		}
	}

	private static void removeFilesFromFinalDestinationWhichAreNotReplaceByNewOne(Set<String> savedFilesBasedNames,
	                                                                              Path finalDir) throws IOException
	{
		try (Stream<Path> paths = Files.walk(finalDir))
		{
			paths.filter(Files::isRegularFile)
					.filter(path -> savedFilesBasedNames.stream().noneMatch(name -> path.getFileName().toString().startsWith(name)))
					.forEach(path -> path.toFile().delete());
		}
	}

	private Set<String> downloadFiles(Map.Entry<TrustedIdPKey, TrustedIdPConfiguration> entry, String httpsTruststore)
	{
		return entry.getValue().logoURI
				.getMap()
				.entrySet().stream()
				.map(entry1 ->
					{
						String federationDirName = LogoFilenameUtils.federationDirName(entry.getValue().federationId);
						String logoFileBasename = LogoFilenameUtils.getLogoFileBasename(entry.getKey(), new Locale(entry1.getKey()), defaultLocale);
						fetchAndSaveFileOnDisk(federationDirName, logoFileBasename, entry1.getValue(), httpsTruststore);
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
		RemoteFileData fileData = uriAccessService.readURL(uri, httpsTruststore, connectionTimeout, socketReadTimeout, 0);
		String extension = LogoFilenameUtils.getExtensionForRemoteFile(fileData);
		saveImageFileAndItsPointer(catalog, name, fileData.getContents(), extension);
	}

	private void saveFileBasedOnDataURI(String catalog, String name, URI logoURI) throws IOException
	{
		String logoURIStr = logoURI.toString();
		int dataStartIndex = logoURIStr.indexOf(",") + 1;
		String data = logoURIStr.substring(dataStartIndex);
		byte[] decoded = getDecoder().decode(data);
		String extension = LogoFilenameUtils.getExtensionFromDataURI(logoURI);
		saveImageFileAndItsPointer(catalog, name, decoded, extension);
	}

	private void saveImageFileAndItsPointer(String catalog, String name, byte[] decoded, String extension) throws IOException
	{
		File imageFile = createFile(catalog, name + "." + extension);
		Files.write(imageFile.toPath(), decoded);
		File pointerFile = createFile(catalog, name);
		Files.write(pointerFile.toPath(), extension.getBytes(StandardCharsets.UTF_8));
		try
		{
			FileUtils.moveFile(imageFile, new File(Path.of(workspaceDir, catalog, name + "." + extension).toUri()));
			FileUtils.moveFile(pointerFile, new File(Path.of(workspaceDir, catalog, name).toUri()));
		}
		catch (FileExistsException e)
		{
			imageFile.delete();
			pointerFile.delete();
		}
	}

	private File createFile(String catalog, String name) throws IOException
	{
		new File(Path.of(workspaceDir, STAGING, catalog).toUri()).mkdirs();
		File file = new File(Path.of(workspaceDir, STAGING, catalog, name).toUri());
		file.createNewFile();
		return file;
	}

	static class FederationSet
	{
		private final Set<String> set = new HashSet<>();

		synchronized void remove(String federationId)
		{
			set.remove(federationId);
		}

		synchronized boolean blockIfNotProcess(String federationId)
		{
			boolean contains = set.contains(federationId);
			set.add(federationId);
			return contains;
		}
	}
}
