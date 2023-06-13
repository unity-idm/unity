/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.idp.statistic.IdpStatisticReporter;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;

public class SamlIdpStatisticReporter extends IdpStatisticReporter
{
	public SamlIdpStatisticReporter(ApplicationEventPublisher applicationEventPublisher, MessageSource msg,
			Endpoint endpoint)
	{
		super(applicationEventPublisher, msg, endpoint);
	}

	public void reportStatus(SAMLAuthnContext samlCtx, Status status)
	{
		super.reportStatus(samlCtx.getRequest().getIssuer().getStringValue(),
				samlCtx.getSamlConfiguration().getDisplayedNameForRequester(samlCtx.getRequest().getIssuer(), msg), status);
	}

	@Component
	public static class SamlIdpStatisticReporterFactory
	{
		private final ApplicationEventPublisher applicationEventPublisher;
		private final MessageSource msg;

		public SamlIdpStatisticReporterFactory(ApplicationEventPublisher applicationEventPublisher, MessageSource msg)
		{
			this.applicationEventPublisher = applicationEventPublisher;
			this.msg = msg;
		}

		public SamlIdpStatisticReporter getForEndpoint(Endpoint e)
		{
			return new SamlIdpStatisticReporter(applicationEventPublisher, msg, e);
		}
	}

}
