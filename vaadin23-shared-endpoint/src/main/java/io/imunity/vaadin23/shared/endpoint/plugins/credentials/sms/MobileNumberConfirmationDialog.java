/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint.plugins.credentials.sms;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.components.CaptchaComponent;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.confirmation.MobileNumberConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;

public class MobileNumberConfirmationDialog extends ConfirmDialog
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, MobileNumberConfirmationDialog.class);

	private MessageSource msg;
	private Callback callback;
	private TextField field;
	private CaptchaComponent captcha;
	private MobileNumberConfirmationManager mobileConfirmationMan;
	private NotificationPresenter notificationPresenter;
	private MobileNumberConfirmationConfiguration confirmationConfiguration;
	private SMSCode code;
	private String mobileToConfirm;
	private ConfirmationInfo confirmationInfo;
	private Component captchaComponent;
	private Component confirmCodeComponent;
	private boolean capchaVerified = false;
	private Label errorLabel;
	
	
	public MobileNumberConfirmationDialog(String mobileToConfirm, ConfirmationInfo confirmatioInfo,
	                                      MessageSource msg,
	                                      MobileNumberConfirmationManager mobileConfirmationMan,
	                                      MobileNumberConfirmationConfiguration confirmationConfiguration,
	                                      Callback callback, NotificationPresenter notificationPresenter)
	{
		setText(msg.getMessage("MobileNumberConfirmationDialog.caption"));
		this.msg = msg;
		this.callback = callback;
		this.mobileConfirmationMan = mobileConfirmationMan;
		this.confirmationConfiguration = confirmationConfiguration;
		this.mobileToConfirm = mobileToConfirm;
		this.confirmationInfo = confirmatioInfo;
		this.notificationPresenter = notificationPresenter;

		this.captchaComponent = getCapchaComponent();
		this.captchaComponent.setVisible(false);
		this.confirmCodeComponent = getConfirmCodeComponent();
		this.confirmCodeComponent.setVisible(false);
		setWidth("36em");
		setHeight("25em");
		addConfirmListener(event -> onConfirm());
		addRejectListener(event -> close());
		setCancelable(true);
		setConfirmText("OK");
		add(getContents());
	}

	
	
	private Component getCapchaComponent()
	{
		Label infoLabel = new Label(msg.getMessage("MobileNumberConfirmationDialog.capchaInfo"));
		infoLabel.setSizeFull();	
		captcha = new CaptchaComponent(msg, 4, false);		
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		wrapper.add(infoLabel, captcha.getAsComponent());
		return wrapper;
	}
	
	private Component getConfirmCodeComponent()
	{
		field = new TextField();
		field.setLabel(msg.getMessage("MobileNumberConfirmationDialog.code"));
		field.setRequiredIndicatorVisible(true);		
		Label infoLabel = new Label(msg.getMessage("MobileNumberConfirmationDialog.confirmInfo",
				mobileToConfirm));
		infoLabel.setSizeFull();
	
		errorLabel = new Label();
		errorLabel.setSizeFull();
		errorLabel.setVisible(false);

		FormLayout mainForm = new FormLayout();
		mainForm.add(field, errorLabel);
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setAlignItems(FlexComponent.Alignment.START);
		wrapper.setMargin(false);
		wrapper.add(infoLabel, mainForm);

		return wrapper;
	}
	

	protected Component getContents()
	{	
		if (isCapchaNeeded())
		{
			captchaComponent.setVisible(true);
		} else 
		{
			capchaVerified = true;
			sendVerificationCode();
			confirmCodeComponent.setVisible(true);
		}
		VerticalLayout main = new VerticalLayout();
		main.add(captchaComponent, confirmCodeComponent);
		return main;
	}
	
	private void sendVerificationCode()
	{
		
		try
		{
			code = mobileConfirmationMan.sendConfirmationRequest(confirmationConfiguration,
					mobileToConfirm, confirmationInfo);

		} catch (EngineException er)
		{
			notificationPresenter.showError(msg.getMessage(
					"MobileNumberConfirmationDialog.confirmationSendError",
					mobileToConfirm), er.getMessage());
			log.error("Cannot send confirmation request message", er);
		}
	}

	protected void onConfirm()
	{

		if (!capchaVerified)
		{
			try
			{
				captcha.verify();
				capchaVerified = true;
				sendVerificationCode();
				captchaComponent.setVisible(false);
				confirmCodeComponent.setVisible(true);
				return;
			} catch (WrongArgumentException e)
			{
				return;
			}
		}

		
		if (code != null && field.getValue().equals(code.getValue()))
		{
			if (System.currentTimeMillis() > code.getValidTo())
			{
				setError((msg.getMessage(
						"MobileNumberConfirmationDialog.invalidCode")));
				return;
			}
			close();
			confirmationInfo.confirm();
			callback.onConfirm();
		} else
		{
			setError(msg.getMessage(
					"MobileNumberConfirmationDialog.incorrectCode"));
		}
	}

	private void setError(String msg)
	{
		if (msg != null)
		{
			errorLabel.setVisible(true);
			errorLabel.setText(msg);
			field.setErrorMessage(msg);

		} else
		{
			errorLabel.setText("");
			errorLabel.setVisible(false);
			field.setErrorMessage(null);
		}

	}

	public interface Callback
	{
		void onConfirm();
	}

	private boolean isCapchaNeeded()
	{
		if (InvocationContext.getCurrent() != null)
			return InvocationContext.getCurrent().getLoginSession() == null;

		return true;
	}

}