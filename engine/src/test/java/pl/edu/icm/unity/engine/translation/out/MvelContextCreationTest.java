/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.translation.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.AuthNInfo;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata.Protocol;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationMVELContextKey;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;

public class MvelContextCreationTest
{

	@Test
	public void shouldAddRemoteAuthContextToMvelContext() throws IllegalAttributeValueException
	{
		InvocationContext invocationContext = new InvocationContext(null, null, null);
		LoginSession loginSession = new LoginSession();
		loginSession
				.setFirstFactorRemoteIdPAuthnContext(new RemoteAuthnMetadata(Protocol.OIDC, "idp", List.of("acr1")));
		loginSession.setLogin1stFactor(new AuthNInfo(AuthenticationOptionKey.authenticatorOnlyKey("1"), null));
		loginSession.setLogin2ndFactor(new AuthNInfo(AuthenticationOptionKey.authenticatorOnlyKey("2"), null));

		InvocationContext.setCurrent(invocationContext);
		invocationContext.setLoginSession(loginSession);

		TranslationInput input = new TranslationInput(List.of(), new Entity(List.of(), null, null), "/", List.of(),
				null, List.of(), null, null, Map.of());
		Map<String, Object> mvelContext = OutputTranslationProfile.createMvelContext(input, null, null);

		assertThat(mvelContext.get(OutputTranslationMVELContextKey.upstreamProtocol.name()))
				.isEqualTo(Protocol.OIDC.name());
		assertThat(mvelContext.get(OutputTranslationMVELContextKey.upstreamACRs.name())).isEqualTo(List.of("acr1"));
		assertThat(mvelContext.get(OutputTranslationMVELContextKey.upstreamIdP.name())).isEqualTo("idp");
	}
}
