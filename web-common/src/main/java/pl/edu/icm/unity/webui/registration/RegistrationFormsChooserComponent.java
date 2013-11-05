/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.authn.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
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
public class RegistrationFormsChooserComponent extends VerticalLayout
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, RegistrationFormsChooserComponent.class);
	protected UnityMessageSource msg;
	protected RegistrationsManagement registrationsManagement;
	protected IdentityEditorRegistry identityEditorRegistry;
	protected CredentialEditorRegistry credentialEditorRegistry;
	protected AttributeHandlerRegistry attributeHandlerRegistry;
	protected AttributesManagement attrsMan;
	protected AuthenticationManagement authnMan;

	protected boolean showNonPublic;
	protected boolean addAutoAccept;
	protected Collection<String> allowedForms;
	protected List<RegistrationForm> displayedForms;
	protected VerticalLayout main;
	protected EventsBus bus;
	
	@Autowired
	public RegistrationFormsChooserComponent(UnityMessageSource msg,
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
		bus = WebSession.getCurrent().getEventBus();
		bus.addListener(new EventListener<RegistrationFormChangedEvent>()
		{
			@Override
			public void handleEvent(RegistrationFormChangedEvent event)
			{
				try
				{
					refresh();
				} catch (EngineException e)
				{
					log.error("Can't refresh registration forms", e);
				}
			}
		}, RegistrationFormChangedEvent.class);
	}

	public void setShowNonPublic(boolean showNonPublic)
	{
		this.showNonPublic = showNonPublic;
	}
	
	public void setAddAutoAccept(boolean addAutoAccept)
	{
		this.addAutoAccept = addAutoAccept;
	}
	
	public void setAllowedForms(Collection<String> allowed)
	{
		allowedForms = allowed;
	}
	
	public List<RegistrationForm> getDisplayedForms()
	{
		return displayedForms;
	}
	
	protected void refresh() throws EngineException
	{
		if (main == null)
			return;
		main.removeAllComponents();
		List<RegistrationForm> forms = registrationsManagement.getForms();
		displayedForms = new ArrayList<>(forms.size());
		boolean available = false;
		for (RegistrationForm form: forms)
		{
			if (!showNonPublic && !form.isPubliclyAvailable())
				continue;
			if (allowedForms != null && !allowedForms.contains(form.getName()))
				continue;
			Button button = new Button(form.getName());
			button.setStyleName(Reindeer.BUTTON_LINK);
			button.addClickListener(new ButtonListener(form));
			main.addComponent(button);
			displayedForms.add(form);
			available = true;
		}
		
		if (!available)
			main.addComponent(new Label(msg.getMessage("RegistrationFormsChooserComponent.noFormsInfo")));
	}
	
	public void initUI()
	{
		setCaption(msg.getMessage("RegistrationFormsChooserComponent.caption"));
		try
		{
			removeAllComponents();
			main = new VerticalLayout();
			main.setSpacing(true);
			main.setMargin(true);
			addComponent(main);
			Button refresh = new Button(msg.getMessage("RegistrationFormsChooserComponent.refresh"));
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
						ErrorPopup.showError(msg.getMessage("RegistrationFormsChooserComponent.errorRefresh"), e);
					}
				}
			});
			addComponent(refresh);
			refresh();
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("RegistrationFormsChooserComponent.errorGetForms"), e);
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
			RegistrationRequestEditorDialog dialog = getDialog(form);
			if (dialog != null)
				dialog.show();
		}
	}
	
	protected boolean addRequest(RegistrationRequest request, boolean andAccept)
	{
		try
		{
			String id = registrationsManagement.submitRegistrationRequest(request);
			if (andAccept)
				registrationsManagement.processReqistrationRequest(id, request, 
						RegistrationRequestAction.accept, null, 
						msg.getMessage("RegistrationFormsChooserComponent.autoAccept"));
			bus.fireEvent(new RegistrationRequestChangedEvent(id));
			return true;
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage(
					"RegistrationFormsChooserComponent.errorRequestSubmit"), e);
			return false;
		}
	}
	
	
	public RegistrationRequestEditorDialog getDialog(RegistrationForm form)
	{
		try
		{
			RegistrationRequestEditor editor = new RegistrationRequestEditor(msg, form, 
					new RemotelyAuthenticatedContext(), identityEditorRegistry, 
					credentialEditorRegistry, 
					attributeHandlerRegistry, attrsMan, authnMan);
			RegistrationRequestEditorDialog dialog = new RegistrationRequestEditorDialog(msg, 
					msg.getMessage("RegistrationFormsChooserComponent.dialogCaption"), 
					editor, addAutoAccept, new RegistrationRequestEditorDialog.Callback()
					{
						@Override
						public boolean newRequest(RegistrationRequest request, boolean autoAccept)
						{
							return addRequest(request, autoAccept);
						}
					});
			return dialog;
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("RegistrationFormsChooserComponent.errorShowFormEdit"), e);
			return null;
		}
	}
}
