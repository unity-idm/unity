/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.registration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.wellknown.PublicWellKnownURLServletProvider;
import pl.edu.icm.unity.engine.api.wellknown.SecuredWellKnownURLServlet;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Defines constants and helper methods used to create public form access URI. 
 * Note that the public form filling code is in principle implemented in web endpoints,
 * however possibility to link to it is required in the core engine, for instance to fill 
 * invitation messages.
 * 
 * @author Krzysztof Benedyczak
 */
public class PublicRegistrationURLSupport
{
	public static final String REGISTRATION_VIEW = "registration";
	public static final String ENQUIRY_VIEW = "enquiry";
	
	public static final String CODE_PARAM = "regcode";
	public static final String FORM_PARAM = "form";
	
	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of a registration form
	 */
	public static String getPublicRegistrationLink(RegistrationForm form, SharedEndpointManagement sharedEndpointMan)
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
	public static String getWellknownEnquiryLink(String formName, SharedEndpointManagement sharedEndpointMan)
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
	public static String getPublicRegistrationLink(BaseForm form, String code, 
			SharedEndpointManagement sharedEndpointMan)
	{
		return sharedEndpointMan.getServletUrl(PublicWellKnownURLServletProvider.SERVLET_PATH) +
				"?" + CODE_PARAM + "=" + code +
				"&" + FORM_PARAM + "=" + urlEncodePath(form.getName()) +
				"#!" + REGISTRATION_VIEW;
	}
	
	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of a enquiry form with included registration code
	 */
	public static String getPublicEnquiryLink(BaseForm form, String code, 
			SharedEndpointManagement sharedEndpointMan)
	{
		return sharedEndpointMan.getServletUrl(PublicWellKnownURLServletProvider.SERVLET_PATH) +
				"?" + CODE_PARAM + "=" + code +
				"&" + FORM_PARAM + "=" + urlEncodePath(form.getName()) +
				"#!" + ENQUIRY_VIEW;
	}
	
	private static String urlEncodePath(String pathElement)
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
