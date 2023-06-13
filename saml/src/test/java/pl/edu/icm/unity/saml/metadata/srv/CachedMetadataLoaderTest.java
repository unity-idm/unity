/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.srv;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

@RunWith(MockitoJUnitRunner.class)
public class CachedMetadataLoaderTest
{
	@Mock
	private URIAccessService uriAccessService;
	@Mock
	private FileStorageService fileStorageService;
	@Mock
	private MetadataDownloader downloader;

	@Test
	public void shouldCacheMeta() throws EngineException, IOException, XmlException, InterruptedException
	{
		CachedMetadataLoader loader = new CachedMetadataLoader(uriAccessService, fileStorageService, downloader);
		String xmlText = IOUtils.toString(new FileInputStream("src/test/resources/unity-as-sp-meta.xml"),
				StandardCharsets.UTF_8);
		when(downloader.download(any(), eq("truststore")))
				.thenReturn(new FileData("name", xmlText.getBytes(), new Date()));
		when(fileStorageService.storeFileInWorkspace(any(), any()))
				.thenReturn(new FileData("name", xmlText.getBytes(), new Date()));
		EntitiesDescriptorDocument fresh = loader.getFresh("http://metadata.aai.switch.ch/metadata.switchaai.xml",
				"truststore");
		Optional<EntitiesDescriptorDocument> cached = loader
				.getCached("http://metadata.aai.switch.ch/metadata.switchaai.xml");
		assertThat(fresh, is(cached.get()));
	}

}
