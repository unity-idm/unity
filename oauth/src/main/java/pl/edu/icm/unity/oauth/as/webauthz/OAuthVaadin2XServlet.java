/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz;

import static pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint.OAUTH_CONSENT_DECIDER_SERVLET_PATH;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

import io.imunity.vaadin.auth.SecuredSpringVaadin2XServletService;
import io.imunity.vaadin.auth.SigInInProgressContextService;
import io.imunity.vaadin.endpoint.common.SpringContextProvider;

public class OAuthVaadin2XServlet extends VaadinServlet
{
	@Override
	protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException
	{
		SecuredSpringVaadin2XServletService service = new SecuredSpringVaadin2XServletService(
				this,
				deploymentConfiguration,
				SpringContextProvider.getContext(),
				getServletContext().getContextPath() + OAUTH_CONSENT_DECIDER_SERVLET_PATH,
				(session, existingKeye, newKey) ->
				{
					OAuthSessionService.putExistingContextUnderNewKey(session, existingKeye, newKey);
					SigInInProgressContextService.putExistingContextUnderNewKey(session, existingKeye, newKey);
				}		);
		service.init();
		return service;
	}
}
