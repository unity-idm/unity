/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.web.filter.ErrorHandler;
import pl.edu.icm.unity.saml.idp.web.filter.SamlParseServlet;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.unicore.samlidp.saml.WebAuthWithETDRequestValidator;
import pl.edu.icm.unity.webui.idpcommon.EopException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Extension of the {@link SamlParseServlet}. It changes the default SAML SSO 
 * validator to the {@link WebAuthWithETDRequestValidator}.
 * 
 * @author K. Benedyczak
 */
public class SamlETDParseServlet extends SamlParseServlet
{
	public SamlETDParseServlet(RemoteMetaManager samlConfigProvider, String endpointAddress,
			String samlUiServletPath, ErrorHandler errorHandler)
	{
		super(samlConfigProvider, endpointAddress, samlUiServletPath, errorHandler);
	}

	@Override
	protected void validate(SAMLAuthnContext context, HttpServletResponse servletResponse, 
			SAMLIdPConfiguration samlConfig)
			throws SAMLProcessingException, IOException, EopException
	{
		WebAuthWithETDRequestValidator validator = new WebAuthWithETDRequestValidator(endpointAddress, 
				samlConfig.getAuthnTrustChecker(), samlConfig.getRequestValidity(), 
				samlConfig.getReplayChecker());
		samlConfig.configureKnownRequesters(validator);

		try
		{
			validator.validate(context.getRequestDocument(), context.getVerifiableElement());
		} catch (SAMLServerException e)
		{
			//security measure: if the request is invalid (usually not trusted) don't send the response,
			//as it may happen that the response URL is evil.
			if (e.getCause() != null && e.getCause() instanceof SAMLValidationException)
				throw new SAMLProcessingException(e);
			errorHandler.commitErrorResponse(context, e, servletResponse);
		}
	}
}
