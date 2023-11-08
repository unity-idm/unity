/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.registration;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.signupAndEnquiry.forms.RegistrationFormEditorV8;
import io.imunity.webconsole.signupAndEnquiry.forms.SignupAndEnquiryFormsView;
import io.imunity.webconsole.signupAndEnquiry.forms.SignupAndEnquiryFormsView.SignupAndEnquiryFormsNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit registration form view.
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class EditRegistrationFormView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditRegistrationForm";

	private RegistrationFormsController controller;
	private MessageSource msg;
	private RegistrationFormEditorV8 editor;
	private String registrationFormName;

	EditRegistrationFormView(RegistrationFormsController controller, MessageSource msg)
	{
		this.controller = controller;
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		registrationFormName = NavigationHelper.getParam(event, CommonViewParam.name.toString());

		RegistrationForm toEdit;
		try
		{
			toEdit = controller.getRegistrationForm(registrationFormName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(SignupAndEnquiryFormsView.VIEW_NAME);
			return;
		}

		try
		{
			editor = controller.getEditor(toEdit, false);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(SignupAndEnquiryFormsView.VIEW_NAME);
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private void onConfirm()
	{

		RegistrationForm form;
		boolean ignoreRequestsAndInvitations;
		try
		{
			form = editor.getForm();
			ignoreRequestsAndInvitations = editor.isIgnoreRequestsAndInvitations();
		} catch (FormValidationException e)
		{
			NotificationPopup.showFormError(msg, e.getMessage());
			return;
		}

		try
		{
			controller.updateRegistrationForm(form, ignoreRequestsAndInvitations);
		} catch (ControllerException e)
		{

			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(SignupAndEnquiryFormsView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(SignupAndEnquiryFormsView.VIEW_NAME);

	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("new");
	}

	@Component
	public static class EditRegistrationFormNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditRegistrationFormNavigationInfoProvider(ObjectFactory<EditRegistrationFormView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(SignupAndEnquiryFormsNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}
}
