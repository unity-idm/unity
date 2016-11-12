/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.webui.authn.CancelHandler;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;

/**
 * Implements handling of cancellation of authentication in the context of OAuth processing.
 *  
 * @author K. Benedyczak
 */
public class OAuthCancelHandler implements CancelHandler
{
	private OAuthResponseHandler responseH;
	
	public OAuthCancelHandler(OAuthResponseHandler responseH)
	{
		this.responseH = responseH;
	}

	@Override
	public void onCancel()
	{
		OAuthAuthzContext ctx = OAuthContextUtils.getContext();
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
				OAuth2Error.ACCESS_DENIED, ctx.getRequest().getState(),
				ctx.getRequest().impliedResponseMode());
		try
		{
			responseH.returnOauthResponse(oauthResponse, false);
		} catch (EopException e)
		{
			//OK - nothing to do.
			return;
		}
	}
}
