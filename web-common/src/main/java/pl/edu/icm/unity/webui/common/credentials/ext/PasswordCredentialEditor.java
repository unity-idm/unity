/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.ext;

import java.util.List;

import com.vaadin.server.UserError;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.authn.CredentialResetSettings;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.PasswordExtraInfo;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Allows to setup password for password credential.
 * @author K. Benedyczak
 */
public class PasswordCredentialEditor implements CredentialEditor
{
	private UnityMessageSource msg;
	private PasswordField passwordCurrent;
	private PasswordField password1;
	private PasswordField password2;
	private ComboBox questionSelection;
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
			String credentialConfiguration, boolean required)
	{
		this.required = required;
		this.askAboutCurrent = askAboutCurrent;
		helper = new PasswordCredential();
		helper.setSerializedConfiguration(credentialConfiguration);
		
		ComponentsContainer ret = new ComponentsContainer();

		if (askAboutCurrent)
		{
			passwordCurrent = new PasswordField(msg.getMessage(
					"PasswordCredentialEditor.currentPassword"));
			ret.add(passwordCurrent);
		}
		password1 = new PasswordField(msg.getMessage("PasswordCredentialEditor.password"));
		password2 = new PasswordField(msg.getMessage("PasswordCredentialEditor.repeatPassword"));
		if (required)
		{
			password1.setRequired(true);
			password2.setRequired(true);
			if (askAboutCurrent)
				passwordCurrent.setRequired(true);
		}
		ret.add(password1, password2);
		
		CredentialResetSettings resetSettings = helper.getPasswordResetSettings();
		requireQA = resetSettings.isEnabled() && resetSettings.isRequireSecurityQuestion(); 
		if (requireQA)
		{
			questionSelection = new ComboBox(msg.getMessage("PasswordCredentialEditor.selectQuestion"));
			for (String question: resetSettings.getQuestions())
				questionSelection.addItem(question);
			questionSelection.select(resetSettings.getQuestions().get(0));
			questionSelection.setNullSelectionAllowed(false);
			answer = new TextField(msg.getMessage("PasswordCredentialEditor.answer"));
			if (required)
				answer.setRequired(true);
			ret.add(questionSelection, answer);
		}
		return ret;
	}

	@Override
	public String getValue() throws IllegalCredentialException
	{
		if (!required && password1.getValue().isEmpty() && password2.getValue().isEmpty())
			return null;
		
		if (required && password1.getValue().isEmpty())
		{
			password1.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new IllegalCredentialException(msg.getMessage("fieldRequired"));
		} else
			password1.setComponentError(null);
		String p1 = password1.getValue();
		String p2 = password2.getValue();
		if (!p1.equals(p2))
		{
			String err = msg.getMessage("PasswordCredentialEditor.passwordsDoNotMatch");
			password2.setComponentError(new UserError(err));
			throw new IllegalCredentialException(err);
		} else
			password2.setComponentError(null);

		PasswordToken pToken = new PasswordToken(p1);
		
		if (requireQA)
		{
			String ans = answer.getValue();
			if (ans == null || ans.trim().length() < 3)
			{
				String err = msg.getMessage("PasswordCredentialEditor.answerRequired", 2);
				answer.setComponentError(new UserError(err));
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
		
		CredentialResetSettings resetS = helper.getPasswordResetSettings();
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
			passwordCurrent.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			throw new IllegalCredentialException(msg.getMessage("fieldRequired"));
		} else
			passwordCurrent.setComponentError(null);
		return new PasswordToken(passwordCurrent.getValue()).toJson();
	}

	@Override
	public void setCredentialError(String message)
	{
		password1.setComponentError(message == null ? null : new UserError(message));
		password2.setComponentError(message == null ? null : new UserError(message));
	}

	@Override
	public void setPreviousCredentialError(String message)
	{
		if (passwordCurrent != null)
			passwordCurrent.setComponentError(message == null ? null : new UserError(message));
	}
}
