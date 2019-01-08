/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.confirmations;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.confirmation.MobileNumberConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Dialog for verify mobile number
 * @author P.Piernik
 *
 */
public class MobileNumberConfirmationDialog extends AbstractDialog
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, MobileNumberConfirmationDialog.class);
	
	private Callback callback;
	private TextField field;
	private CaptchaComponent captcha;
	private MobileNumberConfirmationManager mobileConfirmationMan;
	private MobileNumberConfirmationConfiguration confirmationConfiguration;
	private SMSCode code;
	private String mobileToConfirm;
	private ConfirmationInfo confirmationInfo;
	private Component captchaComponent;
	private Component confirmCodeComponent;
	private boolean capchaVerified = false;
	private Label errorLabel;
	
	
	public MobileNumberConfirmationDialog(String mobileToConfirm, ConfirmationInfo confirmatioInfo,
			UnityMessageSource msg,
			MobileNumberConfirmationManager mobileConfirmationMan,
			MobileNumberConfirmationConfiguration confirmationConfiguration,
			Callback callback)
	{
		super(msg, msg.getMessage("MobileNumberConfirmationDialog.caption"));
		this.callback = callback;
		this.mobileConfirmationMan = mobileConfirmationMan;
		this.confirmationConfiguration = confirmationConfiguration;
		this.mobileToConfirm = mobileToConfirm;
		this.confirmationInfo = confirmatioInfo;
		
		this.captchaComponent = getCapchaComponent();
		this.captchaComponent.setVisible(false);
		this.confirmCodeComponent = getConfirmCodeComponent();
		this.confirmCodeComponent.setVisible(false);
		setSizeEm(36, 25);
	}

	
	
	private Component getCapchaComponent()
	{
		Label infoLabel = new Label(msg.getMessage("MobileNumberConfirmationDialog.capchaInfo"));
		infoLabel.setSizeFull();	
		captcha = new CaptchaComponent(msg, 4, false);		
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		wrapper.addComponents(infoLabel, captcha.getAsComponent());
		return wrapper;
	}
	
	private Component getConfirmCodeComponent()
	{
		field = new TextField();
		field.setCaption(msg.getMessage("MobileNumberConfirmationDialog.code"));
		field.setRequiredIndicatorVisible(true);		
		Label infoLabel = new Label(msg.getMessage("MobileNumberConfirmationDialog.confirmInfo",
				mobileToConfirm));
		infoLabel.setSizeFull();
	
		errorLabel = new Label();
		errorLabel.setSizeFull();
		errorLabel.setVisible(false);
		errorLabel.setStyleName(Styles.error.toString());
		
		CompactFormLayout mainForm = new CompactFormLayout();
		mainForm.addComponents(field,errorLabel);
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		wrapper.addComponents(infoLabel, mainForm);
		return wrapper;
	}
	
	
	@Override
	protected Component getContents() throws Exception
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
		main.addComponents(captchaComponent, confirmCodeComponent);
		main.setSizeFull();
		return main;
	}
	
	@Override
	protected Focusable getFocussedComponent()
	{
		return captchaComponent.isVisible() ? captcha.getFocussTarget() : field;
	}
	
	private void sendVerificationCode()
	{
		
		try
		{
			code = mobileConfirmationMan.sendConfirmationRequest(confirmationConfiguration,
					mobileToConfirm, confirmationInfo);

		} catch (EngineException er)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"MobileNumberConfirmationDialog.confirmationSendError",
					mobileToConfirm), er);
			log.error("Cannot send confirmation request message", er);
		}
	}

	@Override
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
			return;
		}
	}

	private void setError(String msg)
	{
		if (msg != null)
		{
			errorLabel.setVisible(true);
			errorLabel.setValue(msg);
			field.setComponentError(new UserError(msg));

		} else
		{
			errorLabel.setValue("");
			errorLabel.setVisible(false);
			field.setComponentError(null);
		}

	}

	public interface Callback
	{
		public void onConfirm();
	}

	private boolean isCapchaNeeded()
	{
		if (InvocationContext.getCurrent() != null)
			return InvocationContext.getCurrent().getLoginSession() == null;

		return true;
	}

}
