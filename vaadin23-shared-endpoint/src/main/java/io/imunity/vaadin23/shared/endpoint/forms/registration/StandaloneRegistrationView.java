/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.forms.registration;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.components.WorkflowCompletedComponent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IdentityExistsException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.registration.*;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter.PostAuthenticationDecissionWithContext;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;

import java.util.List;
import java.util.Optional;

import static pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement.REGISTRATION_PATH;

@Route(value = REGISTRATION_PATH + ":" + StandaloneRegistrationView.FORM_PARAM)
public class StandaloneRegistrationView extends Composite<Div> implements HasDynamicTitle, BeforeEnterObserver
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StandaloneRegistrationView.class);

	public static final String FORM_PARAM = "form";
	public static final String REG_CODE_PARAM = "regcode";
	private final RegistrationsManagement regMan;
	private final MessageSource msg;
	private final UnityServerConfiguration cfg;
	private final IdPLoginController idpLoginController;
	private final RequestEditorCreatorV23 editorCreator;
	private final AutoLoginAfterSignUpProcessorV23 autoLoginProcessor;
	private final NotificationPresenter notificationPresenter;


	private RegistrationForm form;
	private String registrationCode;

	private PostFillingHandler postFillHandler;
	private VerticalLayout main;
	private SignUpTopHeaderComponent header;
	private HorizontalLayout formButtons;
	private RegistrationRequestEditor currentRegistrationFormEditor;
	private Runnable customCancelHandler;
	private Runnable completedRegistrationHandler;
	private Runnable gotoSignInRedirector;
	
	@Autowired
	public StandaloneRegistrationView(MessageSource msg,
	                                  @Qualifier("insecure") RegistrationsManagement regMan,
	                                  UnityServerConfiguration cfg,
	                                  IdPLoginController idpLoginController,
	                                  RequestEditorCreatorV23 editorCreator,
	                                  AutoLoginAfterSignUpProcessorV23 autoLogin, ImageAccessService imageAccessService,
	                                  NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.cfg = cfg;
		this.idpLoginController = idpLoginController;
		this.editorCreator = editorCreator;
		this.autoLoginProcessor = autoLogin;
		this.notificationPresenter = notificationPresenter;

		main = new VerticalLayout();
		main.addClassName("u-standalone-public-form");
		main.setWidthFull();
		getContent().add(main);
	}

	@Override
	public String getPageTitle()
	{
		return Optional.ofNullable(form.getPageTitle())
				.map(title -> title.getValue(msg))
				.orElse("");
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event)
	{
		form = event.getRouteParameters().get(FORM_PARAM)
				.map(this::getForm)
				.orElse(null);

		String pageTitle = Optional.ofNullable(form)
						.map(BaseForm::getPageTitle)
						.map(x -> x.getValue(msg))
						.orElse(null);
		postFillHandler = new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), true);

		registrationCode = event.getLocation().getQueryParameters()
				.getParameters()
				.getOrDefault(REG_CODE_PARAM, List.of())
				.stream().findFirst().orElse(null);

		enter(TriggeringMode.manualStandalone, null, null, null);
	}

	public void enter(TriggeringMode mode, Runnable customCancelHandler, Runnable completedRegistrationHandler,
			Runnable gotoSignInRedirector)
	{
		this.customCancelHandler = customCancelHandler;
		this.completedRegistrationHandler = completedRegistrationHandler;
		this.gotoSignInRedirector = gotoSignInRedirector;
		selectInitialView(mode);
	}
	
	private void selectInitialView(TriggeringMode mode) 
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		PostAuthenticationDecissionWithContext postAuthnStepDecision = (PostAuthenticationDecissionWithContext) session
				.getAttribute(RemoteRedirectedAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE);
		if (postAuthnStepDecision != null)
		{
			session.removeAttribute(RemoteRedirectedAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE);
			if (!postAuthnStepDecision.triggeringContext.isRegistrationTriggered())
				log.error("Got to standalone registration view with not-registration triggered "
						+ "remote authn results, {}", postAuthnStepDecision.triggeringContext);
			processRemoteAuthnResult(postAuthnStepDecision, mode);
		} else
		{
			showFirstStage(RemotelyAuthenticatedPrincipal.getLocalContext(), mode);
		}
	}

	private void processRemoteAuthnResult(PostAuthenticationDecissionWithContext postAuthnStepDecisionWithContext, 
			TriggeringMode mode)
	{
		log.debug("Remote authentication result found in session, triggering its processing");
		PostAuthenticationStepDecision postAuthnStepDecision = postAuthnStepDecisionWithContext.decision;
		switch (postAuthnStepDecision.getDecision())
		{
		case COMPLETED:
			onUserExists();
			return;
		case ERROR:
			onAuthnError(postAuthnStepDecision.getErrorDetail().error.resovle(msg), mode);
			return;
		case GO_TO_2ND_FACTOR:
			throw new IllegalStateException("2nd factor authN makes no sense upon registration");
		case UNKNOWN_REMOTE_USER:
			showSecondStage(postAuthnStepDecision.getUnknownRemoteUserDetail().unknownRemotePrincipal.remotePrincipal, 
					TriggeringMode.afterRemoteLoginFromRegistrationForm, false,
					postAuthnStepDecisionWithContext.triggeringContext.invitationCode,
					postAuthnStepDecisionWithContext.triggeringContext.authenticationOptionKey);
			return;
		default:
			throw new IllegalStateException("Unsupported post-authn decission for registration view: " 
					+ postAuthnStepDecision.getDecision());
		}
	}
	
	
	private void showFirstStage(RemotelyAuthenticatedPrincipal context, TriggeringMode mode)
	{
		main.removeAll();
		
		editorCreator.init(form, true, context, null, null);
		editorCreator.createFirstStage(new EditorCreatedCallback(mode), this::onLocalSignupClickHandler);
	}

	private void showSecondStage(RemotelyAuthenticatedPrincipal context, TriggeringMode mode, 
			boolean withCredentials, String presetRegistrationCode, AuthenticationOptionKey authnOptionKey)
	{
		main.removeAll();

		editorCreator.init(form, true, context, presetRegistrationCode, authnOptionKey);
		editorCreator.createSecondStage(new EditorCreatedCallback(mode), withCredentials);
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
		header = new SignUpTopHeaderComponent(cfg, msg, getGoToSignInRedirector(editor));
		main.add(header);

		main.add(editor);
		editor.setWidthFull();
		main.setAlignItems(FlexComponent.Alignment.CENTER);
		
		Button okButton = null;
		Button cancelButton = null;
		if (editor.isSubmissionPossible())
		{
			okButton = createOKButton(editor, mode);
			okButton.setWidthFull();
			
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
			formButtons.add(okButton);
			formButtons.setMargin(false);
			formButtons.getStyle().set("margin-left", "2.5em");
			main.add(formButtons);
		} else
		{
			formButtons = null;
		}
		
		if (cancelButton != null)
		{
			main.add(cancelButton);
		}
	}
	
	private Button createOKButton(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		return new Button(
				msg.getMessage("RegistrationRequestEditorDialog.submitRequest"),
				event -> onSubmit(editor, mode));
	}

	private Button createCancelButton()
	{
		return new Button(msg.getMessage("cancel"), event -> onCancel());
	}
	
	private Optional<Runnable> getGoToSignInRedirector(RegistrationRequestEditor editor)
	{
		if (!form.isShowSignInLink() || editor.getStage() != RegistrationRequestEditor.Stage.FIRST)
			return Optional.empty();

		if (gotoSignInRedirector != null)
			return Optional.of(gotoSignInRedirector);
		
		if (Strings.isEmpty(form.getSignInLink()))
			return Optional.empty();
		
		Runnable signinRedirector = () -> 
		{
			if (completedRegistrationHandler != null)
				completedRegistrationHandler.run();
			UI.getCurrent().getPage().open(form.getSignInLink(), null);
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
	
	private void onUserExists()
	{
		log.debug("External authentication resulted in existing user, aborting registration");
		WorkflowFinalizationConfiguration finalScreenConfig = 
				postFillHandler.getFinalRegistrationConfigurationOnError(TriggeringState.PRESET_USER_EXISTS);
		gotoFinalStep(finalScreenConfig);
	}

	private void onAuthnError(String authenticatorError, TriggeringMode mode)
	{
		log.info("External authentication failed, aborting: {}", authenticatorError);
		NotificationPopup.showError(authenticatorError, "");
		showFirstStage(RemotelyAuthenticatedPrincipal.getLocalContext(), mode);
	}
	
	private void onLocalSignupClickHandler(String presetInvitationCode)
	{
		showSecondStage(RemotelyAuthenticatedPrincipal.getLocalContext(), TriggeringMode.manualStandalone,
				true, presetInvitationCode, null);
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
			handleFormSubmissionError(e, msg, editor);
			return;
		} catch (Exception e)
		{
			log.warn("Registration request submision failed", e);
			WorkflowFinalizationConfiguration finalScreenConfig = 
					postFillHandler.getFinalRegistrationConfigurationOnError(TriggeringState.GENERAL_ERROR);
			gotoFinalStep(finalScreenConfig);
		}
	}

	public void handleFormSubmissionError(Exception e, MessageSource msg, RegistrationRequestEditor editor)
	{
		if (e instanceof IllegalFormContentsException)
		{
			editor.markErrorsFromException((IllegalFormContentsException) e);
			if (e instanceof IllegalFormContentsException.OccupiedIdentityUsedInRequest)
			{
				String identity = ((IllegalFormContentsException.OccupiedIdentityUsedInRequest) e).occupiedIdentity.getValue();
				notificationPresenter.showError(msg.getMessage("FormRequest.occupiedIdentity", identity), "");
			} else
			{
				notificationPresenter.showError(msg.getMessage("Generic.formError"), e.getMessage());
			}
		} else
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
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
			redirect(UI.getCurrent().getPage(), config.redirectURL, idpLoginController);
		else
			showFinalScreen(config);
	}
	
	private void showFinalScreen(WorkflowFinalizationConfiguration config)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		wrapper.setSizeFull();

		WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, new Image(config.logoURL, ""));
		wrapper.add(finalScreen);
		finalScreen.setFontSize("2em");
		wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
		getContent().removeAll();
		getContent().add(wrapper);
	}

	private static void redirect(Page page, String redirectUrl, IdPLoginController loginController)
	{
		loginController.breakLogin();
		page.open(redirectUrl, null);
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

	private RegistrationForm getForm(String name)
	{
		try
		{
			List<RegistrationForm> forms = regMan.getForms();
			for (RegistrationForm regForm: forms)
				if (regForm.isPubliclyAvailable() && regForm.getName().equals(name))
					return regForm;
		} catch (EngineException e)
		{
			log.error("Can't load registration forms", e);
		}
		return null;
	}

	private class EditorCreatedCallback implements RequestEditorCreatorV23.RequestEditorCreatedCallback
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
}