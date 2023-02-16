/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.shared.endpoint;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import io.imunity.vaadin.endpoint.common.SpringContextProvider;
import io.imunity.vaadin.endpoint.common.SpringVaadin2XServletService;

@CssImport("./styles/custom-lumo-theme.css")
public class SimpleVaadin2XServlet extends VaadinServlet
{
	@Override
	protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException
	{
		SpringVaadin2XServletService service = new SpringVaadin2XServletService(this, deploymentConfiguration, SpringContextProvider.getContext());
		service.init();
		return service;
	}
}
