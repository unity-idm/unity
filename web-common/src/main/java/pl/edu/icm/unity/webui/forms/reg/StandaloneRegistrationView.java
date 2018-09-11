/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

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
import pl.edu.icm.unity.types.registration.RedirectConfig;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.PostFormFillingHandler;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreator.RequestEditorCreatedCallback;

/**
 * Used to display a standalone (not within a dialog) registration form.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class StandaloneRegistrationView extends CustomComponent implements View
{
	private RegistrationForm form;
	private RegistrationsManagement regMan;
	private UnityMessageSource msg;
	private UnityServerConfiguration cfg;
	private IdPLoginController idpLoginController;
	private VerticalLayout main;
	private RequestEditorCreator editorCreator;
	private RegistrationRequestEditor currentRegistrationFormEditor;
	private SignUpAuthNController signUpAuthNController;
	private SignUpTopHeaderComponent header;
	private HorizontalLayout formButtons;
	
	@Autowired
	public StandaloneRegistrationView(UnityMessageSource msg,
			@Qualifier("insecure") RegistrationsManagement regMan,
			UnityServerConfiguration cfg, 
			IdPLoginController idpLoginController,
			RequestEditorCreator editorCreator,
			AuthenticationProcessor authnProcessor)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.cfg = cfg;
		this.idpLoginController = idpLoginController;
		this.editorCreator = editorCreator;
		this.signUpAuthNController = new SignUpAuthNController(authnProcessor, new SignUpAuthListener());
	}
	
	public StandaloneRegistrationView init(RegistrationForm form)
	{
		this.form = form;
		return this;
	}
	
	@Override
	public void enter(ViewChangeEvent changeEvent)
	{	
		showFirstStage(RemotelyAuthenticatedContext.getLocalContext(), TriggeringMode.manualStandalone);
	}
	
	private void showFirstStage(RemotelyAuthenticatedContext context, TriggeringMode mode)
	{
		initUIBase();
		
		editorCreator.init(form, signUpAuthNController, context);
		editorCreator.createFirstStage(new EditorCreatedCallback(mode), this::onLocalSignupClickHandler);
	}

	private void showSecondStage(RemotelyAuthenticatedContext context, TriggeringMode mode, 
			boolean withCredentials)
	{
		initUIBase();
		
		editorCreator.init(form, signUpAuthNController, context);
		editorCreator.createSecondStage(new EditorCreatedCallback(mode), withCredentials);
	}

	private void initUIBase()
	{
		main = new VerticalLayout();
		addStyleName("u-standalone-public-form");
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
	}
	
	private void editorCreated(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		this.currentRegistrationFormEditor = editor;
		if (isAutoSubmitPossible(editor, mode))
		{
			onSubmit(editor, mode);
		} else
		{
			showEditorContent(editor, mode);
		}
	}
	
	private void showEditorContent(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		header = new SignUpTopHeaderComponent(cfg, msg, this::onUserAuthnCancel, 
				form.isShowSignInLink() ? form.getUserExistsRedirect() : null);
		main.addComponent(header);
		main.setComponentAlignment(header, Alignment.TOP_RIGHT);

		main.addComponent(editor);
		editor.setWidth(100, Unit.PERCENTAGE);
		main.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);
		
		if (editor.isSubmissionPossible())
		{
			formButtons = new HorizontalLayout();
			Button okButton = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"));
			okButton.addStyleName(Styles.vButtonPrimary.toString());
			okButton.addStyleName("u-reg-submit");
			okButton.addClickListener(event -> onSubmit(editor, mode));
			formButtons.addComponent(okButton);
			
			if (form.getLayoutSettings().isShowCancel())
			{
				Button cancelButton = new Button(msg.getMessage("cancel"));
				cancelButton.addClickListener(event -> onCancel());
				cancelButton.addStyleName("u-reg-cancel");
				formButtons.addComponent(cancelButton);
			}
			formButtons.setMargin(false);
			main.addComponent(formButtons);
			main.setComponentAlignment(formButtons, Alignment.MIDDLE_CENTER);		
		} else
		{
			/*
			 * The editor does not contain any registration form, the local sign up
			 * button instead.
			 */
			formButtons = null;
		}
	}

	private boolean isAutoSubmitPossible(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		return mode == TriggeringMode.afterRemoteLoginFromRegistrationForm
				&& !editor.isUserInteractionRequired();
	}

	private void handleError(Exception e)
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
	
	private void onLocalSignupClickHandler()
	{
		showSecondStage(RemotelyAuthenticatedContext.getLocalContext(), TriggeringMode.manualStandalone,
				true);
	}
	
	private void onCancel()
	{
		RegistrationContext context = new RegistrationContext(false, 
				idpLoginController.isLoginInProgress(), 
				TriggeringMode.manualStandalone);
		new PostFormFillingHandler(idpLoginController, form, msg, 
				regMan.getFormAutomationSupport(form))
			.cancelled(true, context);
		showFinalError(msg.getMessage("StandalonePublicFormView.requestCancelled"), null);
	}
	
	private void onSubmit(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		RegistrationContext context = new RegistrationContext(true, 
				idpLoginController.isLoginInProgress(), 
				mode);
		RegistrationRequest request;
		try
		{
			request = editor.getRequest(isWithCredentials(mode));
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
			showFinalSuccess(msg.getMessage("StandalonePublicFormView.requestSubmitted"),
					form.getSuccessRedirect());
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
			showFinalError(msg.getMessage("StandalonePublicFormView.submissionFailed"), null);
		}
	}

	private boolean isWithCredentials(TriggeringMode mode)
	{
		return mode != TriggeringMode.afterRemoteLoginFromRegistrationForm;
	}

	private void showFinalSuccess(String message, RedirectConfig redirectConfig)
	{
		showFinalCommon(message, redirectConfig, false);
	}

	private void showFinalError(String message, RedirectConfig redirectConfig)
	{
		showFinalCommon(message, redirectConfig, true);
	}
	
	private void showFinalCommon(String message, RedirectConfig redirectConfig, boolean isError)
	{
		RegistrationCompletedComponent finalScreen = new RegistrationCompletedComponent(msg, message, 
				isError, form.getLayoutSettings().getLogoURL(), redirectConfig);
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		wrapper.addComponent(finalScreen);
		wrapper.setComponentAlignment(finalScreen, Alignment.MIDDLE_CENTER);
		wrapper.setSizeFull();
		setSizeFull();
		setCompositionRoot(wrapper);
	}
	
	public void refresh(VaadinRequest request)
	{
		signUpAuthNController.refresh(request);
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
	
	private void enableSharedWidgets(boolean isEnabled)
	{
		if (currentRegistrationFormEditor != null)
			currentRegistrationFormEditor.setEnabled(isEnabled);
		if (formButtons != null)
			formButtons.setEnabled(isEnabled);
	}

	private void switchTo2ndStagePostAuthn(AuthenticationResult result)
	{
		enableSharedComponentsAndHideAuthnProgress();
		showSecondStage(result.getRemoteAuthnContext(), TriggeringMode.afterRemoteLoginFromRegistrationForm,
				false);
	}
	
	
	private class EditorCreatedCallback implements RequestEditorCreatedCallback
	{
		private final TriggeringMode mode;
		
		public EditorCreatedCallback(TriggeringMode mode)
		{
			this.mode = mode;
		}

		@Override
		public void onCreationError(Exception e)
		{
			handleError(e);
		}
		
		@Override
		public void onCreated(RegistrationRequestEditor editor)
		{
			editorCreated(editor, mode);
		}

		@Override
		public void onCancel()
		{
			//nop
		}
	}
	
	private class SignUpAuthListener implements SignUpAuthNControllerListener
	{
		@Override
		public void onUnknownUser(AuthenticationResult result)
		{
			switchTo2ndStagePostAuthn(result);
		}

		@Override
		public void onUserExists(AuthenticationResult result)
		{
			enableSharedComponentsAndHideAuthnProgress();
			showFinalError(msg.getMessage("StandalonePublicFormView.userExistsError"), 
						form.getUserExistsRedirect());
		}

		@Override
		public void onAuthnError(AuthenticationException e, String authenticatorError)
		{
			enableSharedComponentsAndHideAuthnProgress();
			String genericError = msg.getMessage(e.getMessage());
			String errorToShow = authenticatorError == null ? genericError : authenticatorError;
			NotificationPopup.showError(msg.getMessage("AuthenticationUI.authnErrorTitle"), errorToShow);
		}

		@Override
		public void onAuthnCancelled()
		{
			enableSharedComponentsAndHideAuthnProgress();
		}

		@Override
		public void onAuthnStarted(boolean showProgress)
		{
			enableSharedWidgets(false);
			header.setAuthNProgressVisibility(showProgress);
		}
	}
}
