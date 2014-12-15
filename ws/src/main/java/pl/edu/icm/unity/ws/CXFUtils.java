/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws;

import javax.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.cxf.xmlbeans.XmlBeansDataBinding;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Simplifies Web service creation in programmatic way.
 * @author K. Benedyczak
 */
public class CXFUtils
{
	public static Endpoint deployWebservice(Bus bus, Class<?> iface, Object impl)
	{
		JaxWsServerFactoryBean factory=new JaxWsServerFactoryBean();
		factory.getServiceFactory().setDataBinding(new XmlBeansDataBinding());
		factory.setServiceBean(impl);
		factory.setServiceClass(impl.getClass());
		factory.setBus(bus);
		String name = iface.getAnnotation(WebService.class).name();
		factory.setAddress("/"+name);
		Server server = factory.create();
		return server.getEndpoint();
	}

	public static ServletContextHandler getServletContextHandler(String contextAddress, String servletPath,
			Bus bus)
	{
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(contextAddress);
		CXFNonSpringServlet cxfServlet = new CXFNonSpringServlet();
		cxfServlet.setBus(bus);
		ServletHolder holder = new ServletHolder(cxfServlet);
		context.addServlet(holder, servletPath + "/*");
		return context;
	}
}
