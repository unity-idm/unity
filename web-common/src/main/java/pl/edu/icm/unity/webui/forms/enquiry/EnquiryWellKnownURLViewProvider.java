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

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.finalization.WorkflowCompletedComponent;
import pl.edu.icm.unity.webui.forms.enquiry.StandaloneEnquiryView.Callback;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormDialogProvider;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier;
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
	private UnityMessageSource msg;
	@Autowired
	private StandardWebAuthenticationProcessor authnProcessor;
	
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
			return new NotApplicableView(form);
		}

		EnquiryResponseEditor editor;
		try
		{
			editor = editorController.getEditorInstance(form,
					RemotelyAuthenticatedContext.getLocalContext());
		} catch (Exception e)
		{
			log.error("Can't load enquiry editor", e);
			return null;
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
				return onSubmission(form, editor);
			}

			@Override
			public WorkflowFinalizationConfiguration cancelled()
			{
				return editorController.cancelled(form, TriggeringMode.manualStandalone, true);
			}
		};

		return overwriteSticky
				? new StandaloneStickyEnquiryView(editor, authnProcessor, msg, callback,
						() -> removePendingRequestSafe(form.getName()) )
				: new StandaloneEnquiryView(editor, authnProcessor, msg, callback);
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

	private WorkflowFinalizationConfiguration onSubmission(EnquiryForm form, EnquiryResponseEditor editor)
	{
		EnquiryResponse request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
		if (request == null)
			return null;
		try
		{
			return editorController.submitted(request, form, TriggeringMode.manualStandalone);
		} catch (WrongArgumentException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			if (e instanceof IllegalFormContentsException)
				editor.markErrorsFromException((IllegalFormContentsException) e);
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
	
	private class NotApplicableView extends CustomComponent implements View
	{
		private EnquiryForm form;
		
		public NotApplicableView(EnquiryForm form)
		{
			this.form = form;
		}
		
		@Override
		public void enter(ViewChangeEvent event)
		{
			WorkflowFinalizationConfiguration config = editorController.getFinalizationHandler(form)
					.getFinalRegistrationConfigurationNonSubmit(false, null,
							TriggeringState.NOT_APPLICABLE_ENQUIRY);
			WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, url -> {});
			com.vaadin.ui.Component wrapper = finalScreen.getWrappedForFullSizeComponent();
			setSizeFull();
			setCompositionRoot(wrapper);
		}
	}
}
