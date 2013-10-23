/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formfill;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.authn.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;

/**
 * Responsible for showing registration forms list and allowing to launch editor of a chosen one.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RegistrationRequestsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private RegistrationsManagement registrationsManagement;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attrsMan;
	private AuthenticationManagement authnMan;

	private boolean showNonPublic;
	private VerticalLayout main;
	
	@Autowired
	public RegistrationRequestsComponent(UnityMessageSource msg,
			RegistrationsManagement registrationsManagement,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, AuthenticationManagement authnMan)
	{
		super();
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attrsMan = attrsMan;
		this.authnMan = authnMan;
	}

	public void setShowNonPublic(boolean showNonPublic)
	{
		this.showNonPublic = showNonPublic;
	}
	
	private void refresh() throws EngineException
	{
		main.removeAllComponents();
		List<RegistrationForm> forms = registrationsManagement.getForms();
		for (RegistrationForm form: forms)
		{
			if (!showNonPublic && !form.isPubliclyAvailable())
				continue;
			Button button = new Button(form.getName());
			button.setStyleName(Reindeer.BUTTON_LINK);
			button.addClickListener(new ButtonListener(form));
			main.addComponent(button);
		}
	}
	
	public void initUI()
	{
		setCaption(msg.getMessage("RegistrationRequestsComponent.caption"));
		try
		{
			main = new VerticalLayout();
			main.setSpacing(true);
			main.setMargin(true);
			addComponent(main);
			Button refresh = new Button(msg.getMessage("RegistrationRequestsComponent.refresh"));
			refresh.setIcon(Images.refresh.getResource());
			refresh.addClickListener(new ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					try
					{
						refresh();
					} catch (EngineException e)
					{
						ErrorPopup.showError(msg.getMessage("RegistrationRequestsComponent.errorRefresh"), e);
					}
				}
			});
			addComponent(refresh);
			refresh();
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("RegistrationRequestsComponent.errorGetForms"), e);
			removeAllComponents();
			addComponent(error);
		}
	}

	
	private class ButtonListener implements ClickListener
	{
		private RegistrationForm form;

		public ButtonListener(RegistrationForm form)
		{
			this.form = form;
		}
		
		@Override
		public void buttonClick(ClickEvent event)
		{
			showDialog(form);
		}
	}
	
	private boolean addRequest(RegistrationRequest request)
	{
		try
		{
			registrationsManagement.submitRegistrationRequest(request);
			return true;
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage(
					"RegistrationRequestsComponent.errorRequestSubmit"), e);
			return false;
		}
	}
	
	private void showDialog(RegistrationForm form)
	{
		try
		{
			RegistrationRequestEditor editor = new RegistrationRequestEditor(msg, form, 
					new RemotelyAuthenticatedContext(), identityEditorRegistry, 
					credentialEditorRegistry, 
					attributeHandlerRegistry, attrsMan, authnMan);
			RegistrationRequestEditorDialog dialog = new RegistrationRequestEditorDialog(msg, 
					msg.getMessage("RegistrationRequestsComponent.dialogCaption"), 
					editor, new RegistrationRequestEditorDialog.Callback()
					{
						@Override
						public boolean newRequest(RegistrationRequest request)
						{
							return addRequest(request);
						}
					});
			dialog.show();
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("RegistrationRequestsComponent.errorShowFormEdit"), e);
		}
	}
}
