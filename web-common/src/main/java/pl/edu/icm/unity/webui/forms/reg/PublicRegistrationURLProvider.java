/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.server.VaadinSession;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.forms.PublicFormURLProviderBase;

/**
 * Provides access to public registration forms via well-known links
 * @author K. Benedyczak
 */
@Component
public class PublicRegistrationURLProvider extends PublicFormURLProviderBase
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, PublicRegistrationURLProvider.class);
	private RegistrationsManagement regMan;
	private ObjectFactory<StandaloneRegistrationView> viewFactory;
	
	/**
	 * @implNote: due to changes in the enquiry links, below format was kept for
	 *            backwards compatibility reasons.
	 */
	@Deprecated
	private static final String REGISTRATION_FRAGMENT_PREFIX = "registration-";
	
	@Autowired
	public PublicRegistrationURLProvider(
			@Qualifier("insecure") RegistrationsManagement regMan,
			ObjectFactory<StandaloneRegistrationView> viewFactory)
	{
		this.regMan = regMan;
		this.viewFactory = viewFactory;
	}

	@Override
	public String getViewName(String viewAndParameters)
	{
		String formName = getFormName(viewAndParameters);
		if (formName == null)
			return null;
		
		RegistrationForm form = getForm(formName);
		if (form == null)
			return null;
		
		return viewAndParameters;
	}

	@Override
	public View getView(String viewName)
	{
		String formName = getFormName(viewName);
		RegistrationForm form = getForm(formName);
		if (form == null)
			return null;
		StandaloneRegistrationView view = viewFactory.getObject().init(form);
		VaadinSession vaadinSession = VaadinSession.getCurrent();
		if (vaadinSession != null)
		{
			vaadinSession.setAttribute(StandaloneRegistrationView.class, view);
		}
		return view;
	}

	protected String getFormName(String viewAndParameters)
	{
		if (PublicRegistrationURLSupport.REGISTRATION_VIEW.equals(viewAndParameters))
			return RegistrationFormDialogProvider.getFormFromURL();
		
		if (viewAndParameters.startsWith(REGISTRATION_FRAGMENT_PREFIX))
			return viewAndParameters.substring(REGISTRATION_FRAGMENT_PREFIX.length());
		
		return null;
	}
	
	private RegistrationForm getForm(String name)
	{
		try
		{
			List<RegistrationForm> forms = regMan.getForms();
			for (RegistrationForm regForm: forms)
				if (regForm.isPubliclyAvailable() && regForm.getName().equals(name))
					return regForm;
		} catch (EngineException e)
		{
			LOG.error("Can't load registration forms", e);
		}
		return null;
	}
}
