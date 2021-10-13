/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import org.springframework.context.ApplicationEventPublisher;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.idp.statistic.IdpStatisticEvent;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.idpcommon.EopException;

/**
 * Implements handling of cancellation of authentication in the context of OAuth
 * processing.
 */
public class OAuthCancelHandler implements CancelHandler
{
	private final OAuthResponseHandler responseH;
	private final ResolvedEndpoint endpoint;
	private final ApplicationEventPublisher eventPublisher;
	private final MessageSource msg;

	public OAuthCancelHandler(OAuthResponseHandler responseH, ApplicationEventPublisher eventPublisher,
			ResolvedEndpoint endpoint, MessageSource msg)
	{
		this.responseH = responseH;
		this.endpoint = endpoint;
		this.eventPublisher = eventPublisher;
		this.msg = msg;
	}

	@Override
	public void onCancel()
	{

		OAuthAuthzContext ctx = OAuthSessionService.getVaadinContext();

		eventPublisher.publishEvent(new IdpStatisticEvent(endpoint.getName(),
				endpoint.getEndpoint().getConfiguration().getDisplayedName() != null
						? endpoint.getEndpoint().getConfiguration().getDisplayedName().getValue(msg)
						: null,
				ctx.getClientUsername(), ctx.getClientName(), Status.FAILED));

		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(),
				OAuth2Error.ACCESS_DENIED, ctx.getRequest().getState(), ctx.getRequest().impliedResponseMode());
		try
		{
			responseH.returnOauthResponse(oauthResponse, false);
		} catch (EopException e)
		{
			// OK - nothing to do.
			return;
		}
	}
}
