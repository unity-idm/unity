/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
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
import pl.edu.icm.unity.types.registration.FormLayoutUtils;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.webui.common.ConfirmationComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.Images;
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
public class StandaloneRegistrationView extends CustomComponent implements View, SignUpAuthNControllerListener
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
	private SignUpTopHederComponent header;
	private HorizontalLayout formBbuttons;
	
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
		this.signUpAuthNController = new SignUpAuthNController(authnProcessor, this);
	}
	
	public StandaloneRegistrationView init(RegistrationForm form)
	{
		this.form = form;
		return this;
	}
	
	@Override
	public void enter(ViewChangeEvent changeEvent)
	{	
		initUIFromScratch(form.getEffectivePrimaryFormLayout(msg),
				RemotelyAuthenticatedContext.getLocalContext(), TriggeringMode.manualStandalone);
	}
	
	private void initUIFromScratch(FormLayout layout, RemotelyAuthenticatedContext context, TriggeringMode mode)
	{
		initUIBase();
		
		editorCreator.init(form, signUpAuthNController, this::onLocalSignupClickHandler, 
				layout, context);
		editorCreator.invoke(new RequestEditorCreatedCallback()
		{
			@Override
			public void onCreationError(Exception e)
			{
				handleError(e);
			}
			
			@Override
			public void onCreated(RegistrationRequestEditor editor)
			{
				editorCreated(editor, layout, mode);
			}

			@Override
			public void onCancel()
			{
				//nop
			}
		});
	}

	private void initUIBase()
	{
		main = new VerticalLayout();
		addStyleName("u-standalone-public-form");
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
	}
	
	private void editorCreated(RegistrationRequestEditor editor, FormLayout effectiveLayout, TriggeringMode mode)
	{
		this.currentRegistrationFormEditor = editor;
		if (isAutoSubbmitPossible(editor, mode))
		{
			onSubmit(editor, mode);
		} else
		{
			showEditorContent(editor, effectiveLayout, mode);
		}
	}
	
	private void showEditorContent(RegistrationRequestEditor editor, FormLayout effectiveLayout, TriggeringMode mode)
	{
		
		header = new SignUpTopHederComponent(cfg, msg, this::onUserAuthnCancel);
		main.addComponent(header);
		main.setComponentAlignment(header, Alignment.TOP_RIGHT);

		main.addComponent(editor);
		editor.setWidth(100, Unit.PERCENTAGE);
		main.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);
		
		if (!FormLayoutUtils.isLayoutWithLocalSignup(effectiveLayout))
		{
			formBbuttons = new HorizontalLayout();
			Button okButton = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"));
			okButton.addStyleName(Styles.vButtonPrimary.toString());
			okButton.addClickListener(event -> onSubmit(editor, mode));
			
			Button cancelButton = new Button(msg.getMessage("cancel"));
			cancelButton.addClickListener(event -> onCancel());
			formBbuttons.addComponents(okButton, cancelButton);
			formBbuttons.setMargin(false);
			main.addComponent(formBbuttons);
			main.setComponentAlignment(formBbuttons, Alignment.MIDDLE_CENTER);		
		} else
		{
			/*
			 * The editor does not contain any registration form, the local sign up
			 * button instead.
			 */
			formBbuttons = null;
		}
	}

	private boolean isAutoSubbmitPossible(RegistrationRequestEditor editor, TriggeringMode mode)
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
		initUIFromScratch(form.getEffectiveSecondaryFormLayout(msg),
				RemotelyAuthenticatedContext.getLocalContext(), TriggeringMode.manualStandalone);
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

	private boolean isWithCredentials(TriggeringMode mode)
	{
		return mode != TriggeringMode.afterRemoteLoginFromRegistrationForm;
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
		if (formBbuttons != null)
			formBbuttons.setEnabled(isEnabled);
	}

	private void authenticationStartedHandler(boolean showProgress)
	{
		enableSharedWidgets(false);
		header.setAuthNProgressVisibility(showProgress);
	}
	
	@Override
	public void onUnknownUser(AuthenticationResult result)
	{
		enableSharedComponentsAndHideAuthnProgress();
		initUIFromScratch(form.getEffectiveSecondaryFormLayoutWithoutCredentials(msg),
				result.getRemoteAuthnContext(), TriggeringMode.afterRemoteLoginFromRegistrationForm);
	}

	@Override
	public void onUserExists(AuthenticationResult result)
	{
		enableSharedComponentsAndHideAuthnProgress();
		String redirectUrl = form.getExternalSignupSpec().getUserExistsRedirectUrl();
		if (!StringUtils.isEmpty(redirectUrl))
			Page.getCurrent().open(redirectUrl, null);
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
		authenticationStartedHandler(showProgress);
	}
}
