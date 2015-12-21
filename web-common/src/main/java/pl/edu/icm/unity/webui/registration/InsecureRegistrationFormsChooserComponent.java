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

import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.api.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.ErrorComponent;

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
			InsecureRegistrationFormLauncher formLauncher)
	{
		super(msg, registrationsManagement, formLauncher);
	}

	@Override
	public void initUI(TriggeringMode mode)
	{
		this.mode = mode;
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
}
