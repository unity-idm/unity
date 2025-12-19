/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz.externalScript;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;

@JsonInclude(JsonInclude.Include.NON_NULL)
record InputIdentity(
		String typeId,
		String value,
		String target,
		String realm,
		String translationProfile,
		String remoteIdp,
		ConfirmationInfo confirmationInfo)
{

	static InputIdentity fromIdentity(IdentityParam id, IdentityTypesRegistry registry)
	{
		Objects.requireNonNull(id, "IdentityParam must not be null");
		Objects.requireNonNull(registry, "IdentityTypesRegistry must not be null");

		ConfirmationInfo confirmation = null;
		if (registry.getByName(id.getTypeId())
				.isEmailVerifiable())
		{
			confirmation = id.getConfirmationInfo();
		}

		return new InputIdentity(id.getTypeId(), id.getValue(), id.getTarget(), id.getRealm(),
				id.getTranslationProfile(), id.getRemoteIdp(), confirmation);
	}
}