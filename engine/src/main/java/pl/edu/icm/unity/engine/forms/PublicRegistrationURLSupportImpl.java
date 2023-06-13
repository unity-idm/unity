/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static pl.edu.icm.unity.engine.api.endpoint.SecuredSharedEndpointPaths.DEFAULT_CONTEXT;
import static pl.edu.icm.unity.engine.api.endpoint.SecuredSharedEndpointPaths.SEC_ENQUIRY_PATH;
import static pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement.ENQUIRY_PATH;
import static pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement.REGISTRATION_PATH;

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
		return sharedEndpointMan.getServletUrl(REGISTRATION_PATH) + urlEncodePath(formName);
	}

	@Override
	public String getWellknownEnquiryLink(String formName)
	{
		return sharedEndpointMan.getServerAddress() + DEFAULT_CONTEXT
				+ SEC_ENQUIRY_PATH + urlEncodePath(formName);
	}
	
	@Override
	public String getWellknownEnquiryLink(String formName, String code)
	{
		return sharedEndpointMan.getServerAddress() + DEFAULT_CONTEXT
				+ SEC_ENQUIRY_PATH + urlEncodePath(formName) + "?" + CODE_PARAM + "=" + code;
	}

	@Override
	public String getPublicRegistrationLink(String form, String code)
	{
		return sharedEndpointMan.getServletUrl(REGISTRATION_PATH) + urlEncodePath(form) + "?" + CODE_PARAM + "=" + code;
	}

	@Override
	public String getPublicEnquiryLink(String form, String code)
	{
		return sharedEndpointMan.getServletUrl(ENQUIRY_PATH) + urlEncodePath(form) + "?" + CODE_PARAM + "=" + code;
	}

	@Override
	public String getPublicFormLink(String form, FormType formType, String code)
	{
		return switch (formType)
				{
					case REGISTRATION -> getPublicRegistrationLink(form, code);
					case ENQUIRY -> getPublicEnquiryLink(form, code);
				};
	}

	private String urlEncodePath(String pathElement)
	{
		return URLEncoder.encode(pathElement, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
	}
}
