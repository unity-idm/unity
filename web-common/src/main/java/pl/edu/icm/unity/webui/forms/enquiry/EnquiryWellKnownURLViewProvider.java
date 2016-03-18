/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import static pl.edu.icm.unity.server.api.registration.PublicRegistrationURLSupport.ENQUIRY_FRAGMENT_PREFIX;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.webui.common.ConfirmationComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryWellKnownURLView.Callback;
import pl.edu.icm.unity.wellknownurl.SecuredViewProvider;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

/**
 * Standalone view presenting enquiry form.
 * 
 * @author K. Benedyczak
 */
@Component
public class EnquiryWellKnownURLViewProvider implements SecuredViewProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			EnquiryWellKnownURLViewProvider.class);
	@Autowired
	private EnquiryResponseEditorController editorController;
	@Autowired
	private UnityMessageSource msg;
	@Autowired
	private UnityServerConfiguration cfg;
	
	@Override
	public String getViewName(String viewAndParameters)
	{
		if (!viewAndParameters.startsWith(ENQUIRY_FRAGMENT_PREFIX))
			return null;
		String formName = viewAndParameters.substring(ENQUIRY_FRAGMENT_PREFIX.length());
		return editorController.getForm(formName) == null ? null : viewAndParameters;
	}

	@Override
	public View getView(String viewName)
	{
		String formName = viewName.substring(ENQUIRY_FRAGMENT_PREFIX.length());
		if (!editorController.isFormApplicable(formName))
			return new NotApplicableView();
		
		EnquiryForm form = editorController.getForm(formName);
		EnquiryResponseEditor editor;
		try
		{
			editor = editorController.getEditorInstance(form, 
					RemotelyAuthenticatedContext.getLocalContext());
		} catch (Exception e)
		{
			log.error("Can't load enquiry editor", e);
			return null;
		}
		
		return new EnquiryWellKnownURLView(editor, msg, cfg, new Callback()
		{
			@Override
			public void submitted()
			{
				EnquiryResponse request;
				try
				{
					request = editor.getRequest();
				} catch (FormValidationException e)
				{
					NotificationPopup.showError(msg, 
							msg.getMessage("EnquiryResponse.errorSubmit"), e);
					return;
				}
				
				editorController.submitted(request, form, TriggeringMode.manualStandalone);
			}
			
			@Override
			public void cancelled()
			{
				editorController.cancelled(form, TriggeringMode.manualStandalone);
			}
		});
	}

	@Override
	public void setEndpointConfiguration(Properties configuration)
	{
	}

	@Override
	public void setSandboxNotifier(SandboxAuthnNotifier sandboxNotifier,
			String sandboxUrlForAssociation)
	{
	}
	
	private class NotApplicableView extends CustomComponent implements View
	{

		@Override
		public void enter(ViewChangeEvent event)
		{
			VerticalLayout wrapper = new VerticalLayout();
			ConfirmationComponent confirmation = new ConfirmationComponent(Images.error32.getResource(), 
					msg.getMessage("EnquiryWellKnownURLViewProvider.notApplicableEnquiry"));
			wrapper.addComponent(confirmation);
			wrapper.setComponentAlignment(confirmation, Alignment.MIDDLE_CENTER);
			wrapper.setSizeFull();
			setSizeFull();
			setCompositionRoot(wrapper);
		}
	}
}
