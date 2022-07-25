package io.imunity.upman.av23;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import io.imunity.upman.UpManEndpointFactory;
import io.imunity.vaadin23.endpoint.common.SpringContextProvider;
import io.imunity.vaadin23.endpoint.common.SpringVaadin23ServletService;

import javax.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = UpManEndpointFactory.SERVLET_PATH + "/*", name = "UpmanVaadin23", asyncSupported = true)
public class UpManServlet extends VaadinServlet {

	@Override
	protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException
	{
		SpringVaadin23ServletService service = new SpringVaadin23ServletService(this, deploymentConfiguration, SpringContextProvider.getContext());
		service.init();
		return service;
	}
}
