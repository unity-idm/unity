/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.web;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import io.imunity.vaadin.auth.SecuredSpringVaadin2XServletService;
import io.imunity.vaadin.endpoint.common.SpringContextProvider;

import static pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint.SAML_ENTRY_SERVLET_PATH;

public class SamlVaadin2XServlet extends VaadinServlet
{
	@Override
	protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException
	{
		SecuredSpringVaadin2XServletService service = new SecuredSpringVaadin2XServletService(
				this,
				deploymentConfiguration,
				SpringContextProvider.getContext(),
				getServletContext().getContextPath() + SAML_ENTRY_SERVLET_PATH
		);		service.init();
		return service;
	}
}
