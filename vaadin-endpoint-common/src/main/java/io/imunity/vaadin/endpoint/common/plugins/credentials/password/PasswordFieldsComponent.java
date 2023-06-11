/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.password;

import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.StringBindingValue;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleStringFieldBinder;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.CredentialRecentlyUsedException;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker.StrengthInfo;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

import java.util.List;
import java.util.function.Consumer;


public class PasswordFieldsComponent extends VerticalLayout implements Focusable<PasswordFieldsComponent>
{
	private final MessageSource msg;
	private final PasswordCredential config;

	private PasswordFieldWithContextLabel password1;
	private PasswordFieldWithContextLabel password2;
	private ComboBox<String> questionSelection;
	private TextField answer;
	private boolean requireQA;
	private final CredentialEditorContext context;
	private final Consumer<String> onPasswordChangeListener;
	private final NotificationPresenter notificationPresenter;

	public PasswordFieldsComponent(MessageSource msg, CredentialEditorContext context,
	                               PasswordCredential config, Consumer<String> onPasswordChangeListener,
	                               NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.config = config;
		this.context = context;
		this.onPasswordChangeListener = onPasswordChangeListener;
		this.notificationPresenter = notificationPresenter;

		initUI();
	}

	@Override
	public void focus()
	{
		password1.focus();
	}
	
	private void initUI()
	{
		setPadding(false);
		SingleStringFieldBinder binder = new SingleStringFieldBinder(msg);
		VerticalLayout root = new VerticalLayout();
		root.setSpacing(false);
		root.setMargin(false);
		root.setPadding(false);
		
		password1 = new PasswordFieldWithContextLabel(context != null && context.isShowLabelInline());
		password1.setLabel(msg.getMessage("PasswordCredentialEditor.password"));
		password1.setValueChangeMode(ValueChangeMode.LAZY);
		password1.addValueChangeListener(event -> onPasswordChangeListener.accept(event.getValue()));
		password1.addClassName("u-password-setup");
	
		password2 = new PasswordFieldWithContextLabel(context.isShowLabelInline());
		password2.setLabel(msg.getMessage("PasswordCredentialEditor.repeatPassword"));
		password2.addClassName("u-password-repeat");
		
		root.add(password1, password2);
		
		if (context.isRequired())
		{
			password1.getElement().setProperty("title", msg.getMessage("fieldRequired"));
			password2.getElement().setProperty("title", msg.getMessage("fieldRequired"));
			password2.setRequiredIndicatorVisible(true);
		}
		
		PasswordCredentialResetSettings resetSettings = config.getPasswordResetSettings();
		requireQA = resetSettings.isEnabled() && resetSettings.isRequireSecurityQuestion(); 
		if (requireQA)
		{
			questionSelection = new ComboBox<>(msg.getMessage("PasswordCredentialEditor.selectQuestion"));
			questionSelection.setItems(resetSettings.getQuestions());
			questionSelection.setValue(resetSettings.getQuestions().get(0));
			questionSelection.setRequired(true);
			answer = new TextField(msg.getMessage("PasswordCredentialEditor.answer"));
			if (context.isRequired())
				answer.setRequiredIndicatorVisible(true);
			root.add(questionSelection, answer);
		}
		
		add(root);
		
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
		binder.forField(password2, context.isRequired()).bind("value");
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
			password1.setErrorMessage(msg.getMessage("PasswordCredentialEditor.newPasswordRequired"));
			password1.setInvalid(true);
			throw new MissingCredentialException(msg.getMessage("PasswordCredentialEditor.newPasswordRequired"));
		}
		
		password1.setInvalid(false);

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
			answer.setErrorMessage(null);
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
			password1.setErrorMessage(null);
			password2.setErrorMessage(null);
			return;
		}
		
		if (error instanceof CredentialRecentlyUsedException)
		{
			notificationPresenter.showError(
					msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
					msg.getMessage("PasswordCredentialEditor.recentlyUsedError"));
			password1.focus();
		} else
		{
			notificationPresenter.showError(msg.getMessage("CredentialChangeDialog.credentialUpdateError"), error.getMessage());
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
