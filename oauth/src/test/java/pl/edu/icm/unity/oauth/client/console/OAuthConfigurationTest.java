/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.console;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;
import static pl.edu.icm.unity.oauth.client.config.OAuthClientProperties.META;
import static pl.edu.icm.unity.oauth.client.config.OAuthClientProperties.P;

import java.util.Properties;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.translation.in.action.IncludeInputProfileActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class OAuthConfigurationTest
{
	private PKIManagement pkiMan = mock(PKIManagement.class);
	private UnityMessageSource msg = mock(UnityMessageSource.class);
	private URIAccessService uriAccessSrv = mock(URIAccessService.class);
	private FileStorageService fileStorageSrv = mock(FileStorageService.class);
	private static final TranslationProfile DEF_PROFILE = new TranslationProfile("Embedded", "", ProfileType.INPUT, 
			Lists.newArrayList(new TranslationRule("true", 
					new IncludeInputProfileActionFactory().getInstance("fooo"))));

	@Test
	public void serializationIsIdempotentForMinimalConfig() throws EngineException
	{
		when(pkiMan.getValidatorNames()).thenReturn(Sets.newHashSet("foo"));
		Properties sourceProviderCfg = ConfigurationGenerator.generateMinimalDefault(
				"unity.oauth2.client.providers.1.", CustomProviderProperties.META)
				.update("embeddedTranslationProfile", DEF_PROFILE.toJsonObject().toString())
				.update("authEndpoint", "foo")
				.update("accessTokenEndpoint", "foo")
				.get();
		Properties sourceCfg = ConfigurationGenerator.generateMinimalDefault(P, META).get();
		sourceCfg.putAll(sourceProviderCfg);
		
		OAuthConfiguration processor = new OAuthConfiguration();
		
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, pkiMan, uriAccessSrv);
		String converted = processor.toProperties(msg, pkiMan, fileStorageSrv, "authName");
		
		Properties result = ConfigurationComparator.fromString(converted, P).get();
		
		createComparator(P, META)
			.addStructuredEntryMeta("providers.1.", CustomProviderProperties.META)
			.checkMatching(result, sourceCfg);
	}
	
	@Test
	public void serializationIsIdempotentForMinimalExplicitDefaultsConfig() throws EngineException
	{
		when(pkiMan.getValidatorNames()).thenReturn(Sets.newHashSet("foo"));
		Properties sourceProviderCfg = ConfigurationGenerator.generateMinimalWithDefaults(
				"unity.oauth2.client.providers.1.", CustomProviderProperties.META)
				.update("embeddedTranslationProfile", DEF_PROFILE.toJsonObject().toString())
				.update("authEndpoint", "foo")
				.update("accessTokenEndpoint", "foo")
				.get();
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithDefaults(P, META).get();
		sourceCfg.putAll(sourceProviderCfg);
		
		OAuthConfiguration processor = new OAuthConfiguration();
		
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, pkiMan, uriAccessSrv);
		String converted = processor.toProperties(msg, pkiMan, fileStorageSrv, "authName");
		
		Properties result = ConfigurationComparator.fromString(converted, P).get();
		
		createComparator(P, META)
			.checkMatching(result, sourceCfg);
	}
	
	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfig() throws EngineException
	{
		when(pkiMan.getValidatorNames()).thenReturn(Sets.newHashSet("foo"));
		Properties sourceProviderCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(
				"unity.oauth2.client.providers.1.", CustomProviderProperties.META)
				.update("embeddedTranslationProfile", DEF_PROFILE.toJsonObject().toString())
				.get();
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(P, META).get();
		sourceCfg.putAll(sourceProviderCfg);
		
		OAuthConfiguration processor = new OAuthConfiguration();
		
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, pkiMan, uriAccessSrv);
		String converted = processor.toProperties(msg, pkiMan, fileStorageSrv, "authName");
		
		Properties result = ConfigurationComparator.fromString(converted, P).get();
		
		createComparator(P, META)
			.ignoringMissing("providers.1.translationProfile")
			.checkMatching(result, sourceCfg);
	}
}
