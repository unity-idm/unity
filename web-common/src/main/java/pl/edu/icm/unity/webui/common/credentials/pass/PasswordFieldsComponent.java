/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.CredentialRecentlyUsedException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker.StrengthInfo;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.binding.SingleStringFieldBinder;
import pl.edu.icm.unity.webui.common.binding.StringBindingValue;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

/**
 * Holds the password text fields with security questions if configured.
 * Does not contain security meter and hints.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class PasswordFieldsComponent extends CustomComponent
{
	private final UnityMessageSource msg;
	private final PasswordCredential config;

	private PasswordFieldWithContextLabel password1;
	private PasswordFieldWithContextLabel password2;
	private ComboBox<String> questionSelection;
	private TextField answer;
	private boolean requireQA;
	private CredentialEditorContext context;
	private Consumer<String> onPasswordChangeListener;
	private SingleStringFieldBinder binder;
	
	public PasswordFieldsComponent(UnityMessageSource msg, CredentialEditorContext context, 
			PasswordCredential config, Consumer<String> onPasswordChangeListener)
	{
		super();
		this.msg = msg;
		this.config = config;
		this.context = context;
		this.onPasswordChangeListener = onPasswordChangeListener;
		
		initUI();
	}

	@Override
	public void focus()
	{
		password1.focus();
	}
	
	private void initUI()
	{
		binder =  new SingleStringFieldBinder(msg);
		VerticalLayout root = new VerticalLayout();
		root.setSpacing(true);
		root.setMargin(false);
		
		password1 = new PasswordFieldWithContextLabel(context == null ? false : context.isShowLabelInline());
		password1.setLabel(msg.getMessage("PasswordCredentialEditor.password"));
		password1.setValueChangeMode(ValueChangeMode.LAZY);
		password1.addValueChangeListener(event -> onPasswordChangeListener.accept(event.getValue()));
		password1.addStyleName("u-password-setup");
	
		password2 = new PasswordFieldWithContextLabel(context.isShowLabelInline());
		password2.setLabel(msg.getMessage("PasswordCredentialEditor.repeatPassword"));
		password2.addStyleName("u-password-repeat");
		
		root.addComponents(password1, password2);
		
		if (context.isRequired())
			password2.setRequiredIndicatorVisible(true);
		
		PasswordCredentialResetSettings resetSettings = config.getPasswordResetSettings();
		requireQA = resetSettings.isEnabled() && resetSettings.isRequireSecurityQuestion(); 
		if (requireQA)
		{
			questionSelection = new ComboBox<>(msg.getMessage("PasswordCredentialEditor.selectQuestion"));
			questionSelection.setItems(resetSettings.getQuestions());
			questionSelection.setValue(resetSettings.getQuestions().get(0));
			questionSelection.setEmptySelectionAllowed(false);
			answer = new TextField(msg.getMessage("PasswordCredentialEditor.answer"));
			if (context.isRequired())
				answer.setRequiredIndicatorVisible(true);
			root.addComponents(questionSelection, answer);
		}
		
		setCompositionRoot(root);
		
		if (context.isCustomWidth())
		{
			setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			password1.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			password2.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			if (questionSelection != null)
				questionSelection.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			if (answer != null)
				answer.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		}
		
		binder.forField(password1, context.isRequired()).bind("value");
		binder.setBean(new StringBindingValue(""));
	}
	
	private boolean isValid()
	{
		String password = this.password1.getValue();
		StrengthInfo measure = StrengthChecker.measure(password, config.getMinScore(), msg);
		if (measure.score < config.getMinScore())
			return false;
		if (password.length() < config.getMinLength())
			return false;
		if (StrengthChecker.getCharacterClasses(password) < config.getMinClassesNum())
			return false;
		if (config.isDenySequences() && !StrengthChecker.hasNoTrivialSequences(password))
			return false;
		return true;
	}
	
	public String getValue() throws IllegalCredentialException
	{
		if (!context.isRequired() && password1.getValue().isEmpty() && password2.getValue().isEmpty())
			return null;
		
		if (context.isRequired() && password1.getValue().isEmpty())
		{
			password1.setComponentError(new UserError(msg.getMessage("PasswordCredentialEditor.newPasswordRequired")));
			throw new MissingCredentialException(msg.getMessage("PasswordCredentialEditor.newPasswordRequired"));
		}
		
		password1.setComponentError(null);
		
		if (!isValid())
		{
			password1.clear();
			password2.clear();
			throw new IllegalCredentialException(msg.getMessage("PasswordCredentialEditor.passwordTooWeak"));
		}
		
		String p1 = password1.getValue();
		String p2 = password2.getValue();
		if (!p1.equals(p2))
		{
			password1.clear();
			password2.clear();
			String err = msg.getMessage("PasswordCredentialEditor.passwordsDoNotMatch");
			throw new IllegalCredentialException(err);
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
			String ques = questionSelection.getValue();
			int qNum=0;
			List<String> questions = config.getPasswordResetSettings().getQuestions(); 
			for (; qNum<questions.size(); qNum++)
				if (questions.get(qNum).equals(ques))
					break;
			pToken.setAnswer(ans);
			pToken.setQuestion(qNum);
		}
		return pToken.toJson();
	}
	
	public void setCredentialError(EngineException error)
	{
		password1.clear();
		password2.setValue("");
		if (error == null)
		{
			password1.setComponentError(null);
			password2.setComponentError(null);
			return;
		}
		
		if (error instanceof CredentialRecentlyUsedException)
		{
			NotificationPopup.showError(
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
	
	public void setLabel(String label)
	{
		password1.setLabel(label);
	}

	public void disablePasswordRepeat()
	{
		password2.setVisible(false);
	}
}
