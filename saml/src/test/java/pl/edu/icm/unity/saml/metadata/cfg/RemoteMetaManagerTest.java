/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.SamlEntityId;
import pl.edu.icm.unity.saml.idp.TrustedServiceProvider;
import pl.edu.icm.unity.saml.idp.TrustedServiceProviders;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteMetaManagerTest extends DBIntegrationTestBase
{
	@Autowired
	private RemoteMetadataService metadataService;
	
	@Autowired
	private PKIManagement pkiManagement;

	@Autowired
	private MessageSource msg;

	@Test
	public void shouldUpdateStaticConfigurationAfterReload()
	{
		SAMLIdPConfiguration samlIdPConfiguration = SAMLIdPConfiguration.builder()
				.withCredentialName("MAIN")
				.withIssuerURI("me")
				.withGroupChooser(Map.of("group", "group"), "group").
				build();

		RemoteMetaManager manager = new RemoteMetaManager(samlIdPConfiguration,
					pkiManagement, metadataService,
					new MetaToIDPConfigConverter(pkiManagement, msg));

		SAMLIdPConfiguration updatedSamlIdPConfiguration = SAMLIdPConfiguration.builder()
				.withCredentialName("MAIN")
				.withIssuerURI("me")
				.withGroupChooser(Map.of("group", "UPDATED"), "group").
				build();

		manager.setBaseConfiguration(updatedSamlIdPConfiguration);

		assertThat(manager.getSAMLIdPConfiguration().getGroupChooser().chooseGroup("group")).isEqualTo("UPDATED");
	}

	@Test
	public void shouldRefreshMetadataOriginatingConfigurationAfterReload() throws Exception
	{
		SAMLIdPConfiguration samlIdPConfiguration = SAMLIdPConfiguration.builder()
				.withCredentialName("MAIN")
				.withIssuerURI("me")
				.withGroupChooser(Map.of("group", "group"), "group")
				.withTrustedMetadataSources(List.of(
						BaseSamlConfiguration.RemoteMetadataSource.builder()
								.withUrl("foo")
								.withTranslationProfile(new TranslationProfile("name", "description", ProfileType.INPUT, List.of()))
								.withRefreshInterval(Duration.of(10, ChronoUnit.SECONDS))
								.build()
				))
				.build();

		MockMetadataService mockMetaService = new MockMetadataService();
		
		
		RemoteMetaManager manager = new RemoteMetaManager(samlIdPConfiguration,
					pkiManagement, mockMetaService,
					new MetaToIDPConfigConverter(pkiManagement, msg));

		EntitiesDescriptorDocument meta = EntitiesDescriptorDocument.Factory.parse(
				new File("src/test/resources/unity-as-sp-meta.xml"));
		mockMetaService.publishMetadata(meta);
		
		//when
		SAMLIdPConfiguration samlIdPConfiguration2 = SAMLIdPConfiguration.builder()
				.withCredentialName("MAIN")
				.withIssuerURI("me")
				.withGroupChooser(Map.of("group", "UPDATED"), "group")
				.withTrustedMetadataSources(List.of(
						BaseSamlConfiguration.RemoteMetadataSource.builder()
								.withUrl("foo")
								.withTranslationProfile(new TranslationProfile("name", "description", ProfileType.INPUT, List.of()))
								.withRefreshInterval(Duration.of(10, ChronoUnit.SECONDS))
								.build()
				))
				.build();
		manager.setBaseConfiguration(samlIdPConfiguration2);

		assertThat(manager.getTrustedSps().getSPConfigs().size()).isEqualTo(0);
	}
	
	@Test
	public void shouldInsertPublishedMetadataAfterInit() throws Exception
	{
		SAMLIdPConfiguration samlIdPConfiguration = SAMLIdPConfiguration.builder()
				.withCredentialName("MAIN")
				.withIssuerURI("me")
				.withGroupChooser(Map.of("group", "group"), "group")
				.withTrustedMetadataSources(List.of(
						BaseSamlConfiguration.RemoteMetadataSource.builder()
								.withUrl("foo")
								.withTranslationProfile(new TranslationProfile("name", "description", ProfileType.INPUT, List.of()))
								.withRefreshInterval(Duration.of(10, ChronoUnit.SECONDS))
								.build()
				))
				.build();
		MockMetadataService mockMetaService = new MockMetadataService();
		
		RemoteMetaManager manager = new RemoteMetaManager(samlIdPConfiguration,
					pkiManagement, mockMetaService,
					new MetaToIDPConfigConverter(pkiManagement, msg));

		//when
		EntitiesDescriptorDocument meta = EntitiesDescriptorDocument.Factory.parse(
				new File("src/test/resources/unity-as-sp-meta.xml"));
		mockMetaService.publishMetadata(meta);

		//expect
		assertThat(manager.getTrustedSps().getSPConfigs().size()).isEqualTo(1);

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
	public void testDownloadFromHttps() throws IOException
	{
		FileUtils.deleteDirectory(new File("target/workspace/downloadedMetadata"));
		SAMLIdPConfiguration samlIdPConfiguration = SAMLIdPConfiguration.builder()
				.withCredentialName("MAIN")
				.withIssuerURI("me")
				.withGroupChooser(Map.of("group", "group"), "group")
				.withTrustedMetadataSources(List.of(
						BaseSamlConfiguration.RemoteMetadataSource.builder()
								.withUrl("https://www.aai.dfn.de/fileadmin/metadata/DFN-AAI-metadata.xml")
								.withRefreshInterval(Duration.of(100, ChronoUnit.MILLIS))
								.withHttpsTruststore("EGI")
								.build()
				))
				.build();

		RemoteMetaManager manager = new RemoteMetaManager(samlIdPConfiguration,
					pkiManagement, metadataService,
					new MetaToIDPConfigConverter(pkiManagement, msg));

		Awaitility.await().atMost(Durations.TEN_SECONDS).untilAsserted(() ->
		{
			TrustedServiceProviders providers = manager.getTrustedSps();
			TrustedServiceProvider spConfig = providers.getSPConfig(new SamlEntityId("https://eu01.alma.exlibrisgroup.com/mng/login", null));
			assertThat(spConfig).isNotNull();
		});
	}
}
