/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.types.I18nString;

public class FakeTrustedIdPConfiguration
{
	public static TrustedIdPConfiguration.Builder getFakeBuilder()
	{
		return TrustedIdPConfiguration.builder()
				.withCertificateNames(Set.of())
				.withRequestedNameFormat("reqNameFormat")
				.withName(new I18nString("idp"))
				.withPublicKeys(List.of())
				.withSamlId("samlId")
				.withKey(new TrustedIdPKey("key"))
				.withTags(Set.of())
				.withBinding(Binding.HTTP_REDIRECT)
				.withLogoutEndpoints(Set.of());
	}
	
	public static TrustedIdPConfiguration get()
	{
		return getFakeBuilder().build();
	}
}
