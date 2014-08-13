/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;

import pl.edu.icm.unity.webui.authn.CancelHandler;

/**
 * Implements handling of cancellation of authentication in the context of OAuth processing.
 *  
 * @author K. Benedyczak
 */
public class OAuthCancelHandler implements CancelHandler
{
	@Override
	public void onCancel()
	{
		OAuthResponseHandler responseH = new OAuthResponseHandler();

		OAuthAuthzContext ctx = OAuthResponseHandler.getContext();
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
				OAuth2Error.ACCESS_DENIED, ctx.getRequest().getState());
		responseH.returnOauthResponse(oauthResponse, false);
	}
}
