/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.idp.statistic.IdpStatisticReporter;

public class OAuthIdpStatisticReporter extends IdpStatisticReporter
{

	public OAuthIdpStatisticReporter(ApplicationEventPublisher applicationEventPublisher, MessageSource msg,
			Endpoint endpoint)
	{
		super(applicationEventPublisher, msg, endpoint);
	}

	public void reportStatus(OAuthAuthzContext ctx, Status status)
	{
		super.reportStatus(ctx.getClientUsername(), ctx.getClientName(), status);

	}

	@Component
	public static class OAuthIdpStatisticReporterFactory
	{
		private final ApplicationEventPublisher applicationEventPublisher;
		private final MessageSource msg;

		public OAuthIdpStatisticReporterFactory(ApplicationEventPublisher applicationEventPublisher, MessageSource msg)
		{

			this.applicationEventPublisher = applicationEventPublisher;
			this.msg = msg;

		}

		public OAuthIdpStatisticReporter getForEndpoint(Endpoint e)
		{
			return new OAuthIdpStatisticReporter(applicationEventPublisher, msg, e);
		}
	}

}
