/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.enquiry;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;

import java.util.List;
import java.util.Optional;

/**
 * Component allows for editing and also updating requests for sticky enquiry. 
 * If request already exists then remove last request button is visible and editor is hidden. After
 * removal of the last request, an editor is shown and enquiry can be submitted again.
 */
class StickyEnquiryUpdatableComponent extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StickyEnquiryUpdatableComponent.class);

	private final MessageSource msg;
	private final EnquiryResponseEditorController controller;
	private final NotificationPresenter notificationPresenter;
	private final List<String> forms;

	StickyEnquiryUpdatableComponent(MessageSource msg, EnquiryResponseEditorController controller,
									NotificationPresenter notificationPresenter,
									List<String> forms)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
		this.forms = forms;
		setMargin(false);
		setPadding(false);
		reload();
	}

	private Component removeLastComponent(EnquiryForm form)
	{

		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		wrapper.setPadding(false);

		H3 formName = new H3(form.getDisplayedName().getValue(msg));
		formName.addClassName("u-reg-title");
		wrapper.add(formName);

		Label info = new Label(
				msg.getMessage("SingleStickyEnquiryUpdater.overwriteRequestInfo"));

		Button removeLast = new Button(msg.getMessage("SingleStickyEnquiryUpdater.removeLastRequest"));
		removeLast.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		removeLast.addClickListener(event -> {
			try
			{
				controller.removePendingRequest(form.getName());
			} catch (EngineException e)
			{
				log.error("Can not remove pending request for enquiry form " + form.getName(), e);
				notificationPresenter.showError(
						msg.getMessage("SingleStickyEnquiryUpdater.cannotRemovePendingRequest", form.getName()),
						e.getMessage());
			}
			reload();
		});

		wrapper.add(formName, info, removeLast);
		wrapper.setAlignItems(Alignment.CENTER);
		return wrapper;
	}

	private Component getEditorComponent(EnquiryForm form)
	{
		VerticalLayout editorWrapper = new VerticalLayout();
		editorWrapper.setMargin(false);
		editorWrapper.setPadding(false);

		EnquiryResponseEditor editor;
		try
		{

			editor = controller.getEditorInstanceForAuthenticatedUser(form, RemotelyAuthenticatedPrincipal.getLocalContext());
		} catch (Exception e)
		{
			log.error("Can not get editor for enquiry form " + form.getName(), e);
			notificationPresenter.showError(msg.getMessage(
					"SingleStickyEnquiryUpdater.cannotSubmitRequest", form.getName()), e.getMessage());
			return editorWrapper;
		}
		
		Button ok = new Button(msg.getMessage("SingleStickyEnquiryUpdater.submitRequest"));
		ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		ok.addClickListener(event -> {
			EnquiryResponse request = editor.getRequestWithStandardErrorHandling(false).orElse(null);
			if (request == null)
				return;
			try
			{
				WorkflowFinalizationConfiguration workflowConfig = controller.submitted(request, form,
						TriggeringMode.manualStandalone, Optional.empty());
				showNotificationAfterSubmit(workflowConfig);
			} catch (Exception e)
			{
				log.error("Can not sumbit new request for form " + form.getName(), e);
				notificationPresenter.showError(
						msg.getMessage("SingleStickyEnquiryUpdater.cannotSubmitRequest"), e.getMessage());
			}
			reload();
		});

		editorWrapper.add(editor, ok);
		editorWrapper.setAlignItems(Alignment.CENTER);
		editorWrapper.getStyle().set("gap", "0");

		return editorWrapper;
	}

	private void showNotificationAfterSubmit(WorkflowFinalizationConfiguration workflowConfig)
	{
		if (workflowConfig.success)
			notificationPresenter.showSuccess(workflowConfig.mainInformation, workflowConfig.extraInformation);
		else
			notificationPresenter.showError(workflowConfig.mainInformation, workflowConfig.extraInformation);

	}

	public void reload()
	{
		removeAll();
		EnquiryForm form = null;
		for (String enquiryForm : forms)
		{
			if (controller.isStickyFormApplicableForCurrentUser(enquiryForm))
			{
				form = controller.getForm(enquiryForm);
				break;
			}
		}
		if (form == null)
		{
			add(new Label(msg.getMessage("SingleStickyEnquiryUpdater.notApplicableForms")));
			return;
		}
			

		boolean requestExist = true;
		try
		{
			requestExist = controller.checkIfRequestExistsForLoggedUser(form.getName());
		} catch (Exception e)
		{
			log.error("Can not check if pending request exists for form " + form.getName(), e);
			notificationPresenter.showError(msg.getMessage(
					"SingleStickyEnquiryUpdater.cannotCheckPendingRequest", form.getName()), e.getMessage());
		}

		if (requestExist)
			add(removeLastComponent(form));
		else
			add(getEditorComponent(form));
	}
}
