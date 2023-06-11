/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.forms;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.signupAndEnquiry.SignupAndEnquiryNavigationInfoProvider;
import io.imunity.webconsole.signupAndEnquiry.enquiry.EnquiryFormsComponent;
import io.imunity.webconsole.signupAndEnquiry.enquiry.EnquiryFormsController;
import io.imunity.webconsole.signupAndEnquiry.formfill.AdminEnquiryFormLauncher;
import io.imunity.webconsole.signupAndEnquiry.formfill.AdminRegistrationFormLauncher;
import io.imunity.webconsole.signupAndEnquiry.registration.RegistrationFormsComponent;
import io.imunity.webconsole.signupAndEnquiry.registration.RegistrationFormsController;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Lists all registration and enquiry forms
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class SignupAndEnquiryFormsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "SignupAndEnquirySetup";

	private MessageSource msg;
	private RegistrationFormsController regController;
	private EnquiryFormsController enqController;
	private AdminRegistrationFormLauncher adminRegistrationFormLauncher;
	private AdminEnquiryFormLauncher adminEnquiryFormLauncher;

	@Autowired
	SignupAndEnquiryFormsView(MessageSource msg, RegistrationFormsController regController,
			EnquiryFormsController enqController,
			AdminRegistrationFormLauncher adminRegistrationFormLauncher,
			AdminEnquiryFormLauncher adminEnquiryFormLauncher)
	{
		this.msg = msg;
		this.regController = regController;
		this.enqController = enqController;
		this.adminRegistrationFormLauncher = adminRegistrationFormLauncher;
		this.adminEnquiryFormLauncher = adminEnquiryFormLauncher;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();

		main.addComponent(new RegistrationFormsComponent(msg, regController, adminRegistrationFormLauncher));
		main.addComponent(new Label());
		main.addComponent(new EnquiryFormsComponent(msg, enqController, adminEnquiryFormLauncher));
		main.addComponent(new Label());
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.signupAndEnquiry.forms");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class SignupAndEnquiryFormsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = VIEW_NAME;
		
		@Autowired
		public SignupAndEnquiryFormsNavigationInfoProvider(MessageSource msg,
				SignupAndEnquiryNavigationInfoProvider parent,
				ObjectFactory<SignupAndEnquiryFormsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(SignupAndEnquiryNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.signupAndEnquiry.forms"))
					.withIcon(Images.form.getResource())
					.withPosition(10).build());

		}
	}
}
