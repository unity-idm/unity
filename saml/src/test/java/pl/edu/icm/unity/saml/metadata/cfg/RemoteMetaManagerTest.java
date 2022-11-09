/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.function.BiConsumer;

import static pl.edu.icm.unity.saml.SamlProperties.*;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.P;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.*;

public class RemoteMetaManagerTest extends DBIntegrationTestBase
{
	@Autowired
	private RemoteMetadataService metadataService;
	
	@Autowired
	private PKIManagement pkiManagement;

	@Autowired
	private MessageSource msg;

	@Test
	public void shouldUpdateStaticConfigurationAfterReload() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P + CREDENTIAL, "MAIN");
		p.setProperty(P + ISSUER_URI, "me");
		p.setProperty(P + GROUP, "group");
		p.setProperty(P + DEFAULT_GROUP, "group");
		SamlIdpProperties configuration = new SamlIdpProperties(p);

//		RemoteMetaManager manager = new RemoteMetaManager(configuration,
//					pkiManagement,
//					new MetaToIDPConfigConverter(pkiManagement, msg),
//					metadataService, SamlIdpProperties.SPMETA_PREFIX);
//
//		p.setProperty(P + GROUP, "UPDATED");
//		manager.setBaseConfiguration(new SamlIdpProperties(p, pkiManagement));
//
//		assertThat(manager.getTrustedSps().getValue(GROUP), is("UPDATED"));
	}

	@Test
	public void shouldRefreshMetadataOriginatingConfigurationAfterReload() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P + CREDENTIAL, "MAIN");
		p.setProperty(P + ISSUER_URI, "me");
		p.setProperty(P + GROUP, "group");
		p.setProperty(P + DEFAULT_GROUP, "group");
		p.setProperty(P + SPMETA_PREFIX + "1." + METADATA_URL,
				new String("foo"));
		SamlIdpProperties configuration = new SamlIdpProperties(p);
		MockMetadataService mockMetaService = new MockMetadataService();
		
		
//		RemoteMetaManager manager = new RemoteMetaManager(configuration,
//					pkiManagement,
//					new MetaToIDPConfigConverter(pkiManagement, msg),
//					mockMetaService, SamlIdpProperties.SPMETA_PREFIX);
//
//		EntitiesDescriptorDocument meta = EntitiesDescriptorDocument.Factory.parse(
//				new File("src/test/resources/unity-as-sp-meta.xml"));
//		mockMetaService.publishMetadata(meta);
		
		//when
//		p.setProperty(P + GROUP, "UPDATED");
//		manager.setBaseConfiguration(new SamlIdpProperties(p, pkiManagement));
//
//		assertThat(manager.getTrustedSps()
//				.getStructuredListKeys(SamlIdpProperties.ALLOWED_SP_PREFIX).size(), is(0));
	}
	
	@Test
	public void shouldInsertPublishedMetadataAfterInit() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P + CREDENTIAL, "MAIN");
		p.setProperty(P + ISSUER_URI, "me");
		p.setProperty(P + GROUP, "group");
		p.setProperty(P + DEFAULT_GROUP, "group");
		p.setProperty(P + SPMETA_PREFIX + "1." + METADATA_URL,
				new String("foo"));
//		SamlIdpProperties configuration = new SamlIdpProperties(p, pkiManagement);
//		MockMetadataService mockMetaService = new MockMetadataService();
		
//		RemoteMetaManager manager = new RemoteMetaManager(configuration,
//					pkiManagement,
//					new MetaToIDPConfigConverter(pkiManagement, msg),
//					mockMetaService, SamlIdpProperties.SPMETA_PREFIX);
//
//		//when
//		EntitiesDescriptorDocument meta = EntitiesDescriptorDocument.Factory.parse(
//				new File("src/test/resources/unity-as-sp-meta.xml"));
//		mockMetaService.publishMetadata(meta);
//
//		//expect
//		assertThat(manager.getTrustedSps()
//				.getStructuredListKeys(SamlIdpProperties.ALLOWED_SP_PREFIX).size(), is(1));
	}
	
	private static class MockMetadataService implements RemoteMetadataService
	{
		private BiConsumer<EntitiesDescriptorDocument, String> consumer;

		@Override
		public void registerConsumer(String key, Duration refreshInterval,
				String customTruststore,
				BiConsumer<EntitiesDescriptorDocument, String> consumer)
		{
			this.consumer = consumer;
		}

		void publishMetadata(EntitiesDescriptorDocument doc)
		{
			consumer.accept(doc, "1");
		}
		
		@Override
		public void unregisterConsumer(String id)
		{
		}

		@Override
		public void reset()
		{
		}

		@Override
		public String preregisterConsumer(String url)
		{
			return "1";
		}
	}
	
	@Ignore
	@Test
	public void testDownloadFromHttps() throws IOException, EngineException, InterruptedException
	{
		FileUtils.deleteDirectory(new File("target/workspace/downloadedMetadata"));
		Properties p = new Properties();
		p.setProperty(P + CREDENTIAL, "MAIN");
		p.setProperty(P + PUBLISH_METADATA, "false");
		p.setProperty(P + ISSUER_URI, "me");
		p.setProperty(P + GROUP, "group");
		p.setProperty(P + DEFAULT_GROUP, "group");
		p.setProperty(P + METADATA_REFRESH, "100");
		//p.setProperty(P + SPMETA_PREFIX + "1." + SamlProperties.METADATA_HTTPS_TRUSTSTORE, "EGI");
		p.setProperty(P + SPMETA_PREFIX + "1." + METADATA_URL,
				new String("https://www.aai.dfn.de/fileadmin/metadata/DFN-AAI-metadata.xml"));
		SamlIdpProperties configuration = new SamlIdpProperties(p);

//		RemoteMetaManager manager = new RemoteMetaManager(configuration,
//					pkiManagement,
//					new MetaToIDPConfigConverter(pkiManagement, msg),
//					metadataService, SamlIdpProperties.SPMETA_PREFIX);
//
//		Awaitility.await().atMost(Durations.TEN_SECONDS).untilAsserted(() ->
//		{
//			SamlIdpProperties config = (SamlIdpProperties) manager.getTrustedSps();
//			String ret = config.getPrefixOfSP("https://eu01.alma.exlibrisgroup.com/mng/login");
//			assertThat(ret, is(notNullValue()));
//		});
	}
}
