/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import io.imunity.vaadin23.endpoint.common.SpringContextProvider;
import io.imunity.vaadin23.endpoint.common.SpringVaadin23ServletService;

@CssImport("./styles/custom-lumo-theme.css")
public class SimpleVaadin23Servlet extends VaadinServlet
{
	@Override
	protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException
	{
		SpringVaadin23ServletService service = new SpringVaadin23ServletService(this, deploymentConfiguration, SpringContextProvider.getContext());
		service.init();
		return service;
	}
}
