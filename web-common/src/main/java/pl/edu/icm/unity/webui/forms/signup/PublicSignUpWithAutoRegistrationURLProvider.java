/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.signup;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.navigator.View;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormDialogProvider;
import pl.edu.icm.unity.webui.wellknownurl.PublicViewProvider;

/**
 * Provides access to public registration forms via well-known links
 * @author K. Benedyczak
 */
@Deprecated
public class PublicSignUpWithAutoRegistrationURLProvider implements PublicViewProvider
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB,
			PublicSignUpWithAutoRegistrationURLProvider.class);
	private RegistrationsManagement regMan;
	private ObjectFactory<StandaloneSignupWithAutoRegistrationView> viewFactory;
	
	@Autowired
	public PublicSignUpWithAutoRegistrationURLProvider(
			@Qualifier("insecure") RegistrationsManagement regMan,
			ObjectFactory<StandaloneSignupWithAutoRegistrationView> viewFactory)
	{
		this.regMan = regMan;
		this.viewFactory = viewFactory;
	}

	@Override
	public String getViewName(String viewAndParameters)
	{
		throw new IllegalStateException("not implemented!");
	}

	@Override
	public View getView(String viewName)
	{
		RegistrationForm form = getForm();
		if (form == null)
			return null;
		StandaloneSignupWithAutoRegistrationView view = viewFactory.getObject().init(form);
		VaadinSession vaadinSession = VaadinSession.getCurrent();
		if (vaadinSession != null)
		{
			vaadinSession.setAttribute(StandaloneSignupWithAutoRegistrationView.class, view);
		}
		return view;
	}

	
	private RegistrationForm getForm()
	{
		String formName = RegistrationFormDialogProvider.getFormFromURL();
		try
		{
			List<RegistrationForm> forms = regMan.getForms();
			for (RegistrationForm regForm: forms)
				if (regForm.isPubliclyAvailable() && regForm.getName().equals(formName))
					return regForm;
		} catch (EngineException e)
		{
			LOG.error("Can't load registration forms", e);
		}
		return null;
	}

	@Override
	public void refresh(VaadinRequest request)
	{
		LOG.debug("auto sign up refreshed");
		VaadinSession vaadinSession = VaadinSession.getCurrent();
		if (vaadinSession != null)
		{
			StandaloneSignupWithAutoRegistrationView view = vaadinSession
					.getAttribute(StandaloneSignupWithAutoRegistrationView.class);
			if (view != null)
				view.refresh(request);
		}
	}
}
