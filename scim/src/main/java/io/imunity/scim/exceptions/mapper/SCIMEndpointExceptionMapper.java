/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.exceptions.mapper;

import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;



public class SCIMEndpointExceptionMapper
{
	public static void installExceptionHandlers(Set<Object> ret)
	{
		ret.add(new EngineExceptionMapper());
		ret.add(new UnknownIdentityExceptionMapper());
		ret.add(new UnknownGroupExceptionMapper());
		ret.add(new SCIMExceptionMapper());
	}

	public static Endpoint createCxfEndpoint(Application application, Bus bus)
	{
		JAXRSServerFactoryBean sf = ResourceUtils.createApplication(application, false, false, false, bus);

		JAXRSBindingFactory factory = new JAXRSBindingFactory();
		factory.setBus(bus);

		BindingFactoryManager manager = bus.getExtension(BindingFactoryManager.class);
		manager.registerBindingFactory(JAXRSBindingFactory.JAXRS_BINDING_ID, factory);

		Server server = sf.create();

		return server.getEndpoint();
	}
}
