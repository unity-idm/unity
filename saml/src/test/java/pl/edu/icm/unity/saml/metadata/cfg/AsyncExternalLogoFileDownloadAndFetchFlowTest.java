/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.xmlbeans.XmlException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;

public class AsyncExternalLogoFileDownloadAndFetchFlowTest extends DBIntegrationTestBase
{
	private static final String TINY_PNG_BASE64 =
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M8AAAMBAQDJ/AP4AAAAAElFTkSuQmCC";

	@Autowired
	private ExternalLogoFileLoader externalLogoFileLoader;
	@Autowired
	private AsyncExternalLogoFileDownloader fileDownloader;

	@AfterEach
	public void tearDown() throws IOException
	{
		deleteDirectory(new File("target/workspace/downloadedIdPLogos"));
	}

	@Test
	public void shouldFetchAndSaveLogoFile()
	{
		EntitiesDescriptorDocument metadata = loadMetadata("src/test/resources/metadata-of-signed-response.xml");
		TrustedIdPKey trustedIdPKey = TrustedIdPKey.metadataEntity("http://centos6-unity1:8080/simplesaml/saml2/idp/metadata.php", 1);

		fileDownloader.downloadLogoFilesAsync(metadata, null).join();

		checkIfFileSaved(metadata.getEntitiesDescriptor().getID(), trustedIdPKey);
		checkIfStagingCleaned(metadata.getEntitiesDescriptor().getID());
	}

	@Test
	public void shouldCleanOldFile()
	{
		String oldFileName = "old";
		String oldFileExtension = "png";

		EntitiesDescriptorDocument metadata = loadMetadata("src/test/resources/metadata-of-signed-response.xml");
		TrustedIdPKey trustedIdPKey = TrustedIdPKey.metadataEntity("http://centos6-unity1:8080/simplesaml/saml2/idp/metadata.php", 1);
		createOldFiles(oldFileName, oldFileExtension, metadata);

		fileDownloader.downloadLogoFilesAsync(metadata, null).join();

		checkIfFileSaved(metadata.getEntitiesDescriptor().getID(), trustedIdPKey);
		checkIfOldFileCleaned(metadata.getEntitiesDescriptor().getID(), oldFileName, oldFileExtension);
		checkIfStagingCleaned(metadata.getEntitiesDescriptor().getID());
	}

	@Test
	public void shouldReplaceCachedLogoWhenExtensionChanges() throws IOException
	{
		EntitiesDescriptorDocument oldMetadata = loadMetadata("src/test/resources/metadata-of-signed-response.xml");
		EntitiesDescriptorDocument newMetadata = loadMetadataWithLogoMimeType("image/jpeg");
		TrustedIdPKey trustedIdPKey = TrustedIdPKey.metadataEntity("http://centos6-unity1:8080/simplesaml/saml2/idp/metadata.php", 1);
		String baseName = LogoFilenameUtils.getLogoFileBasename(trustedIdPKey, "en");

		fileDownloader.downloadLogoFilesAsync(oldMetadata, null).join();
		assertThat(getLogoFile(oldMetadata, baseName, "png")).isFile();

		fileDownloader.downloadLogoFilesAsync(newMetadata, null).join();

		assertThat(getLogoFile(newMetadata, baseName, "jpeg")).isFile();
		assertThat(getLogoFile(newMetadata, baseName, "png")).doesNotExist();
		assertThat(Files.readString(getLogoPointer(newMetadata, baseName).toPath())).isEqualTo("jpeg");
		checkIfStagingCleaned(newMetadata.getEntitiesDescriptor().getID());
	}

	@Test
	public void shouldKeepOtherLocaleLogoWhenOneLocaleExtensionChanges() throws IOException
	{
		EntitiesDescriptorDocument pngForBothLocales = loadMetadataWithEnAndDeLogos("image/png", "image/png");
		EntitiesDescriptorDocument enJpegDePng = loadMetadataWithEnAndDeLogos("image/jpeg", "image/png");
		TrustedIdPKey trustedIdPKey = TrustedIdPKey.metadataEntity("http://centos6-unity1:8080/simplesaml/saml2/idp/metadata.php", 1);
		String enBaseName = LogoFilenameUtils.getLogoFileBasename(trustedIdPKey, "en");
		String deBaseName = LogoFilenameUtils.getLogoFileBasename(trustedIdPKey, "de");

		fileDownloader.downloadLogoFilesAsync(pngForBothLocales, null).join();
		assertThat(getLogoFile(pngForBothLocales, enBaseName, "png")).isFile();
		assertThat(getLogoFile(pngForBothLocales, deBaseName, "png")).isFile();

		fileDownloader.downloadLogoFilesAsync(enJpegDePng, null).join();

		assertThat(getLogoFile(enJpegDePng, enBaseName, "jpeg")).isFile();
		assertThat(getLogoFile(enJpegDePng, enBaseName, "png")).doesNotExist();
		assertThat(getLogoFile(enJpegDePng, deBaseName, "png")).isFile();
		assertThat(Files.readString(getLogoPointer(enJpegDePng, enBaseName).toPath())).isEqualTo("jpeg");
		assertThat(Files.readString(getLogoPointer(enJpegDePng, deBaseName).toPath())).isEqualTo("png");
		checkIfStagingCleaned(enJpegDePng.getEntitiesDescriptor().getID());
	}

	private static void createOldFiles(String oldFileName, String oldFileExtension, EntitiesDescriptorDocument metadata)
	{
		try
		{
			File oldFile = new File("target/workspace/downloadedIdPLogos/" + LogoFilenameUtils.federationDirName(metadata.getEntitiesDescriptor().getID()) + "/" + oldFileName + "." + oldFileExtension);
			oldFile.getParentFile().mkdirs();
			oldFile.createNewFile();
			File oldFilePointer = new File("target/workspace/downloadedIdPLogos/" + LogoFilenameUtils.federationDirName(metadata.getEntitiesDescriptor().getID()) + "/" + oldFileName);
			oldFilePointer.getParentFile().mkdirs();
			oldFilePointer.createNewFile();
		} catch (IOException e)
		{
			throw new RuntimeException("Test files not created");
		}
	}


	private void checkIfFileSaved(String federationId, TrustedIdPKey trustedIdPKey)
	{
		Optional<File> file = externalLogoFileLoader.getFile(federationId, trustedIdPKey, null);
		if(file.isEmpty())
			throw new IllegalArgumentException("Empty file");
		if(!file.get().isFile())
			throw new IllegalArgumentException("This is not a file");
	}

	private void checkIfOldFileCleaned(String federationId, String oldFileName, String extension)
	{
		if(new File("target/workspace/downloadedIdPLogos/" + LogoFilenameUtils.federationDirName(federationId) + "/" + oldFileName + "." + extension).isFile())
			throw new IllegalStateException("File not clean");
		if(new File("target/workspace/downloadedIdPLogos/" + LogoFilenameUtils.federationDirName(federationId) + "/" + oldFileName).isFile())
			throw new IllegalStateException("File not clean");
	}

	private void checkIfStagingCleaned(String federationId)
	{
		File staging = new File("target/workspace/downloadedIdPLogos/staging/" + LogoFilenameUtils.federationDirName(federationId));
		if(Objects.requireNonNull(staging.listFiles()).length != 0)
			throw new IllegalStateException("Staging catalog not clean");
	}

	private File getLogoFile(EntitiesDescriptorDocument metadata, String baseName, String extension)
	{
		return new File("target/workspace/downloadedIdPLogos/"
				+ LogoFilenameUtils.federationDirName(metadata.getEntitiesDescriptor().getID()) + "/" + baseName + "." + extension);
	}

	private File getLogoPointer(EntitiesDescriptorDocument metadata, String baseName)
	{
		return new File("target/workspace/downloadedIdPLogos/"
				+ LogoFilenameUtils.federationDirName(metadata.getEntitiesDescriptor().getID()) + "/" + baseName);
	}

	private EntitiesDescriptorDocument loadMetadata(String path)
	{
		try
		{
			return EntitiesDescriptorDocument.Factory.parse(new File(path));
		} catch (XmlException | IOException e)
		{
			throw new RuntimeException("Can't load test XML", e);
		}
	}

	private EntitiesDescriptorDocument loadMetadataWithLogoMimeType(String mimeType)
	{
		try
		{
			String metadata = Files.readString(new File("src/test/resources/metadata-of-signed-response.xml").toPath());
			return EntitiesDescriptorDocument.Factory.parse(metadata.replace("data:image/png", "data:" + mimeType));
		} catch (XmlException | IOException e)
		{
			throw new RuntimeException("Can't load test XML", e);
		}
	}

	private EntitiesDescriptorDocument loadMetadataWithEnAndDeLogos(String enMimeType, String deMimeType)
	{
		try
		{
			String metadata = Files.readString(new File("src/test/resources/metadata-of-signed-response.xml").toPath());
			metadata = metadata.replace("data:image/png", "data:" + enMimeType);
			String deLogo = "<mdui:Logo height=\"16\" width=\"16\" xml:lang=\"de\">data:"
					+ deMimeType + ";base64," + TINY_PNG_BASE64 + "</mdui:Logo>";
			metadata = metadata.replace("</mdui:UIInfo>", deLogo + "</mdui:UIInfo>");
			return EntitiesDescriptorDocument.Factory.parse(metadata);
		} catch (XmlException | IOException e)
		{
			throw new RuntimeException("Can't load test XML", e);
		}
	}
}
