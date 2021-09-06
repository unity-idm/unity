/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.finalization.WorkflowCompletedComponent;
import pl.edu.icm.unity.webui.forms.FormsUIHelper;
import pl.edu.icm.unity.webui.forms.InvitationResolver;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.RegCodeException;
import pl.edu.icm.unity.webui.forms.enquiry.StandaloneEnquiryView.Callback;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormDialogProvider;
import pl.edu.icm.unity.webui.wellknownurl.SecuredViewProvider;

/**
 * Standalone view presenting enquiry form.
 * 
 * @author K. Benedyczak
 */
@Component
public class EnquiryWellKnownURLViewProvider implements SecuredViewProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			EnquiryWellKnownURLViewProvider.class);
	@Autowired
	private EnquiryResponseEditorController editorController;
	@Autowired
	private MessageSource msg;
	@Autowired
	private StandardWebLogoutHandler authnProcessor;
	@Autowired
	private ImageAccessService imageAccessService;
	@Autowired
	private InvitationResolver invitationResolver;
	
	/**
	 * @implNote: due to changes in the enquiry links, below format was kept for
	 *            backwards compatibility reasons.
	 */
	@Deprecated
	private static final String ENQUIRY_FRAGMENT_PREFIX = "enquiry-";
	
	@Override
	public String getViewName(String viewAndParameters)
	{
		String formName = getFormName(viewAndParameters);
		if (formName == null)
			return null;
		
		EnquiryForm enquiry = editorController.getForm(formName);
		if (enquiry == null)
			return null;
	
		return viewAndParameters;
	}
	
	@Override
	public View getView(String viewName)
	{
		String formName = getFormName(viewName);
		EnquiryForm form = editorController.getForm(formName);
		if (!editorController.isFormApplicable(formName) && !editorController.isStickyFormApplicable(formName))
		{
			log.debug("Enquiry form {} is not applicable", formName);
			return new ErrorView(form, TriggeringState.NOT_APPLICABLE_ENQUIRY);
		}

		PrefilledSet prefilledSet = null;	
		String registrationCode = RegistrationFormDialogProvider.getCodeFromURL();
		
		if (registrationCode != null)
		{	try
			{
				prefilledSet = getPrefilledFromInvitation(registrationCode, form);
			} catch (RegCodeException e)
			{
				log.error("Can not get invitation", e);
				return new ErrorView(form, e.cause.getTriggerState());
			}
		}
			
		EnquiryResponseEditorWithRegistrationCodePreserve editorWithCode;
		try
		{
			editorWithCode = new EnquiryResponseEditorWithRegistrationCodePreserve(
					editorController.getEditorInstanceForAuthenticatedUser(form, prefilledSet,
							RemotelyAuthenticatedPrincipal.getLocalContext()),
					registrationCode);

		} catch (Exception e)
		{
			log.error("Can't load enquiry editor", e);
			return new ErrorView(form, TriggeringState.GENERAL_ERROR);
		}

		boolean overwriteSticky = false;
		if (form.getType().equals(EnquiryType.STICKY))
		{
			try
			{
				overwriteSticky = editorController.checkIfRequestExists(form.getName());
			} catch (Exception e)
			{
				log.warn("Can't check if enquiry request exists", e);
			}
		}

		Callback callback = new Callback()
		{
			@Override
			public WorkflowFinalizationConfiguration submitted()
			{
				return onSubmission(form, editorWithCode);
			}

			@Override
			public WorkflowFinalizationConfiguration cancelled()
			{
				return editorController.cancelled(form, TriggeringMode.manualStandalone, true);
			}
		};

		return overwriteSticky
				? new StandaloneStickyEnquiryView(editorWithCode.editor, authnProcessor, imageAccessService, msg, callback,
						() -> removePendingRequestSafe(form.getName()) )
				: new StandaloneEnquiryView(editorWithCode.editor, authnProcessor, imageAccessService,  msg, callback);
	}
	
	private PrefilledSet getPrefilledFromInvitation(String registrationCode, EnquiryForm form) throws RegCodeException
	{
		EnquiryInvitationParam invitation = invitationResolver.getInvitationByCode(registrationCode, form)
				.getAsEnquiryInvitationParam();
		if (invitation != null)
		{
			FormPrefill formPrefill = invitation.getFormPrefill();
			return new PrefilledSet(formPrefill.getIdentities(), formPrefill.getGroupSelections(),
					formPrefill.getAttributes(), formPrefill.getAllowedGroups());
		}
		return null;
	}
	
	private void removePendingRequestSafe(String formName)
	{
		try
		{
			editorController.removePendingRequest(formName);
		} catch (Exception e)
		{
			// ok, we remove request before submit
			log.warn("Can not remove pending request for form " + formName);
		}
	}

	private WorkflowFinalizationConfiguration onSubmission(EnquiryForm form, EnquiryResponseEditorWithRegistrationCodePreserve editor)
	{
		EnquiryResponse request = editor.getRequest();
		if (request == null)
			return null;
		try
		{
			return editorController.submitted(request, form, TriggeringMode.manualStandalone);
		} catch (WrongArgumentException e)
		{
			FormsUIHelper.handleFormSubmissionError(e, msg, editor.editor);
			return null;
		}
	}

	
	private String getFormName(String viewAndParameters)
	{
		if (PublicRegistrationURLSupport.ENQUIRY_VIEW.equals(viewAndParameters))
			return RegistrationFormDialogProvider.getFormFromURL();
		
		if (viewAndParameters.startsWith(ENQUIRY_FRAGMENT_PREFIX))
			return viewAndParameters.substring(ENQUIRY_FRAGMENT_PREFIX.length());
		
		return null;
	}

	@Override
	public void setEndpointConfiguration(Properties configuration)
	{
	}

	@Override
	public void setSandboxNotifier(SandboxAuthnNotifier sandboxNotifier,
			String sandboxUrlForAssociation)
	{
	}
	
	private class ErrorView extends CustomComponent implements View
	{
		private final EnquiryForm form;
		private final TriggeringState state;
		
		public ErrorView(EnquiryForm form, TriggeringState state)
		{
			this.form = form;
			this.state = state;
		}
		
		@Override
		public void enter(ViewChangeEvent event)
		{
			WorkflowFinalizationConfiguration config = editorController.getFinalizationHandler(form)
					.getFinalRegistrationConfigurationNonSubmit(false, null,
							state);
			WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, (p, url) -> {}, 
					imageAccessService);
			com.vaadin.ui.Component wrapper = finalScreen.getWrappedForFullSizeComponent();
			setSizeFull();
			setCompositionRoot(wrapper);
		}
	}
	
	private static class EnquiryResponseEditorWithRegistrationCodePreserve
	{
		private final EnquiryResponseEditor editor;
		private final String code;
		
		private EnquiryResponseEditorWithRegistrationCodePreserve(EnquiryResponseEditor editor, String code)
		{
			this.editor = editor;
			this.code = code;
		}
		
		EnquiryResponse getRequest()
		{
			EnquiryResponse request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
			if (request != null)
				request.setRegistrationCode(code);
			return request;
		}	
	}
}
