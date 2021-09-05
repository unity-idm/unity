/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.wellknown.PublicWellKnownURLServletProvider;
import pl.edu.icm.unity.engine.api.wellknown.SecuredWellKnownURLServlet;
import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.RegistrationForm;

@Component
public class PublicRegistrationURLSupportImpl implements PublicRegistrationURLSupport
{
	private final SharedEndpointManagement sharedEndpointMan;

	@Autowired
	public PublicRegistrationURLSupportImpl(SharedEndpointManagement sharedEndpointManagement)
	{
		this.sharedEndpointMan = sharedEndpointManagement;
	}

	@Override
	public String getPublicRegistrationLink(RegistrationForm form)
	{
		String formName = form.getName();
		return sharedEndpointMan.getServletUrl(PublicWellKnownURLServletProvider.SERVLET_PATH) + "?" + FORM_PARAM + "="
				+ urlEncodePath(formName) + "#!" + REGISTRATION_VIEW;
	}

	@Override
	public String getWellknownEnquiryLink(String formName)
	{
		return sharedEndpointMan.getServerAddress() + SecuredWellKnownURLServlet.DEFAULT_CONTEXT
				+ SecuredWellKnownURLServlet.SERVLET_PATH + "?" + FORM_PARAM + "=" + urlEncodePath(formName) + "#!"
				+ ENQUIRY_VIEW;
	}

	@Override
	public String getPublicRegistrationLink(String form, String code)
	{
		return sharedEndpointMan.getServletUrl(PublicWellKnownURLServletProvider.SERVLET_PATH) + "?" + CODE_PARAM + "="
				+ code + "&" + FORM_PARAM + "=" + urlEncodePath(form) + "#!" + REGISTRATION_VIEW;
	}

	@Override
	public String getPublicEnquiryLink(String form, String code)
	{
		return sharedEndpointMan.getServletUrl(PublicWellKnownURLServletProvider.SERVLET_PATH) + "?" + CODE_PARAM + "="
				+ code + "&" + FORM_PARAM + "=" + urlEncodePath(form) + "#!" + ENQUIRY_VIEW;
	}

	@Override
	public String getPublicFormLink(String form, FormType formType, String code) throws IllegalFormTypeException
	{
		switch (formType)
		{
		case REGISTRATION:
			return getPublicRegistrationLink(form, code);
		case ENQUIRY:
			return getPublicEnquiryLink(form, code);
		default:
			throw new IllegalFormTypeException("Invalid form type");
		}
	}

	private String urlEncodePath(String pathElement)
	{
		try
		{
			return URLEncoder.encode(pathElement, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
