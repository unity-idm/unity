/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import static java.util.Base64.getDecoder;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private final Duration connectionAndSocketReadTimeout;

	public AsyncExternalLogoFileDownloader(UnityServerConfiguration conf, MessageSource msg, URIAccessService uriAccessService,
	                                       ExecutorsService executorsService, MetadataToSPConfigConverter converter)
	{
		workspaceDir = LogoFilenameUtils.getLogosWorkspace(conf);
		executorService = executorsService.getService();
		defaultLocale = msg.getLocale().toString();
		this.uriAccessService = uriAccessService;
		this.converter = converter;
		this.connectionAndSocketReadTimeout = Duration.ofMillis(conf.getIntValue(UnityServerConfiguration.BULK_FILES_DOWNLOAD_TIMEOUT));
	}

	public void downloadLogoFilesAsync(EntitiesDescriptorDocument entitiesDescriptorDocument, String httpsTruststore)
	{
		RemoteMetadataSource metadataSource = RemoteMetadataSource.builder()
				.withTranslationProfile(new TranslationProfile("mock", "description", ProfileType.INPUT, List.of()))
				.withUrl("url")
				.withRefreshInterval(Duration.ZERO)
				.build();
		TrustedIdPs trustedIdPs = converter.convertToTrustedIdPs(entitiesDescriptorDocument, metadataSource);
		log.debug("Will download logos for {} IdPs of federation {}", trustedIdPs.getKeys().size(), 
				entitiesDescriptorDocument.getEntitiesDescriptor().getName());
		CompletableFuture<?>[] completableFutures = trustedIdPs.getEntrySet().stream()
				.map(entry -> CompletableFuture.runAsync(() -> downloadFiles(entry, httpsTruststore), executorService))
				.toArray(CompletableFuture[]::new);
		CompletableFuture.allOf(completableFutures)
				.thenRunAsync(
						() -> copyToAndCleanFinalDir(entitiesDescriptorDocument.getEntitiesDescriptor().getID()),
						executorService
				);
	}

	private void copyToAndCleanFinalDir(String federationId)
	{
		String catalog = LogoFilenameUtils.federationDirName(federationId);
		Path stagingDir = Paths.get(workspaceDir, STAGING, catalog);
		if (!Files.exists(stagingDir))
		{
			log.info("No logos for federation id {}", federationId);
			return;
		}

		try
		{
			Set<Path> stagingDestinationFileNames = getFileNamesFromDir(stagingDir);

			Path finalDir = Paths.get(workspaceDir, catalog);
			finalDir.toFile().mkdir();
			Set<Path> finalDestinationFileNames = getFileNamesFromDir(finalDir);

			removeFileFromFinalDestinationWhichDoNotHaveEquivalentInStageDestination(finalDestinationFileNames, 
					stagingDestinationFileNames, finalDir);
			copyDirectory(stagingDir.toFile(), finalDir.toFile());
			deleteDirectory(stagingDir.toFile());
			log.info("Logos from federation id {} has been refreshed and put into {}", federationId, finalDir);
		}
		catch (IOException e)
		{
			log.error("Failed while coping/cleaning images from stage to final destination", e);
		}
	}

	private static void removeFileFromFinalDestinationWhichDoNotHaveEquivalentInStageDestination(Set<Path> finalDestinationFileNames,
				Set<Path> stagingDestinationFileNames,
				Path finalDir) throws IOException
	{
		finalDestinationFileNames.removeAll(stagingDestinationFileNames);
		try (Stream<Path> paths = Files.walk(finalDir))
		{
			paths.filter(path -> finalDestinationFileNames.contains(path.getFileName()))
					.forEach(path -> path.toFile().delete());
		}
	}

	private static Set<Path> getFileNamesFromDir(Path stagingDir) throws IOException
	{
		Set<Path> stagingDestinationFileNames;
		try (Stream<Path> paths = Files.walk(stagingDir))
		{
			stagingDestinationFileNames = paths.filter(Files::isRegularFile)
					.map(Path::getFileName)
					.collect(Collectors.toSet());
		}
		return stagingDestinationFileNames;
	}

	private void downloadFiles(Map.Entry<TrustedIdPKey, TrustedIdPConfiguration> entry, String httpsTruststore)
	{
		entry.getValue().logoURI
				.getMap()
				.forEach((locale, uri) ->
					{
						String federationDirName = LogoFilenameUtils.federationDirName(entry.getValue().federationId);
						String logoFileBasename = LogoFilenameUtils.getLogoFileBasename(entry.getKey(), new Locale(locale), defaultLocale);
						fetchAndSaveFileOnDisk(federationDirName, logoFileBasename, uri, httpsTruststore);
					}
				);
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
		RemoteFileData fileData = uriAccessService.readURL(uri, httpsTruststore, connectionAndSocketReadTimeout, 0);
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
	}

	private File createFile(String catalog, String name) throws IOException
	{
		new File(Path.of(workspaceDir, STAGING, catalog).toUri()).mkdirs();
		File file = new File(Path.of(workspaceDir, STAGING, catalog, name).toUri());
		file.createNewFile();
		return file;
	}
}
