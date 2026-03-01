/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.List;

import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.op.ACRRequest;

import pl.edu.icm.unity.engine.api.authn.RequestedAuthenticationContextClassReference;

public class RequestedACRMapper
{
	public static RequestedAuthenticationContextClassReference mapToInternalACRFromNimbusdsACRType(ACRRequest acrRequest)
	{
		return new RequestedAuthenticationContextClassReference(
				getMappedACRs(acrRequest != null ? acrRequest.getEssentialACRs() : null),
				getMappedACRs(acrRequest != null ? acrRequest.getVoluntaryACRs() : null));

	}

	private static List<String> getMappedACRs(List<ACR> acrs)
	{
		return acrs != null ? acrs.stream()
				.map(a -> a.getValue())
				.toList() : List.of();
	}
}
