/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.secured_shared_endpoint;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import io.imunity.vaadin.auth.SecuredSpringVaadin2XServletService;
import io.imunity.vaadin.endpoint.common.SpringContextProvider;

@CssImport("./styles/custom-lumo-theme.css")
public class SharedVaadin2XServlet extends VaadinServlet
{
	@Override
	protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException
	{
		SecuredSpringVaadin2XServletService service = new SecuredSpringVaadin2XServletService(this, deploymentConfiguration, SpringContextProvider.getContext());
		service.init();
		return service;
	}
}
