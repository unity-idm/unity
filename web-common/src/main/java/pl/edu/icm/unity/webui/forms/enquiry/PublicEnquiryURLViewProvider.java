/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.server.VaadinSession;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.webui.forms.PublicFormURLProviderBase;
import pl.edu.icm.unity.webui.forms.StandalonePublicView;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormDialogProvider;

/**
 * Provides access to public registration forms via well-known links
 * @author K. Benedyczak
 */
@Component
public class PublicEnquiryURLViewProvider extends PublicFormURLProviderBase
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, PublicEnquiryURLViewProvider.class);
	private EnquiryManagement enqMan;

	private ObjectFactory<StandalonePublicEnquiryView> viewFactory;
	
	@Autowired
	public PublicEnquiryURLViewProvider(
			@Qualifier("insecure") EnquiryManagement enqMan,
			ObjectFactory<StandalonePublicEnquiryView> viewFactory, UnityMessageSource msg)
		
	{
		this.enqMan = enqMan;
		this.viewFactory = viewFactory;
	}

	@Override
	public String getViewName(String viewAndParameters)
	{
		String formName = getFormName(viewAndParameters);
		if (formName == null)
			return null;
		
		EnquiryForm form = getForm(formName);
		if (form == null)
			return null;
		
		return viewAndParameters;
	}

	@Override
	public View getView(String viewName)
	{
		String formName = getFormName(viewName);
		EnquiryForm form = getForm(formName);
		if (form == null)
			return null;		
		
		StandalonePublicEnquiryView view = viewFactory.getObject().init(form);
	
		VaadinSession vaadinSession = VaadinSession.getCurrent();
		if (vaadinSession != null)
		{
			vaadinSession.setAttribute(StandalonePublicEnquiryView.class, view);
		}
		return view;
	}
	
	@Override
	protected String getFormName(String viewAndParameters)
	{
		if (PublicRegistrationURLSupport.ENQUIRY_VIEW.equals(viewAndParameters))
			return RegistrationFormDialogProvider.getFormFromURL();
		return null;
	}
	
	private EnquiryForm getForm(String name)
	{
		try
		{
			List<EnquiryForm> forms = enqMan.getEnquires();
			for (EnquiryForm enqForm: forms)
				if (enqForm.getName().equals(name))
					return enqForm;
		} catch (EngineException e)
		{
			LOG.error("Can't load enquiry forms", e);
		}
		return null;
	}

	@Override
	protected StandalonePublicView getViewFromSession()
	{
		VaadinSession vaadinSession  = VaadinSession.getCurrent();
		if (vaadinSession == null)
			return null;
		
		return vaadinSession.getAttribute(StandalonePublicEnquiryView.class);
	}
	
}
