/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.ui.VerticalLayout;



/**
 * Responsible for showing registration forms list and allowing to launch editor of a chosen one.
 * <p>
 * This extension of {@link RegistrationFormsChooserComponent} uses insecure engine modules. 
 * The auto accept feature is removed in the code. The components are used only to get the list of 
 * available credential definitions, attribute types, registration forms and to submit the request.
 *  
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class InsecureRegistrationFormsChooserComponent extends RegistrationFormsChooserComponent
{
	@Autowired
	public InsecureRegistrationFormsChooserComponent(UnityMessageSource msg,
			@Qualifier("insecure") RegistrationsManagement registrationsManagement,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") AttributesManagement attrsMan, 
			@Qualifier("insecure") AuthenticationManagement authnMan)
	{
		super(msg, registrationsManagement, identityEditorRegistry, 
				credentialEditorRegistry, attributeHandlerRegistry, attrsMan, authnMan);
	}

	@Override
	public void initUI()
	{
		try
		{
			removeAllComponents();
			main = new VerticalLayout();
			main.setSpacing(true);
			main.setMargin(true);
			addComponent(main);
			refresh();
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("RegistrationFormsChooserComponent.errorGetForms"), e);
			removeAllComponents();
			addComponent(error);
		}
	}

	@Override
	protected boolean addRequest(RegistrationRequest request, boolean autoAccept)
	{
		try
		{
			String id = registrationsManagement.submitRegistrationRequest(request);
			bus.fireEvent(new RegistrationRequestChangedEvent(id));
			
			ErrorPopup.showNotice(msg.getMessage("RegistrationFormsChooserComponent.requestSubmitted"), 
					msg.getMessage("RegistrationFormsChooserComponent.requestSubmittedInfo"));
			return true;
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage(
					"RegistrationFormsChooserComponent.errorRequestSubmit"), e);
			return false;
		}
	}
}
