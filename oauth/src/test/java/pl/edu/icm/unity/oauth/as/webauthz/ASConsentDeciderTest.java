/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;

@RunWith(MockitoJUnitRunner.class)

public class ASConsentDeciderTest
{
	@Mock
	private EnquiryManagement enquiryManagement;
	@Mock
	private PolicyAgreementManagement policyAgreementsMan;
	@Mock
	private MessageSource msg;

	@Test
	public void shouldConsentWhenAudienceChanged()
	{
		InvocationContext ctx = new InvocationContext(null, null, Collections.emptyList());
		InvocationContext.setCurrent(ctx);
		LoginSession ls = new LoginSession("1", new Date(), new Date(System.currentTimeMillis() + 1000), 50, 1, "r1",
				null, null, null);
		ctx.setLoginSession(ls);

		Properties config = new Properties();
		config.setProperty("unity.oauth2.as.issuerUri", "http://unity.example.com");
		config.setProperty("unity.oauth2.as.skipConsent", "true");

		OAuthASProperties props = new OAuthASProperties(config, null, null);
		ASConsentDecider decider = new ASConsentDecider(enquiryManagement, policyAgreementsMan, msg);
		OAuthAuthzContext oauthCtx = new OAuthAuthzContext(null, props);

		oauthCtx.setAdditionalAudience(List.of("aud2"));

		OAuthClientSettings settings = new OAuthClientSettings();
		settings.setAudience(Set.of("aud1"));
		settings.setEffectiveRequestedScopes(Collections.emptySet());

		decider.isInteractiveUIRequired(settings, oauthCtx);
		assertThat(decider.isInteractiveUIRequired(settings, oauthCtx)).isTrue();
	}
}
