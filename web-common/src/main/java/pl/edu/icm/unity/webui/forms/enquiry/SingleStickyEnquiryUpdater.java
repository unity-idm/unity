/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.NotificationTray;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.finalization.WorkflowCompletedComponent;

/**
 * Component allows update requests for sticky enquiry. If request already
 * exists then remove last request button is visible and editor is hide. After
 * remove last request editor is shown and we can fill enquiry and submit new
 * one.
 * 
 * @author P.Piernik
 *
 */
public class SingleStickyEnquiryUpdater extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SingleStickyEnquiryUpdater.class);

	private UnityMessageSource msg;
	private EnquiryResponseEditorController controller;
	private List<String> forms;
	private boolean showNotAppFormsInfo;

	public SingleStickyEnquiryUpdater(UnityMessageSource msg, EnquiryResponseEditorController controller,
			List<String> forms, boolean showNotAppFormsInfo) throws WrongArgumentException
	{
		this.msg = msg;
		this.controller = controller;
		this.forms = forms;
		this.showNotAppFormsInfo = showNotAppFormsInfo;
		reload();
	}

	private Component getRemoveLastComponent(EnquiryForm form)
	{

		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		wrapper.setSpacing(true);

		Label formName = new Label(form.getDisplayedName().getValue(msg));
		formName.addStyleName(Styles.vLabelH1.toString());
		formName.addStyleName("u-reg-title");
		wrapper.addComponent(formName);

		Label info = new Label(
				msg.getMessage("SingleStickyEnquiryUpdater.overwriteRequestInfo"));
		info.setContentMode(ContentMode.HTML);
		
		Button removeLast = new Button(msg.getMessage("SingleStickyEnquiryUpdater.removeLastRequest"));
		removeLast.addStyleName(Styles.vButtonPrimary.toString());
		removeLast.addClickListener(event -> {
			try
			{
				controller.removePendingRequest(form.getName());
			} catch (EngineException e)
			{
				log.error("Can not remove pending request for enquiry form " + form.getName());
				NotificationPopup.showError(msg,
						msg.getMessage("SingleStickyEnquiryUpdater.cannotRemovePendingRequest",
								form.getName()),
						e);
			}
			reload();
		});

		wrapper.addComponents(formName ,info, removeLast);
		wrapper.setComponentAlignment(formName, Alignment.MIDDLE_CENTER);
		wrapper.setComponentAlignment(info, Alignment.MIDDLE_CENTER);
		wrapper.setComponentAlignment(removeLast, Alignment.MIDDLE_CENTER);
		return wrapper;
	}

	private Component getEditorComponent(EnquiryForm form)
	{
		VerticalLayout editorWrapper = new VerticalLayout();
		editorWrapper.setMargin(false);

		EnquiryResponseEditor editor;
		try
		{

			editor = controller.getEditorInstance(form, RemotelyAuthenticatedContext.getLocalContext());
		} catch (Exception e)
		{
			log.error("Can not get editor for enquiry form " + form.getName());
			NotificationPopup.showError(msg, msg.getMessage(
					"SingleStickyEnquiryUpdater.cannotSubmitRequest", form.getName()), e);
			return editorWrapper;
		}
		if (!editor.isUserInteractionRequired())
		{
			WorkflowFinalizationConfiguration config = controller
					.getFinalizationHandler(form)
					.getFinalRegistrationConfigurationNonSubmit(false, null,
							TriggeringState.NOT_APPLICABLE_ENQUIRY);
			
			
			WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, url -> {});
			editorWrapper.addComponent(finalScreen);
			editorWrapper.setComponentAlignment(finalScreen, Alignment.MIDDLE_CENTER);
			return editorWrapper;	
		}
		
		Button ok = new Button(msg.getMessage("SingleStickyEnquiryUpdater.submitRequest"));
		ok.addStyleName(Styles.vButtonPrimary.toString());
		ok.addClickListener(event -> {
			EnquiryResponse request = editor.getRequestWithStandardErrorHandling(false).orElse(null);
			if (request == null)
				return;

			try
			{
				WorkflowFinalizationConfiguration workflowConfig = controller.submitted(request, form,
						TriggeringMode.manualStandalone);
				showNotificationAfterSubmit(workflowConfig);
			} catch (Exception e)
			{
				log.error("Can not sumbit new request for form " + form.getName(), e);
				NotificationPopup.showError(msg,
						msg.getMessage("SingleStickyEnquiryUpdater.cannotSubmitRequest"), e);
			}

			reload();

		});

		editorWrapper.addComponents(editor, ok);
		editorWrapper.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);
		editorWrapper.setComponentAlignment(ok, Alignment.MIDDLE_CENTER);
		return editorWrapper;
	}

	private void showNotificationAfterSubmit(WorkflowFinalizationConfiguration workflowConfig)
	{
		if (workflowConfig.success)
		{
			NotificationTray.showSuccess(workflowConfig.mainInformation, workflowConfig.extraInformation);		
		}else
		{
			NotificationTray.showError(workflowConfig.mainInformation, workflowConfig.extraInformation);		
		}
	}

	public void reload()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(false);
		main.setMargin(false);
		setCompositionRoot(main);
		
		EnquiryForm form = null;
		for (String enquiryForm : forms)
		{
			if (controller.isStickyFormApplicable(enquiryForm))
			{
				form = controller.getForm(enquiryForm);
				break;
			}
		}
		if (form == null)
		{
			if (showNotAppFormsInfo)
			{
				main.addComponent(new Label(msg.getMessage("SingleStickyEnquiryUpdater.notApplicableForms")));
			}
		
			return;
		}
			

		boolean requestExist = true;
		try
		{
			requestExist = controller.checkIfRequestExists(form.getName());
		} catch (Exception e)
		{
			log.error("Can not check if pending request exists for form " + form.getName(), e);
			NotificationPopup.showError(msg, msg.getMessage(
					"SingleStickyEnquiryUpdater.cannotCheckPendingRequest", form.getName()), e);
		}

		if (requestExist)
		{
			main.addComponent(getRemoveLastComponent(form));
		}

		else
		{
			main.addComponent(getEditorComponent(form));

		}
	}
	
	public boolean isFormsAreApplicable() 
	{
		for (String enquiryForm : forms)
		{
			if (controller.isStickyFormApplicable(enquiryForm))
			{
				return true;
			}
		}
		return false;
	}
}
