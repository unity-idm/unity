/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.signup;

import java.util.Iterator;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.webui.common.ConfirmationComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.PostFormFillingHandler;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormDialogProvider;

/**
 * Used to display a standalone (not within a dialog) registration form.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
class StandaloneSignupWithAutoRegistrationView extends CustomComponent implements View
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, StandaloneSignupWithAutoRegistrationView.class);
	
	private RegistrationForm form;
	private RegistrationsManagement regMan;
	private UnityMessageSource msg;
	private UnityServerConfiguration cfg;
	private IdPLoginController idpLoginController;
	private VerticalLayout main;
	private SignUpWithAutoRegistrationRequestEditorFactory editorCreator;
	private SignUpWithAutoRegistrationRequestEditor editor;
	private SignUpAuthNController signUpAuthNController;
	private SignUpTopHederComponent header;
	private Button okButton;
	private Button cancelButton;
	
	@Autowired
	public StandaloneSignupWithAutoRegistrationView(UnityMessageSource msg,
			@Qualifier("insecure") RegistrationsManagement regMan,
			UnityServerConfiguration cfg, 
			IdPLoginController idpLoginController,
			SignUpWithAutoRegistrationRequestEditorFactory editorCreator,
			AuthenticationProcessor authnProcessor)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.cfg = cfg;
		this.idpLoginController = idpLoginController;
		this.editorCreator = editorCreator;
		this.signUpAuthNController = new SignUpAuthNController(authnProcessor, createSignUpAuthNListener());
	}
	
	public StandaloneSignupWithAutoRegistrationView init(RegistrationForm form)
	{
		this.form = form;
		return this;
	}
	
	@Override
	public void enter(ViewChangeEvent changeEvent)
	{		
		initUIFromScratch();
	}
	
	private void initUIFromScratch()
	{
		initUIBase();
		
		if (!isValidRegistrationRequest())
		{
			showConfirm(Images.error,
					msg.getMessage("StandalonePublicFormView.invalidRegistrationRequest"));
			return;
		}
		
		try
		{
			editor = editorCreator.create(form, RemotelyAuthenticatedContext.getLocalContext(), signUpAuthNController);
			initUIContent();
		} catch (Exception e)
		{
			onEditorCreationError(e);
		}
	}
	
	private boolean isValidRegistrationRequest()
	{
		String registrationCode = RegistrationFormDialogProvider.getCodeFromURL();
		if (registrationCode == null && form.isByInvitationOnly())
		{
			return false;
		}
		return true;
	}

	private void initUIBase()
	{
		main = new VerticalLayout();
	
		addStyleName("u-standalone-public-form");
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
	}
	
	private void initUIContent()
	{
		header = new SignUpTopHederComponent(cfg, msg, this::onUserAuthnCancel);
		main.addComponent(header);
		main.setComponentAlignment(header, Alignment.TOP_RIGHT);

		main.addComponent(editor);
		editor.setWidth(100, Unit.PERCENTAGE);
		main.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);

		HorizontalLayout buttons = new HorizontalLayout();
		
		okButton = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"));
		okButton.addStyleName(Styles.vButtonPrimary.toString());
		okButton.addClickListener(event -> {
			onOK(editor);
		});
		
		cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addClickListener(event -> {
			onCancel();
		});
		
		buttons.addComponents(cancelButton, okButton);
		buttons.setMargin(false);
		main.addComponent(buttons);
		main.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);		
	}

	
	private void onCancel()
	{
		RegistrationContext context = new RegistrationContext(false, 
				idpLoginController.isLoginInProgress(), 
				TriggeringMode.manualStandalone);
		new PostFormFillingHandler(idpLoginController, form, msg, 
				regMan.getFormAutomationSupport(form))
			.cancelled(true, context);
		showConfirm(Images.error,
				msg.getMessage("StandalonePublicFormView.requestCancelled"));
	}
	
	private void onEditorCreationError(Exception e)
	{
		if (e instanceof IllegalArgumentException)
		{
			ErrorComponent ec = new ErrorComponent();
			ec.setError(e.getMessage());
			setCompositionRoot(ec);
		} else
		{
			ErrorComponent ec = new ErrorComponent();
			ec.setError("Can not open registration editor", e);
			setCompositionRoot(ec);
		}
	}
	
	private void onOK(SignUpWithAutoRegistrationRequestEditor editor)
	{
		RegistrationContext context = new RegistrationContext(true, 
				idpLoginController.isLoginInProgress(), 
				TriggeringMode.manualStandalone);
		RegistrationRequest request;
		try
		{
			request = editor.getRequest();
		} catch (Exception e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
		
		
		try
		{
			String requestId = regMan.submitRegistrationRequest(request, context);
			new PostFormFillingHandler(idpLoginController, form, msg, 
					regMan.getFormAutomationSupport(form))
				.submittedRegistrationRequest(requestId, regMan, request, context);
			showConfirm(Images.ok,
					msg.getMessage("StandalonePublicFormView.requestSubmitted"));
		} catch (WrongArgumentException e)
		{
			if (e instanceof IllegalFormContentsException)
				editor.markErrorsFromException((IllegalFormContentsException) e);
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		} catch (Exception e)
		{
			new PostFormFillingHandler(idpLoginController, form, msg, 
					regMan.getFormAutomationSupport(form))
				.submissionError(e, context);
			showConfirm(Images.error,
					msg.getMessage("StandalonePublicFormView.submissionFailed"));
		}
	}

	private void showConfirm(Images icon, String message)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		ConfirmationComponent confirmation = new ConfirmationComponent(icon, message);
		wrapper.addComponent(confirmation);
		wrapper.setComponentAlignment(confirmation, Alignment.MIDDLE_CENTER);
		wrapper.setSizeFull();
		setSizeFull();
		setCompositionRoot(wrapper);
	}
	
	public void refresh(VaadinRequest request)
	{
		editor.refresh(request);
	}
	
	private SignUpAuthNControllerListener createSignUpAuthNListener()
	{
		return new SignUpAuthNControllerListener()
		{
			@Override
			public void onAuthnStarted(boolean showProgress)
			{
				authenticationStartedHandler(showProgress);
			}
			
			@Override
			public void onAuthnCancelled()
			{
				enableSharedComponentsAndHideAuthnProgress();
			}
			
			@Override
			public void onUserExists(AuthenticationResult result)
			{
				enableSharedComponentsAndHideAuthnProgress();
				// TODO: here probably a redirect 
				LOG.info("SignUp authn completed, user exists.");
			}
			
			@Override
			public void onUnknownUser(AuthenticationResult result)
			{
				enableSharedComponentsAndHideAuthnProgress();
				showFormWithDisabledAutoSignUpFor(result);
			}

			@Override
			public void onAuthnError(AuthenticationException e, String authenticatorError)
			{
				enableSharedComponentsAndHideAuthnProgress();
				String genericError = msg.getMessage(e.getMessage());
				String errorToShow = authenticatorError == null ? genericError : authenticatorError;
				NotificationPopup.showError(msg.getMessage("AuthenticationUI.authnErrorTitle"), errorToShow);
			}
		};
	}

	/**
	 * Creates the UI from scratch, fills the form with the remotely obtained
	 * context, and disables the authN option so the login buttons are not
	 * displayed.
	 */
	private void showFormWithDisabledAutoSignUpFor(AuthenticationResult result)
	{
		RegistrationForm formWithDisabledFlows = new RegistrationForm(form.toJson());
		formWithDisabledFlows.getExternalSignupSpec().getSpecs().clear();
		
		/*
		 *  TODO: here more flexible mechanism of controlling the second registration form layout
		 *  is required.
		 */
		Iterator<FormElement> formLayoutIter = formWithDisabledFlows.getLayout().getElements().iterator();
		while (formLayoutIter.hasNext())
		{
			FormElement element = formLayoutIter.next();
			if (FormLayout.CREDENTIAL.equals(element.getType()))
				formLayoutIter.remove();
		}
		
		initUIBase();
		
		try
		{
			editor = editorCreator.create(formWithDisabledFlows, result.getRemoteAuthnContext(), signUpAuthNController);
			initUIContent();
			
			// TODO: check if form allows for auto submit
			onOK(editor);
			
		} catch (Exception e)
		{
			initUIFromScratch();
			LOG.error("Failed to process registration form after authn", e);
			NotificationPopup.showError(msg.getMessage("error"), e.getMessage());
		}
	}
	
	/**
	 * When user clicks cancel button on unity side.
	 */
	private void onUserAuthnCancel()
	{
		signUpAuthNController.manualCancel();
		enableSharedComponentsAndHideAuthnProgress();
	}
	
	private void enableSharedComponentsAndHideAuthnProgress()
	{
		enableSharedWidgets(true);
		header.setAuthNProgressVisibility(false);
	}
	
	private void authenticationStartedHandler(boolean showProgress)
	{
		enableSharedWidgets(false);
		header.setAuthNProgressVisibility(showProgress);
	}

	private void enableSharedWidgets(boolean isEnabled)
	{
		editor.setEnabled(isEnabled);
		okButton.setEnabled(isEnabled);
		cancelButton.setEnabled(isEnabled);
	}
}
