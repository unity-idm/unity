/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;

import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.idpcommon.EopException;

/**
 * Implements handling of cancellation of authentication in the context of OAuth
 * processing.
 */
public class OAuthCancelHandler implements CancelHandler
{
	private final OAuthResponseHandler responseH;

	public OAuthCancelHandler(OAuthResponseHandler responseH)
	{
		this.responseH = responseH;
	}

	@Override
	public void onCancel()
	{

		OAuthAuthzContext ctx = OAuthSessionService.getVaadinContext();
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(),
				OAuth2Error.ACCESS_DENIED, ctx.getRequest().getState(), ctx.getRequest().impliedResponseMode());
		try
		{
			responseH.returnOauthResponseAndReportStatistic(oauthResponse, false, ctx, Status.FAILED);
		} catch (EopException e)
		{
			// OK - nothing to do.
			return;
		}
	}
}
