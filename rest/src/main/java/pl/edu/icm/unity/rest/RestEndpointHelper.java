/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest;

import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

import pl.edu.icm.unity.rest.exception.BadRequestExceptionMapper;
import pl.edu.icm.unity.rest.exception.EngineExceptionMapper;
import pl.edu.icm.unity.rest.exception.IllegalArgumentExceptionMapper;
import pl.edu.icm.unity.rest.exception.InternalExceptionMapper;
import pl.edu.icm.unity.rest.exception.JSONExceptionMapper;
import pl.edu.icm.unity.rest.exception.JSONParseExceptionMapper;
import pl.edu.icm.unity.rest.exception.JSONParsingExceptionMapper;
import pl.edu.icm.unity.rest.exception.NPEExceptionMapper;
import pl.edu.icm.unity.rest.exception.RuntimeEngineExceptionMapper;

/**
 * Collection of methods useful for creating rest endpoints
 * 
 * @author P.Piernik
 *
 */
public class RestEndpointHelper
{
	public static void installExceptionHandlers(Set<Object> ret)
	{
		ret.add(new EngineExceptionMapper());
		ret.add(new RuntimeEngineExceptionMapper());
		ret.add(new NPEExceptionMapper());
		ret.add(new IllegalArgumentExceptionMapper());
		ret.add(new InternalExceptionMapper());
		ret.add(new JSONParseExceptionMapper());
		ret.add(new JSONParsingExceptionMapper());
		ret.add(new JSONExceptionMapper());
		ret.add(new WebApplicationExceptionMapper());
		ret.add(new BadRequestExceptionMapper());
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
