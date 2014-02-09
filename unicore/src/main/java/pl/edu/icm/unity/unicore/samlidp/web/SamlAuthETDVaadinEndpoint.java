/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import javax.servlet.Servlet;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint;
import pl.edu.icm.unity.saml.idp.web.filter.ErrorHandler;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;


/**
 * Extends a simple {@link SamlAuthVaadinEndpoint}, changing the SAML parse servlet to 
 * {@link SamlETDParseServlet}. 
 * 
 * @author K. Benedyczak
 */
public class SamlAuthETDVaadinEndpoint extends SamlAuthVaadinEndpoint
{
	public SamlAuthETDVaadinEndpoint(EndpointTypeDescription type, ApplicationContext applicationContext,
			FreemarkerHandler freemarkerHandler, Class<?> uiClass, String servletPath, 
			PKIManagement pkiManagement, ExecutorsService executorsService, 
			String samlConsumerPath, String samlMetadataPath)
	{
		super(type, applicationContext, freemarkerHandler, uiClass, servletPath, pkiManagement, 
				executorsService, samlConsumerPath, samlMetadataPath);
	}

	@Override
	protected Servlet getSamlParseServlet(String endpointURL, String uiUrl)
	{
		return new SamlETDParseServlet(samlProperties, 
				endpointURL, uiUrl, new ErrorHandler(freemarkerHandler));
	}
}
