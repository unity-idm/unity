/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.P;
import static pl.edu.icm.unity.oauth.as.OAuthASProperties.defaults;

import java.util.Collections;
import java.util.Properties;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.engine.translation.out.action.IncludeOutputProfileActionFactory;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.SystemOAuthScopeProvidersRegistry;

public class OAuthServiceConfigurationTest
{
	private MessageSource msg = mock(MessageSource.class);

	private static final TranslationProfile DEF_PROFILE = new TranslationProfile("Embedded", "", ProfileType.OUTPUT,
			Lists.newArrayList(
					new TranslationRule("true", new IncludeOutputProfileActionFactory().getInstance("sys:default"))));

	@Test
	public void serializationIsIdempotentForMinimalConfig()
	{
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithoutDefaults(P, defaults).get();

		OAuthServiceConfiguration processor = new OAuthServiceConfiguration(msg, Collections.emptyList(),
				new OAuthScopesService(mock(SystemOAuthScopeProvidersRegistry.class)));

		processor.fromProperties(msg, ConfigurationComparator.getAsString(sourceCfg), Collections.emptyList(),
				new OAuthScopesService(mock(SystemOAuthScopeProvidersRegistry.class)));
		String converted = processor.toProperties(msg);

		Properties result = ConfigurationComparator.fromString(converted, P).get();

		createComparator(P, defaults).ignoringSuperflous("embeddedTranslationProfile", "scopes.1.enabled").checkMatching(result, sourceCfg);
		String defaultProfileJson = DEF_PROFILE.toJsonObject().toString();
		assertThat(result.get(P + "embeddedTranslationProfile")).isEqualTo(defaultProfileJson);
	}

	@Test
	public void serializationIsIdempotentForMinimalExplicitDefaultsConfig()
	{
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithDefaults(P, OAuthASProperties.defaults).get();
		OAuthServiceConfiguration processor = new OAuthServiceConfiguration(msg, Collections.emptyList(),
				new OAuthScopesService(mock(SystemOAuthScopeProvidersRegistry.class)));

		processor.fromProperties(msg, ConfigurationComparator.getAsString(sourceCfg), Collections.emptyList(),
				new OAuthScopesService(mock(SystemOAuthScopeProvidersRegistry.class)));
		String converted = processor.toProperties(msg);

		Properties result = ConfigurationComparator.fromString(converted, P).get();

		createComparator(P, defaults).ignoringSuperflous("embeddedTranslationProfile").checkMatching(result, sourceCfg);
		String defaultProfileJson = DEF_PROFILE.toJsonObject().toString();
		assertThat(result.get(P + "embeddedTranslationProfile")).isEqualTo(defaultProfileJson);
	}

	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfig()
	{
		TranslationProfile tp = new TranslationProfile("name", "description", ProfileType.OUTPUT,
				Collections.emptyList());
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(P, OAuthASProperties.defaults)
				.update("embeddedTranslationProfile", tp.toJsonObject().toString())
				.update("policyAgreements.1.policyDocuments", "1")
				.update("policyAgreements.1.policyAgreementPresentationType",
						PolicyAgreementPresentationType.CHECKBOX_NOTSELECTED.toString())
				.update("refreshTokenIssuePolicy", RefreshTokenIssuePolicy.ALWAYS.toString())

				.get();
		OAuthServiceConfiguration processor = new OAuthServiceConfiguration(msg, Collections.emptyList(),
				new OAuthScopesService(mock(SystemOAuthScopeProvidersRegistry.class)));

		processor.fromProperties(msg, ConfigurationComparator.getAsString(sourceCfg), Collections.emptyList(),
				new OAuthScopesService(mock(SystemOAuthScopeProvidersRegistry.class)));
		String converted = processor.toProperties(msg);

		Properties result = ConfigurationComparator.fromString(converted, P).get();

		createComparator(P, defaults).ignoringMissing("translationProfile").checkMatching(result, sourceCfg);
	}
}
