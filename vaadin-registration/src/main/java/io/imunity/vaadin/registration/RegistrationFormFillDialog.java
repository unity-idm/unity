/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.registration;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.forms.components.WorkflowCompletedComponent;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.RegistrationRequest;

/**
 * Dialog allowing to fill a registration form. It takes an editor component as argument.
 * Dialog uses 2 buttons: submit request, cancel.
 */
public class RegistrationFormFillDialog extends ConfirmDialog
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RegistrationFormFillDialog.class);
	private MessageSource msg;
	private RegistrationRequestEditor editor;
	private final VaadinLogoImageLoader imageAccessService;
	private final NotificationPresenter notificationPresenter;
	private final Callback callback;
	private boolean onFinalScreen;
	private IdPLoginController idpLoginController;
	private final boolean withSimplifiedFinalization;
	
	public RegistrationFormFillDialog(MessageSource msg, VaadinLogoImageLoader imageAccessService, String caption,
	                                  RegistrationRequestEditor editor, Callback callback, IdPLoginController idpLoginController,
									  NotificationPresenter notificationPresenter,
	                                  boolean withSimplifiedFinalization)
	{
		this.msg = msg;
		this.editor = editor;
		this.callback = callback;
		this.idpLoginController = idpLoginController;
		this.withSimplifiedFinalization = withSimplifiedFinalization;
		this.imageAccessService = imageAccessService;
		this.notificationPresenter = notificationPresenter;
		setText(caption);
		init();
	}

	private void init()
	{
		setWidth("80%");
		setCancelable(true);
		setCancelButton(msg.getMessage("cancel"), e -> callback.cancelled());
		setConfirmButton(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"), e ->
		{
			if (onFinalScreen)
				close();
			else
				submitRequest();
		});

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
		setCancelable(false);
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
	
//	private static void redirect(Page page, String redirectUrl, IdPLoginController loginController)
//	{
//		loginController.breakLogin();
//		page.open(redirectUrl, null);
//	}
	
	private void submitRequest()
	{
		RegistrationRequest request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
		if (request == null)
		{
//			open();
			return;
		}
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
		notificationPresenter.showWarning(mainInformation, mainInformation);
	}

	public interface Callback
	{
		WorkflowFinalizationConfiguration newRequest(RegistrationRequest request) throws WrongArgumentException;
		void cancelled();
	}
}
