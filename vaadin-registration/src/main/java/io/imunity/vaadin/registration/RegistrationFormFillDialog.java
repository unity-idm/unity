/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.registration;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.forms.components.WorkflowCompletedComponent;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;

/**
 * Dialog allowing to fill a registration form. It takes an editor component as argument.
 * Dialog uses 2 buttons: submit request, cancel.
 */
public class RegistrationFormFillDialog extends Dialog
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RegistrationFormFillDialog.class);
	private final MessageSource msg;
	private final RegistrationRequestEditor editor;
	private final VaadinLogoImageLoader imageAccessService;
	private final NotificationPresenter notificationPresenter;
	private final Callback callback;
	private boolean onFinalScreen;
	private final boolean withSimplifiedFinalization;
	
	public RegistrationFormFillDialog(MessageSource msg, VaadinLogoImageLoader imageAccessService, String caption,
	                                  RegistrationRequestEditor editor, Callback callback, IdPLoginController idpLoginController,
									  NotificationPresenter notificationPresenter,
	                                  boolean withSimplifiedFinalization)
	{
		this.msg = msg;
		this.editor = editor;
		this.callback = callback;
		this.withSimplifiedFinalization = withSimplifiedFinalization;
		this.imageAccessService = imageAccessService;
		this.notificationPresenter = notificationPresenter;
		setHeaderTitle(caption);
		init();
	}

	private void init()
	{
		setWidth("80%");
		Button cancelButton = new Button(msg.getMessage("cancel"), e ->
		{
			callback.cancelled();
			close();
		});
		cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		cancelButton.getStyle().set("margin-right", "auto");
		getFooter().add(cancelButton);
		Button submitButton = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"), e ->
		{
			if (onFinalScreen)
				close();
			else
				submitRequest();
		});
		submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		getFooter().add(submitButton);

		VerticalLayout vl = new VerticalLayout();
		vl.setMargin(false);
		vl.setPadding(false);
		vl.add(editor);
		vl.setAlignItems(FlexComponent.Alignment.CENTER);
		add(vl);
	}
	
	private void gotoFinalScreen(WorkflowFinalizationConfiguration config)
	{
		log.debug("Registration is finalized, status: {}", config);
		onFinalScreen = true;
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		wrapper.setSizeFull();
		setSizeFull();
		add(wrapper);

		WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, imageAccessService.loadImageFromUri(config.logoURL).orElse(new Image()));
		wrapper.add(finalScreen);
		wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
	}
	
	private void submitRequest()
	{
		RegistrationRequest request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
		if (request == null)
			return;
		try
		{
			WorkflowFinalizationConfiguration config = callback.newRequest(request);
			if (withSimplifiedFinalization)
			{
				closeDialogAndShowInfo(config.mainInformation);
				if (config.isAutoLoginAfterSignUp())
					UI.getCurrent().getPage().reload();
			} else
			{
				gotoFinalScreen(config);
			}
		} catch (Exception e) 
		{
			SubmissionErrorHandler.handleFormSubmissionError(e, msg, editor, notificationPresenter);
		}
	}
	
	private void closeDialogAndShowInfo(String mainInformation)
	{
		close();
		notificationPresenter.showSuccess(mainInformation, "");
	}

	public interface Callback
	{
		WorkflowFinalizationConfiguration newRequest(RegistrationRequest request) throws WrongArgumentException;
		void cancelled();
	}
}
