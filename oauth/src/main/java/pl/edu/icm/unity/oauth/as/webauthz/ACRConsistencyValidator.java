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

public class ACRConsistencyValidator
{
	static void verifyACRAttribute(OAuthAuthzContext ctx, Collection<DynamicAttribute> attributes) throws OAuthErrorResponseException
	{
		if (ctx.getAcr().isEmpty())
			return;
		if (ctx.getAcr().getEssentialACRs() == null || ctx.getAcr().getEssentialACRs().isEmpty())
			return;
		Optional<DynamicAttribute> any = attributes.stream().filter(a -> a.getAttribute().getName().equals(IDTokenClaimsSet.ACR_CLAIM_NAME)).findAny();
		
		if (any.isPresent())
		{
			if (any.get().getAttribute().getValues().containsAll(ctx.getAcr().getEssentialACRs().stream().map(acr -> acr.getValue()).toList()))
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
