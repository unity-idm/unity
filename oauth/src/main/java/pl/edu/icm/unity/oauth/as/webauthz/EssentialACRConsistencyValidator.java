/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.Collection;
import java.util.Optional;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;

import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;

public class EssentialACRConsistencyValidator
{
	static void verifyEssentialRequestedACRisReturned(OAuthAuthzContext ctx, Collection<DynamicAttribute> attributes) throws OAuthErrorResponseException
	{
		if (ctx.getRequestedAcr().isEmpty())
			return;
		if (ctx.getRequestedAcr().getEssentialACRs() == null || ctx.getRequestedAcr().getEssentialACRs().isEmpty())
			return;
		Optional<DynamicAttribute> acrAttribute = attributes.stream().filter(a -> a.getAttribute().getName().equals(IDTokenClaimsSet.ACR_CLAIM_NAME)).findAny();
		
		if (acrAttribute.isPresent())
		{
			if (acrAttribute.get().getAttribute().getValues().containsAll(ctx.getRequestedAcr().getEssentialACRs().stream().map(acr -> acr.getValue()).toList()))
				return;
		}	
		
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
			new ErrorObject(OAuth2Error.INVALID_REQUEST_CODE, 
						"Unsupported acr value", 
						HTTPResponse.SC_BAD_REQUEST), ctx.getRequest().getState(),
				ctx.getRequest().impliedResponseMode());
		throw new OAuthErrorResponseException(oauthResponse, true);
	}
}
