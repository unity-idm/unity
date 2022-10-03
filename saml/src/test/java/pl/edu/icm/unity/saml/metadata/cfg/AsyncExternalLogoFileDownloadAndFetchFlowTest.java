/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.xmlbeans.XmlException;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.apache.commons.io.FileUtils.deleteDirectory;

public class AsyncExternalLogoFileDownloadAndFetchFlowTest extends DBIntegrationTestBase
{
	@Autowired
	private ExternalLogoFileLoader externalLogoFileLoader;
	@Autowired
	private AsyncExternalLogoFileDownloader fileDownloader;

	@After
	public void tearDown() throws IOException
	{
		deleteDirectory(new File("target/workspace/downloadedIdPLogos"));
	}

	@Test
	public void shouldFetchAndSaveLogoFile()
	{
		EntitiesDescriptorDocument metadata = loadMetadata("src/test/resources/metadata-of-signed-response.xml");
		TrustedIdPKey trustedIdPKey = TrustedIdPKey.metadataEntity("http://centos6-unity1:8080/simplesaml/saml2/idp/metadata.php", 1);

		fileDownloader.downloadLogoFilesAsync(metadata, null);

		Awaitility.await()
				.atMost(Durations.TEN_SECONDS)
				.untilAsserted(() -> checkIfFileSaved(metadata.getEntitiesDescriptor().getID(), trustedIdPKey));
	}

	private void checkIfFileSaved(String federationId, TrustedIdPKey trustedIdPKey)
	{
		Optional<File> file = externalLogoFileLoader.getFile(federationId, trustedIdPKey, null);
		if(file.isEmpty())
			throw new IllegalArgumentException("Empty file");
		if(!file.get().isFile())
			throw new IllegalArgumentException("This is not a file");
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
}
