/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Used to display a standalone (not within a dialog) registration form.
 * 
 * @author K. Benedyczak
 */
public class StandalonePublicFormView extends CustomComponent implements View
{
	private RegistrationForm form;
	private RegistrationsManagement regMan;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attrsMan;
	private AuthenticationManagement authnMan;
	private GroupsManagement groupsMan;
	private UnityMessageSource msg;
	private UnityServerConfiguration cfg;
	
	public StandalonePublicFormView(RegistrationForm form, UnityMessageSource msg,
			RegistrationsManagement regMan,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, 
			AuthenticationManagement authnMan,
			GroupsManagement groupsMan,
			UnityServerConfiguration cfg)
	{
		this.form = form;
		this.msg = msg;
		this.regMan = regMan;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attrsMan = attrsMan;
		this.authnMan = authnMan;
		this.groupsMan = groupsMan;
		this.cfg = cfg;
	}
	
	@Override
	public void enter(ViewChangeEvent changeEvent)
	{
		RegistrationRequestEditor editor;
		try
		{
			editor = new RegistrationRequestEditor(msg, form, 
					null, identityEditorRegistry, 
					credentialEditorRegistry, 
					attributeHandlerRegistry, attrsMan, authnMan, groupsMan);
		} catch (EngineException e)
		{
			ErrorComponent ec = new ErrorComponent();
			ec.setError("Can not open registration editor", e);
			setCompositionRoot(ec);
			return;
		}
		
		LocaleChoiceComponent localeChoice = new LocaleChoiceComponent(cfg, msg);
		
		VerticalLayout main = new VerticalLayout();
		main.setWidthUndefined();
		main.setMargin(true);
		main.setSpacing(true);
		
		main.addComponent(localeChoice);
		main.setComponentAlignment(localeChoice, Alignment.TOP_RIGHT);

		main.addComponent(editor);

		Button ok = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"));
		ok.addStyleName(Styles.vButtonPrimary.toString());
		ok.addClickListener(event -> {
			accept(editor);
		});
		
		Label cancel = new Label(msg.getMessage("StandalonePublicFormView.cancel"));
		main.addComponents(ok, cancel);
		main.setComponentAlignment(ok, Alignment.MIDDLE_CENTER);
		addStyleName("u-standalone-public-form");
		setCompositionRoot(main);
		setWidthUndefined();
	}
	
	private void accept(RegistrationRequestEditor editor)
	{
		
		try
		{
			RegistrationRequest request = editor.getRequest();
			String requestId = regMan.submitRegistrationRequest(request, false);
			RegistrationFormLauncher.invokePostRegistrationAction(form, requestId, msg, regMan);
		} catch (FormValidationException e) 
		{
			if (e.getMessage() == null || e.getMessage().equals(""))
				NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), 
						msg.getMessage("Generic.formErrorHint"));
			else
				NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"RegistrationFormsChooserComponent.errorRequestAutoAccept"), e);
		}
	}

}
