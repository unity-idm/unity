/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formfill;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryFormChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormChangedEvent;



/**
 * Responsible for showing registration and enquiry forms list and allowing to launch editor of a chosen one.
 * The component is naturally intended for use in admin UI.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FormsChooserComponent extends CustomComponent
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, FormsChooserComponent.class);
	private UnityMessageSource msg;
	private RegistrationsManagement registrationsManagement;
	private EnquiryManagement enquiryManagement;
	private AdminRegistrationFormLauncher registrationFormLauncher;

	private VerticalLayout registrationForms;
	private EventsBus bus;
	private VerticalLayout enquiryForms;
	private AdminEnquiryFormLauncher enquiryFormLauncher;
	
	@Autowired
	public FormsChooserComponent(UnityMessageSource msg,
			RegistrationsManagement registrationsManagement,
			AdminRegistrationFormLauncher registrationFormLauncher,
			AdminEnquiryFormLauncher enquiryFormLauncher,
			EnquiryManagement enquiryManagement)
	{
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
		this.registrationFormLauncher = registrationFormLauncher;
		this.enquiryFormLauncher = enquiryFormLauncher;
		this.enquiryManagement = enquiryManagement;
		bus = WebSession.getCurrent().getEventBus();
		bus.addListener(event -> refreshQuiet(), RegistrationFormChangedEvent.class);
		bus.addListener(event -> refreshQuiet(), EnquiryFormChangedEvent.class);
		initUI();
	}
	
	private void refreshQuiet()
	{
		try
		{
			refresh();
		} catch (EngineException e)
		{
			log.error("Can't refresh registration forms", e);
		}
	}
	
	private void refresh() throws EngineException
	{
		refreshRegistrations();
		refreshEnquires();
	}	
	
	private void refreshRegistrations() throws EngineException
	{
		registrationForms.removeAllComponents();
		List<RegistrationForm> forms = registrationsManagement.getForms();
		for (RegistrationForm form: forms)
		{
			registrationForms.addComponent(createLaunchButton(form.getName(), 
					new RegistrationButtonListener(form)));
		}
		
		if (forms.isEmpty())
			registrationForms.addComponent(new Label(
					msg.getMessage("FormsChooserComponent.noFormsInfo")));
	}

	private void refreshEnquires() throws EngineException
	{
		enquiryForms.removeAllComponents();
		List<EnquiryForm> forms = enquiryManagement.getEnquires();
		for (EnquiryForm form: forms)
		{
			enquiryForms.addComponent(createLaunchButton(form.getName(), new EnquiryButtonListener(form)));
		}
		
		if (forms.isEmpty())
			enquiryForms.addComponent(new Label(
					msg.getMessage("FormsChooserComponent.noFormsInfo")));
	}
	
	private Button createLaunchButton(String name, ClickListener listener)
	{
		Button button = new Button(name);
		button.setStyleName(Styles.vButtonLink.toString());
		button.addClickListener(listener);
		return button;
	}
	
	private void initUI()
	{
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("FormsChooserComponent.caption"));
		try
		{
			VerticalLayout main = new VerticalLayout();
			main.setSpacing(true);
			main.setMargin(true);
			
			registrationForms = new VerticalLayout();
			registrationForms.setSpacing(true);
			registrationForms.setMargin(false);
			registrationForms.setCaption(msg.getMessage("FormsChooserComponent.registrationForms"));
			main.addComponent(registrationForms);

			enquiryForms = new VerticalLayout();
			enquiryForms.setSpacing(true);
			enquiryForms.setMargin(false);
			enquiryForms.setCaption(msg.getMessage("FormsChooserComponent.enquiryForms"));
			main.addComponent(enquiryForms);
			
			main.addComponent(createRefreshButton());
			setCompositionRoot(main);
			refresh();
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("FormsChooserComponent.errorGetForms"), e);
			setCompositionRoot(error);
		}
	}

	
	private Button createRefreshButton()
	{
		Button refresh = new Button(msg.getMessage("FormsChooserComponent.refresh"));
		refresh.setIcon(Images.refresh.getResource());
		refresh.addClickListener(event -> {
			try
			{
				refresh();
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, 
						msg.getMessage("FormsChooserComponent.errorRefresh"), e);
			}
		});
		return refresh;
	}
	
	private class RegistrationButtonListener implements ClickListener
	{
		private RegistrationForm form;

		public RegistrationButtonListener(RegistrationForm form)
		{
			this.form = form;
		}
		
		@Override
		public void buttonClick(ClickEvent event)
		{
			registrationFormLauncher.showRegistrationDialog(form, 
					RemotelyAuthenticatedContext.getLocalContext(), 
					TriggeringMode.manualAdmin, FormsChooserComponent.this::handleError);
		}
	}

	private class EnquiryButtonListener implements ClickListener
	{
		private EnquiryForm form;

		public EnquiryButtonListener(EnquiryForm form)
		{
			this.form = form;
		}
		
		@Override
		public void buttonClick(ClickEvent event)
		{
			enquiryFormLauncher.showDialog(form, 
					RemotelyAuthenticatedContext.getLocalContext(), 
					FormsChooserComponent.this::handleError);
		}
	}
	
	private void handleError(Exception error)
	{
		NotificationPopup.showError(msg, 
				msg.getMessage("FormsChooserComponent.errorShowFormEdit"), error);
	}
}
