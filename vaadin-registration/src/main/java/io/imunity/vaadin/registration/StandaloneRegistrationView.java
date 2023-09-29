/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.registration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.forms.components.WorkflowCompletedComponent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IdentityExistsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.*;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter.PostAuthenticationDecissionWithContext;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement.REGISTRATION_PATH;

@Route(value = REGISTRATION_PATH + ":" + StandaloneRegistrationView.FORM_PARAM)
class StandaloneRegistrationView extends Composite<Div> implements HasDynamicTitle, BeforeEnterObserver
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StandaloneRegistrationView.class);

	public static final String FORM_PARAM = "form";
	public static final String REG_CODE_PARAM = "regcode";
	private final RegistrationsManagement regMan;
	private final MessageSource msg;
	private final UnityServerConfiguration cfg;
	private final IdPLoginController idpLoginController;
	private final RequestEditorCreator editorCreator;
	private final AutoLoginAfterSignUpProcessor autoLoginProcessor;
	private final NotificationPresenter notificationPresenter;
	private final VaadinLogoImageLoader logoImageLoader;
	private RegistrationForm form;
	private String registrationCode;
	private PostFillingHandler postFillHandler;
	private final VerticalLayout main;
	private Runnable customCancelHandler;
	private Runnable completedRegistrationHandler;
	private Runnable gotoSignInRedirector;
	private Map<String, List<String>> parameters;

	@Autowired
	StandaloneRegistrationView(MessageSource msg,
	                                  @Qualifier("insecure") RegistrationsManagement regMan,
	                                  UnityServerConfiguration cfg,
	                                  IdPLoginController idpLoginController,
	                                  RequestEditorCreator editorCreator,
	                                  AutoLoginAfterSignUpProcessor autoLogin, VaadinLogoImageLoader logoImageLoader,
	                                  NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.cfg = cfg;
		this.idpLoginController = idpLoginController;
		this.editorCreator = editorCreator;
		this.autoLoginProcessor = autoLogin;
		this.logoImageLoader = logoImageLoader;
		this.notificationPresenter = notificationPresenter;

		main = new VerticalLayout();
		main.addClassName("u-standalone-public-form");
		main.setWidthFull();
		main.getStyle().set("gap", "0");
		getContent().add(main);
	}

	@Override
	public String getPageTitle()
	{
		return Optional.ofNullable(form)
				.flatMap(form -> Optional.ofNullable(form.getPageTitle()))
				.map(title -> title.getValue(msg))
				.orElse("");
	}

	void init(RegistrationForm form, TriggeringMode mode, Runnable customCancelHandler, Runnable completedRegistrationHandler,
	          Runnable gotoSignInRedirector)
	{
		this.form = form;
		String pageTitle = form.getPageTitle() == null ? null : form.getPageTitle().getValue(msg);
		this.postFillHandler = new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), true);
		enter(mode, customCancelHandler, completedRegistrationHandler, gotoSignInRedirector);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event)
	{
		form = event.getRouteParameters().get(FORM_PARAM)
				.map(this::getForm)
				.orElse(null);
		parameters = event.getLocation().getQueryParameters().getParameters();
		if(form == null)
		{
			notificationPresenter.showError(msg.getMessage("RegistrationErrorName.title"), msg.getMessage("RegistrationErrorName.description"));
			return;
		}

		String pageTitle = form.getPageTitle().getValue(msg);
		postFillHandler = new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), true);

		registrationCode = event.getLocation().getQueryParameters()
				.getParameters()
				.getOrDefault(REG_CODE_PARAM, List.of())
				.stream().findFirst().orElse(null);

		enter(TriggeringMode.manualStandalone, null, null, null);
	}

	private void enter(TriggeringMode mode, Runnable customCancelHandler, Runnable completedRegistrationHandler,
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
			case COMPLETED -> onUserExists();
			case ERROR -> onAuthnError(postAuthnStepDecision.getErrorDetail().error.resovle(msg), mode);
			case GO_TO_2ND_FACTOR ->
					throw new IllegalStateException("2nd factor authN makes no sense upon registration");
			case UNKNOWN_REMOTE_USER -> showSecondStage(postAuthnStepDecision.getUnknownRemoteUserDetail().unknownRemotePrincipal.remotePrincipal,
					TriggeringMode.afterRemoteLoginFromRegistrationForm, false,
					postAuthnStepDecisionWithContext.triggeringContext.invitationCode,
					postAuthnStepDecisionWithContext.triggeringContext.authenticationOptionKey);
			default -> throw new IllegalStateException("Unsupported post-authn decission for registration view: "
					+ postAuthnStepDecision.getDecision());
		}
	}
	
	
	private void showFirstStage(RemotelyAuthenticatedPrincipal context, TriggeringMode mode)
	{
		main.removeAll();
		
		editorCreator.init(form, true, context, registrationCode, null, parameters);
		editorCreator.createFirstStage(new EditorCreatedCallback(mode), this::onLocalSignupClickHandler);
	}

	private void showSecondStage(RemotelyAuthenticatedPrincipal context, TriggeringMode mode, 
			boolean withCredentials, String presetRegistrationCode, AuthenticationOptionKey authnOptionKey)
	{
		main.removeAll();

		editorCreator.init(form, true, context, presetRegistrationCode, authnOptionKey, parameters);
		editorCreator.createSecondStage(new EditorCreatedCallback(mode), withCredentials);
	}
	
	private void editorCreated(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		if (isAutoSubmitPossible(editor, mode))
		{
			onSubmit(editor, mode);
		} else
		{
			showEditorContent(editor, mode);
			editor.performAutomaticRemoteSignupIfNeeded();
		}
	}
	
	private void showEditorContent(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		SignUpTopHeaderComponent header = new SignUpTopHeaderComponent(cfg, msg, getGoToSignInRedirector(editor));
		main.add(header);

		main.add(editor);
		editor.setWidthFull();
		main.setAlignItems(FlexComponent.Alignment.CENTER);
		
		Button okButton = null;
		Component cancelButton = null;
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

		HorizontalLayout formButtons;
		if (okButton != null)
		{
			formButtons = new HorizontalLayout();
			formButtons.setWidth(editor.formWidth(), editor.formWidthUnit());
			formButtons.add(okButton);
			formButtons.setMargin(false);
			main.add(formButtons);
		}

		if (cancelButton != null)
		{
			main.add(cancelButton);
		}
	}
	
	private Button createOKButton(RegistrationRequestEditor editor, TriggeringMode mode)
	{
		Button button = new Button(
				msg.getMessage("RegistrationRequestEditorDialog.submitRequest"),
				event -> onSubmit(editor, mode));
		button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		if(form.getLayoutSettings().isCompactInputs())
			button.getStyle().set("margin-top", "-1.5em");
		return button;
	}

	private Component createCancelButton()
	{
		return new LinkButton(msg.getMessage("cancel"), event -> onCancel());
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
		return isStanaloneModeFromAuthNScreen()
				&& !postFillHandler.hasConfiguredFinalizationFor(TriggeringState.CANCELLED);
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
			SubmissionErrorHandler.handleFormSubmissionError(e, msg, editor, notificationPresenter);
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

		Image logo = logoImageLoader.loadImageFromUri(config.logoURL).orElse(null);
		WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, logo);
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
		name = URLDecoder.decode(name, StandardCharsets.UTF_8);
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

	private class EditorCreatedCallback implements RequestEditorCreator.RequestEditorCreatedCallback
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
