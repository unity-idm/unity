/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import java.util.List;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.CredentialRecentlyUsedException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalPreviousCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExtraInfo;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Allows to setup password for password credential.
 * @author K. Benedyczak
 */
public class PasswordCredentialEditor implements CredentialEditor
{
	private UnityMessageSource msg;
	private PasswordField passwordCurrent;
	private PasswordEditComponent password1;
	private PasswordField password2;
	private ComboBox<String> questionSelection;
	private TextField answer;
	private boolean requireQA;
	private PasswordCredential helper;
	private boolean required;
	private boolean askAboutCurrent;

	public PasswordCredentialEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(boolean askAboutCurrent, 
			String credentialConfiguration, boolean required, Long entityId, boolean adminMode)
	{
		this.required = required;
		this.askAboutCurrent = askAboutCurrent;
		helper = new PasswordCredential();
		helper.setSerializedConfiguration(JsonUtil.parse(credentialConfiguration));
		
		ComponentsContainer ret = new ComponentsContainer();

		if (askAboutCurrent)
		{
			passwordCurrent = new PasswordField(msg.getMessage(
					"PasswordCredentialEditor.currentPassword"));
			ret.add(passwordCurrent);
		}
		password1 = new PasswordEditComponent(msg, helper);
		password1.focus();
		password2 = new PasswordField(msg.getMessage("PasswordCredentialEditor.repeatPassword"));
		if (required)
		{
			password1.setRequiredIndicatorVisible(true);
			password2.setRequiredIndicatorVisible(true);
			if (askAboutCurrent)
				passwordCurrent.setRequiredIndicatorVisible(true);
		}
		ret.add(password1.getAsContainer().getComponents());
		ret.add(password2);
		
		PasswordCredentialResetSettings resetSettings = helper.getPasswordResetSettings();
		requireQA = resetSettings.isEnabled() && resetSettings.isRequireSecurityQuestion(); 
		if (requireQA)
		{
			questionSelection = new ComboBox<String>(msg.getMessage("PasswordCredentialEditor.selectQuestion"));
			questionSelection.setItems(resetSettings.getQuestions());
			questionSelection.setValue(resetSettings.getQuestions().get(0));
			questionSelection.setEmptySelectionAllowed(false);
			answer = new TextField(msg.getMessage("PasswordCredentialEditor.answer"));
			if (required)
				answer.setRequiredIndicatorVisible(true);
			ret.add(questionSelection, answer);
		}
		return ret;
	}

	@Override
	public String getValue() throws IllegalCredentialException
	{
		if (!required && password1.getPassword().isEmpty() && password2.getValue().isEmpty())
			return null;
		
		if (required && password1.getPassword().isEmpty())
			throw new IllegalCredentialException(msg.getMessage("fieldRequired"));
		
		String p1 = password1.getPassword();
		String p2 = password2.getValue();
		if (!p1.equals(p2))
		{
			password1.clear();
			password2.clear();
			String err = msg.getMessage("PasswordCredentialEditor.passwordsDoNotMatch");
			throw new IllegalCredentialException(err);
		}

		if (!password1.isValid())
		{
			password1.clear();
			password2.clear();
			throw new IllegalCredentialException(msg.getMessage("PasswordCredentialEditor.passwordTooWeak"));
		}
		
		PasswordToken pToken = new PasswordToken(p1);
		
		if (requireQA)
		{
			String ans = answer.getValue();
			if (ans == null || ans.trim().length() < 3)
			{
				String err = msg.getMessage("PasswordCredentialEditor.answerRequired", 2);
				throw new IllegalCredentialException(err);
			}
			answer.setComponentError(null);
			String ques = (String) questionSelection.getValue();
			int qNum=0;
			List<String> questions = helper.getPasswordResetSettings().getQuestions(); 
			for (; qNum<questions.size(); qNum++)
				if (questions.get(qNum).equals(ques))
					break;
			pToken.setAnswer(ans);
			pToken.setQuestion(qNum);
		}
		return pToken.toJson();
	}

	@Override
	public Component getViewer(String credentialExtraInformation)
	{
		PasswordExtraInfo pei = PasswordExtraInfo.fromJson(credentialExtraInformation);
		if (pei.getLastChange() == null)
			return null;

		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
		ret.setMargin(true);
		
		ret.addComponent(new Label(msg.getMessage("PasswordCredentialEditor.lastModification", 
				pei.getLastChange())));
		
		PasswordCredentialResetSettings resetS = helper.getPasswordResetSettings();
		if (resetS.isEnabled() && !resetS.getQuestions().isEmpty())
		{
			String secQ = pei.getSecurityQuestion() == null ? 
					msg.getMessage("PasswordCredentialEditor.notDefined")
					: pei.getSecurityQuestion();
			ret.addComponent(new Label(msg.getMessage("PasswordCredentialEditor.securityQuestion", secQ)));
		}
		return ret;
	}

	@Override
	public String getCurrentValue() throws IllegalCredentialException
	{
		if (askAboutCurrent && required && passwordCurrent.getValue().isEmpty())
		{
			throw new IllegalCredentialException(msg.getMessage("fieldRequired"));
		} else
		{
			passwordCurrent.setComponentError(null);
		}
		return new PasswordToken(passwordCurrent.getValue()).toJson();
	}

	@Override
	public void setCredentialError(EngineException error)
	{
		password1.clear();
		password2.setValue("");
		
		if (error instanceof IllegalPreviousCredentialException)
		{
			NotificationPopup.showError(msg, 
					msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
					msg.getMessage("PasswordCredentialEditor.wrongOldPasswordError"));
			passwordCurrent.setValue("");
			passwordCurrent.focus();
		} else if (error instanceof CredentialRecentlyUsedException)
		{
			NotificationPopup.showError(msg, 
					msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
					msg.getMessage("PasswordCredentialEditor.recentlyUsedError"));
			password1.focus();
		} else
		{
			NotificationPopup.showError(msg, 
					msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
					error);
			password1.focus();
		}
	}
}
