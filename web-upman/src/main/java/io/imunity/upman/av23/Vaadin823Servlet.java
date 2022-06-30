package io.imunity.upman.av23;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import io.imunity.upman.av23.spring.SpringContextProvider;
import io.imunity.upman.av23.spring.SpringVaadin23ServletService;

import javax.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = "/upman/*", name = "UpmanVaadin23", asyncSupported = true)
public class Vaadin823Servlet extends VaadinServlet {

	@Override
	protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException
	{
		SpringVaadin23ServletService service = new SpringVaadin23ServletService(this, deploymentConfiguration, SpringContextProvider.getContext());
		service.init();
		return service;
	}
}
