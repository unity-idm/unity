/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import eu.unicore.samly2.exceptions.SAMLServerException;

import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.SamlProperties;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.web.EopException;
import pl.edu.icm.unity.saml.idp.web.filter.SamlParseFilter;
import pl.edu.icm.unity.unicore.samlidp.saml.WebAuthWithETDRequestValidator;

/**
 * Extension of the {@link SamlParseFilter}. It changes the default SAML SSO 
 * validator to the {@link WebAuthWithETDRequestValidator}.
 * 
 * @author K. Benedyczak
 */
public class SamlETDParseFilter extends SamlParseFilter
{
	public SamlETDParseFilter(SamlProperties samlConfig, FreemarkerHandler freemarker, String endpointAddress,
			String samlConsumerPath, String samlServletPath)
	{
		super(samlConfig, freemarker, endpointAddress, samlConsumerPath, samlServletPath);
	}

	protected void validate(SAMLAuthnContext context, HttpServletResponse servletResponse) 
			throws SAMLProcessingException, IOException, EopException
	{
		WebAuthWithETDRequestValidator validator = new WebAuthWithETDRequestValidator(endpointAddress, 
				samlConfig.getAuthnTrustChecker(), samlConfig.getRequestValidity(), 
				samlConfig.getReplayChecker());
		
		try
		{
			validator.validate(context.getRequestDocument());
		} catch (SAMLServerException e)
		{
			errorHandler.commitErrorResponse(context, e, servletResponse);
		}
	}
}
