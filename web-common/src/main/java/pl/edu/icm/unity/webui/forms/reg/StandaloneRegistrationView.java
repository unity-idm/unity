/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
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
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IdentityExistsException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.finalization.WorkflowCompletedComponent;
import pl.edu.icm.unity.webui.forms.FormsUIHelper;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;
import pl.edu.icm.unity.webui.forms.StandalonePublicView;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestEditor.Stage;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreator.RequestEditorCreatedCallback;

/**
 * Used to display a standalone (not within a dialog) registration form.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class StandaloneRegistrationView extends CustomComponent implements StandalonePublicView
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StandaloneRegistrationView.class);
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
	private PostFillingHandler postFillHandler;
	private Runnable customCancelHandler;
	private Runnable completedRegistrationHandler;
	private Runnable gotoSignInRedirector;
	private AutoLoginAfterSignUpProcessor autoLoginProcessor;
	
	@Autowired
	public StandaloneRegistrationView(UnityMessageSource msg,
			@Qualifier("insecure") RegistrationsManagement regMan,
			UnityServerConfiguration cfg, 
			IdPLoginController idpLoginController,
			RequestEditorCreator editorCreator,
			AuthenticationProcessor authnProcessor,
			AutoLoginAfterSignUpProcessor autoLogin)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.cfg = cfg;
		this.idpLoginController = idpLoginController;
		this.editorCreator = editorCreator;
		this.signUpAuthNController = new SignUpAuthNController(authnProcessor, new SignUpAuthListener());
		this.autoLoginProcessor = autoLogin;
	}
	
	@Override
	public String getFormName()
	{
		if (form == null)
			return null;
		return form.getName();
	}
	
	public StandaloneRegistrationView init(RegistrationForm form)
	{
		this.form = form;
		String pageTitle = form.getPageTitle() == null ? null : form.getPageTitle().getValue(msg);
		this.postFillHandler = new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), true);
		return this;
	}
	
	@Override
	public void enter(ViewChangeEvent changeEvent)
	{	
		enter(TriggeringMode.manualStandalone, null, null, null);
	}
	
	/**
	 * @param customCancelHandler
	 *            Used only in case where registration form is displayed from
	 *            authentication screen.
	 * 
	 *            The custom cancel handler is used when there is no explicit
	 *            TriggeringState.CANCELLED finalization configured in the form. 
	 *            It is supposed to handle the UI changes.
	 * @param completedRegistrationHandler run when registration is completed. It is notification which should
	 * cause reset of the UI during the *subsequent* request in scope of the current session 
	 */
	public void enter(TriggeringMode mode, Runnable customCancelHandler, Runnable completedRegistrationHandler,
			Runnable gotoSignInRedirector)
	{
		this.customCancelHandler = customCancelHandler;
		this.completedRegistrationHandler = completedRegistrationHandler;
		this.gotoSignInRedirector = gotoSignInRedirector;
		showFirstStage(RemotelyAuthenticatedContext.getLocalContext(), mode);
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
		if (form.getPageTitle() != null)
			Page.getCurrent().setTitle(form.getPageTitle().getValue(msg));
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
				getGoToSignInRedirector(editor));
		main.addComponent(header);
		main.setComponentAlignment(header, Alignment.TOP_RIGHT);

		main.addComponent(editor);
		editor.setWidth(100, Unit.PERCENTAGE);
		main.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);
		
		Button okButton = null;
		Button cancelButton = null;
		if (editor.isSubmissionPossible())
		{
			okButton = createOKButton(editor, mode);
			
			if (form.getLayoutSettings().isShowCancel())
			{
				cancelButton = createCancelButton();
			}
		} else if (isStanaloneModeFromAuthNScreen() && form.getLayoutSettings().isShowCancel())
		{
			cancelButton = createCancelButton();
		}
		
		if (okButton != null)
		{
			formButtons = new HorizontalLayout();
			formButtons.setWidth(editor.formWidth(), editor.formWidthUnit());
			formButtons.addComponent(okButton);
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
		
		if (cancelButton != null)
		{
			main.addComponent(cancelButton);
			main.setComponentAlignment(cancelButton, Alignment.MIDDLE_CENTER);
		}
	}
	
	private Button createOKButton(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		Button okButton = FormsUIHelper.createOKButton(
				msg.getMessage("RegistrationRequestEditorDialog.submitRequest"),
				event -> onSubmit(editor, mode));
		return okButton;
	}

	private Button createCancelButton()
	{
		Button cancelButton = FormsUIHelper.createCancelButton(msg.getMessage("cancel"), event -> onCancel());
		cancelButton.setStyleName(Styles.vButtonLink.toString());
		return cancelButton;
	}
	
	private Optional<Runnable> getGoToSignInRedirector(RegistrationRequestEditor editor)
	{
		if (!form.isShowSignInLink() || editor.getStage() != Stage.FIRST)
			return Optional.empty();

		if (gotoSignInRedirector != null)
			return Optional.of(gotoSignInRedirector);
		
		if (Strings.isEmpty(form.getSignInLink()))
			return Optional.empty();
		
		Runnable signinRedirector = () -> 
		{
			if (completedRegistrationHandler != null)
				completedRegistrationHandler.run();
			Page.getCurrent().open(form.getSignInLink(), null);
		};
		return Optional.of(signinRedirector);
	}
	
	private boolean isAutoSubmitPossible(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		return mode == TriggeringMode.afterRemoteLoginFromRegistrationForm
				&& !editor.isUserInteractionRequired();
	}

	private void handleError(Exception e, ErrorCause cause)
	{
		log.warn("Registration error", e);
		WorkflowFinalizationConfiguration finalScreenConfig = postFillHandler
				.getFinalRegistrationConfigurationOnError(cause.getTriggerState());
		gotoFinalStep(finalScreenConfig);
	}
	
	private void onLocalSignupClickHandler()
	{
		showSecondStage(RemotelyAuthenticatedContext.getLocalContext(), TriggeringMode.manualStandalone,
				true);
	}
	
	private void onCancel()
	{
		if (isCustomCancelHandlerEnabled())
		{
			customCancelHandler.run();
		}
		else
		{
			WorkflowFinalizationConfiguration finalScreenConfig = postFillHandler
					.getFinalRegistrationConfigurationOnError(TriggeringState.CANCELLED);
			gotoFinalStep(finalScreenConfig);
		}
	}
	
	private boolean isCustomCancelHandlerEnabled()
	{
		if (isStanaloneModeFromAuthNScreen()
				&& !postFillHandler.hasConfiguredFinalizationFor(TriggeringState.CANCELLED))
		{
			return true;
		}
		return false;
	}
	
	private boolean isStanaloneModeFromAuthNScreen()
	{
		return customCancelHandler != null;
	}

	private void onSubmit(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		RegistrationContext context = new RegistrationContext(idpLoginController.isLoginInProgress(), mode);
		RegistrationRequest request = editor.getRequestWithStandardErrorHandling(isWithCredentials(mode))
				.orElse(null);
		if (request == null)
			return;
		try
		{
			String requestId = regMan.submitRegistrationRequest(request, context);
			RegistrationRequestState requestState = getRequestStatus(requestId);
			
			autoLoginProcessor.signInIfPossible(editor, requestState);
			
			RegistrationRequestStatus effectiveStateForFinalization = requestState == null 
					? RegistrationRequestStatus.rejected 
					: requestState.getStatus();
			WorkflowFinalizationConfiguration finalScreenConfig = 
					postFillHandler.getFinalRegistrationConfigurationPostSubmit(requestId,
							effectiveStateForFinalization);
			gotoFinalStep(finalScreenConfig);
		} catch (IdentityExistsException e)
		{
			WorkflowFinalizationConfiguration finalScreenConfig = 
					postFillHandler.getFinalRegistrationConfigurationOnError(TriggeringState.PRESET_USER_EXISTS);
			gotoFinalStep(finalScreenConfig);
			
		} catch (WrongArgumentException e)
		{
			if (e instanceof IllegalFormContentsException)
				editor.markErrorsFromException((IllegalFormContentsException) e);
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		} catch (Exception e)
		{
			log.warn("Registration request submision failed", e);
			WorkflowFinalizationConfiguration finalScreenConfig = 
					postFillHandler.getFinalRegistrationConfigurationOnError(TriggeringState.GENERAL_ERROR);
			gotoFinalStep(finalScreenConfig);
		}
	}

	private boolean isWithCredentials(TriggeringMode mode)
	{
		return mode != TriggeringMode.afterRemoteLoginFromRegistrationForm;
	}

	private void gotoFinalStep(WorkflowFinalizationConfiguration config)
	{
		log.debug("Registration is finalized, status: {}", config);
		if (completedRegistrationHandler != null)
			completedRegistrationHandler.run();
		if (config.autoRedirect)
			redirect(config.redirectURL, idpLoginController);
		else
			showFinalScreen(config);
	}
	
	private void showFinalScreen(WorkflowFinalizationConfiguration config)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		wrapper.setSizeFull();
		setSizeFull();
		setCompositionRoot(wrapper);

		WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, 
			url -> redirect(url, idpLoginController));
		wrapper.addComponent(finalScreen);
		wrapper.setComponentAlignment(finalScreen, Alignment.MIDDLE_CENTER);
	}
	
	public void refresh(VaadinRequest request)
	{
		signUpAuthNController.refresh(request);
		if (currentRegistrationFormEditor != null)
			currentRegistrationFormEditor.focusFirst();
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
		header.setInteractionsEnabled(isEnabled);
	}

	private void switchTo2ndStagePostAuthn(AuthenticationResult result)
	{
		enableSharedComponentsAndHideAuthnProgress();
		showSecondStage(result.getRemoteAuthnContext(), TriggeringMode.afterRemoteLoginFromRegistrationForm,
				false);
	}
	
	private static void redirect(String redirectUrl, IdPLoginController loginController)
	{
		loginController.breakLogin();
		Page.getCurrent().open(redirectUrl, null);
	}
	
	
	private RegistrationRequestState getRequestStatus(String requestId) 
	{
		try
		{
			return regMan.getRegistrationRequest(requestId);
		} catch (EngineException e)
		{
			log.error("Shouldn't happen: can't get request status, assuming rejected", e);
		}
		return null;
	}
	
	private class EditorCreatedCallback implements RequestEditorCreatedCallback
	{
		private final TriggeringMode mode;
		
		public EditorCreatedCallback(TriggeringMode mode)
		{
			this.mode = mode;
		}

		@Override
		public void onCreationError(Exception e, ErrorCause cause)
		{
			handleError(e, cause);
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
			log.debug("External authentication resulted in unknown user, proceeding to 2nd stage");
			switchTo2ndStagePostAuthn(result);
		}

		@Override
		public void onUserExists(AuthenticationResult result)
		{
			log.debug("External authentication resulted in existing user, aborting registration");
			enableSharedComponentsAndHideAuthnProgress();
			WorkflowFinalizationConfiguration finalScreenConfig = 
					postFillHandler.getFinalRegistrationConfigurationOnError(TriggeringState.PRESET_USER_EXISTS);
			gotoFinalStep(finalScreenConfig);
		}

		@Override
		public void onAuthnError(AuthenticationException e, String authenticatorError)
		{
			log.debug("External authentication failed, aborting: {}, {}", e.toString(), authenticatorError);
			enableSharedComponentsAndHideAuthnProgress();
			String genericError = msg.getMessage(e.getMessage());
			String errorToShow = authenticatorError == null ? genericError : authenticatorError;
			NotificationPopup.showError(errorToShow, "");
		}

		@Override
		public void onAuthnCancelled()
		{
			log.debug("External authentication cancelled, performing reset");
			enableSharedComponentsAndHideAuthnProgress();
		}

		@Override
		public void onAuthnStarted(boolean showProgress)
		{
			log.debug("External authentication started");
			enableSharedWidgets(false);
			header.setAuthNProgressVisibility(showProgress);
		}
	}
}
