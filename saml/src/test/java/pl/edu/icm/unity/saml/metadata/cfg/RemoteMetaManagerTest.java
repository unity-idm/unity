/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static pl.edu.icm.unity.saml.SamlProperties.METADATA_REFRESH;
import static pl.edu.icm.unity.saml.SamlProperties.METADATA_URL;
import static pl.edu.icm.unity.saml.SamlProperties.PUBLISH_METADATA;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.CREDENTIAL;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.DEFAULT_GROUP;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.GROUP;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.ISSUER_URI;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.P;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.SPMETA_PREFIX;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.function.BiConsumer;

import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

public class RemoteMetaManagerTest extends DBIntegrationTestBase
{
	@Autowired
	private RemoteMetadataService metadataService;
	
	@Autowired
	private PKIManagement pkiManagement;

	@Autowired
	private UnityMessageSource msg;

	@Test
	public void shouldUpdateStaticConfigurationAfterReload() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P + CREDENTIAL, "MAIN");
		p.setProperty(P + ISSUER_URI, "me");
		p.setProperty(P + GROUP, "group");
		p.setProperty(P + DEFAULT_GROUP, "group");
		SamlIdpProperties configuration = new SamlIdpProperties(p, pkiManagement);

		RemoteMetaManager manager = new RemoteMetaManager(configuration,
					pkiManagement,
					new MetaToIDPConfigConverter(pkiManagement, msg),
					metadataService, SamlIdpProperties.SPMETA_PREFIX);

		p.setProperty(P + GROUP, "UPDATED");
		manager.setBaseConfiguration(new SamlIdpProperties(p, pkiManagement));
		
		assertThat(manager.getVirtualConfiguration().getValue(GROUP), is("UPDATED"));
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
		SamlIdpProperties configuration = new SamlIdpProperties(p, pkiManagement);
		MockMetadataService mockMetaService = new MockMetadataService();
		
		
		RemoteMetaManager manager = new RemoteMetaManager(configuration,
					pkiManagement,
					new MetaToIDPConfigConverter(pkiManagement, msg),
					mockMetaService, SamlIdpProperties.SPMETA_PREFIX);

		EntitiesDescriptorDocument meta = EntitiesDescriptorDocument.Factory.parse(
				new File("src/test/resources/unity-as-sp-meta.xml"));
		mockMetaService.publishMetadata(meta);
		
		//when
		p.setProperty(P + GROUP, "UPDATED");
		manager.setBaseConfiguration(new SamlIdpProperties(p, pkiManagement));
		
		assertThat(manager.getVirtualConfiguration()
				.getStructuredListKeys(SamlIdpProperties.ALLOWED_SP_PREFIX).size(), is(0));
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
		SamlIdpProperties configuration = new SamlIdpProperties(p, pkiManagement);
		MockMetadataService mockMetaService = new MockMetadataService();
		
		RemoteMetaManager manager = new RemoteMetaManager(configuration,
					pkiManagement,
					new MetaToIDPConfigConverter(pkiManagement, msg),
					mockMetaService, SamlIdpProperties.SPMETA_PREFIX);

		//when
		EntitiesDescriptorDocument meta = EntitiesDescriptorDocument.Factory.parse(
				new File("src/test/resources/unity-as-sp-meta.xml"));
		mockMetaService.publishMetadata(meta);
		
		//expect
		assertThat(manager.getVirtualConfiguration()
				.getStructuredListKeys(SamlIdpProperties.ALLOWED_SP_PREFIX).size(), is(1));
	}
	
	private static class MockMetadataService implements RemoteMetadataService
	{
		private BiConsumer<EntitiesDescriptorDocument, String> consumer;

		@Override
		public void registerConsumer(String key, long refreshIntervalMs,
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
		SamlIdpProperties configuration = new SamlIdpProperties(p, pkiManagement);

		RemoteMetaManager manager = new RemoteMetaManager(configuration,
					pkiManagement,
					new MetaToIDPConfigConverter(pkiManagement, msg),
					metadataService, SamlIdpProperties.SPMETA_PREFIX);
		
		Awaitility.await().atMost(Duration.TEN_SECONDS).untilAsserted(() -> 
		{
			SamlIdpProperties config = (SamlIdpProperties) manager.getVirtualConfiguration();
			String ret = config.getPrefixOfSP("https://eu01.alma.exlibrisgroup.com/mng/login");
			assertThat(ret, is(notNullValue()));
		});
	}
}
