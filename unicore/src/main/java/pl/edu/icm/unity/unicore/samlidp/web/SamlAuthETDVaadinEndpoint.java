/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import javax.servlet.Filter;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Extends a simple {@link SamlAuthVaadinEndpoint}, changing the SAML parse filter to {@link SamlETDParseFilter}. 
 * 
 * @author K. Benedyczak
 */
public class SamlAuthETDVaadinEndpoint extends SamlAuthVaadinEndpoint
{
	public SamlAuthETDVaadinEndpoint(EndpointTypeDescription type, ApplicationContext applicationContext,
			FreemarkerHandler freemarkerHandler, Class<?> uiClass, String servletPath, 
			PKIManagement pkiManagement)
	{
		super(type, applicationContext, freemarkerHandler, uiClass, servletPath, pkiManagement);
	}
	
	@Override
	protected Filter getSamlParseFilter(String endpointURL)
	{
		return new SamlETDParseFilter(samlProperties, freemarkerHandler, endpointURL);
	}
}
