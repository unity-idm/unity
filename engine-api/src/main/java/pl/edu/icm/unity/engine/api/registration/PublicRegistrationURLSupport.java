/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.registration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.wellknown.PublicWellKnownURLServletProvider;
import pl.edu.icm.unity.engine.api.wellknown.SecuredWellKnownURLServlet;
import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Defines constants and helper methods used to create public form access URI. 
 * Note that the public form filling code is in principle implemented in web endpoints,
 * however possibility to link to it is required in the core engine, for instance to fill 
 * invitation messages.
 * 
 * @author Krzysztof Benedyczak
 */
@Component
public class PublicRegistrationURLSupport
{
	public static final String REGISTRATION_VIEW = "registration";
	public static final String ENQUIRY_VIEW = "enquiry";
	
	public static final String CODE_PARAM = "regcode";
	public static final String FORM_PARAM = "form";
	
	private final SharedEndpointManagement sharedEndpointMan;
	
	@Autowired
	public PublicRegistrationURLSupport(SharedEndpointManagement sharedEndpointManagement)
	{
		this.sharedEndpointMan = sharedEndpointManagement;
	}

	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of a registration form
	 */
	public String getPublicRegistrationLink(RegistrationForm form)
	{
		String formName = form.getName();
		return sharedEndpointMan.getServletUrl(PublicWellKnownURLServletProvider.SERVLET_PATH) + 
				"?" + FORM_PARAM + "=" + urlEncodePath(formName) +
				"#!" + REGISTRATION_VIEW;
	}
	
	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of an enquiry form
	 */
	public String getWellknownEnquiryLink(String formName)
	{
		return sharedEndpointMan.getServerAddress() + 
				SecuredWellKnownURLServlet.DEFAULT_CONTEXT + 
				SecuredWellKnownURLServlet.SERVLET_PATH + 
				"?" + FORM_PARAM + "=" + urlEncodePath(formName) +
				"#!" + ENQUIRY_VIEW;
	}

	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of a registration form with included registration code
	 */
	public String getPublicRegistrationLink(String form, String code)
	{
		return sharedEndpointMan.getServletUrl(PublicWellKnownURLServletProvider.SERVLET_PATH) +
				"?" + CODE_PARAM + "=" + code +
				"&" + FORM_PARAM + "=" + urlEncodePath(form) +
				"#!" + REGISTRATION_VIEW;
	}
	
	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of a enquiry form with included registration code
	 */
	public String getPublicEnquiryLink(String form, String code)
	{
		return sharedEndpointMan.getServletUrl(PublicWellKnownURLServletProvider.SERVLET_PATH) +
				"?" + CODE_PARAM + "=" + code +
				"&" + FORM_PARAM + "=" + urlEncodePath(form) +
				"#!" + ENQUIRY_VIEW;
	}
	
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
			return URLEncoder.encode(pathElement, StandardCharsets.UTF_8.name()).
					replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
