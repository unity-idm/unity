/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.ext;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.authn.CredentialResetSettings;
import pl.edu.icm.unity.engine.api.authn.CredentialResetSettings.ConfirmationMode;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.EmailPasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.credential.MobilePasswordResetTemplateDef;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.ListOfElements.RemoveHandler;
import pl.edu.icm.unity.webui.common.TextFieldWithButton;
import pl.edu.icm.unity.webui.common.TextFieldWithButton.ButtonHandler;

/**
 * Part of UI, insertable into FormLayout, useful for {@link CredentialResetSettings} editing or viewing.
 * @author K. Benedyczak
 */
public class CredentialResetSettingsEditor
{
	private UnityMessageSource msg;
	private CredentialResetSettings initial;
	private CheckBox enable;
	private IntStepper codeLength;
	private CheckBox requireQuestionConfirmation;
	private TextFieldWithButton questionAdder;
	private CompatibleTemplatesComboBox emailCodeMessageTemplate;
	private CompatibleTemplatesComboBox mobileCodeMessageTemplate;
	private ListOfElements<String> questions;
	private MessageTemplateManagement msgTplMan;
	private ComboBox<ConfirmationMode> confirmationMode;
	
	public CredentialResetSettingsEditor(UnityMessageSource msg, MessageTemplateManagement msgTplMan)
	{
		this(msg, msgTplMan, new CredentialResetSettings());
	}
	
	public CredentialResetSettingsEditor(UnityMessageSource msg, MessageTemplateManagement msgTplMan,
			CredentialResetSettings initial)
	{
		this.msg = msg;
		this.initial = initial;
		this.msgTplMan = msgTplMan;
	}
	
	public void addViewerToLayout(FormLayout parent)
	{
		Label status = new Label(initial.isEnabled() ? msg.getMessage("yes") : msg.getMessage("no"));
		status.setCaption(msg.getMessage("CredentialResetSettings.enableRo"));
		parent.addComponent(status);
		if (!initial.isEnabled())
			return;
		
		Label codeLength = new Label(String.valueOf(initial.getCodeLength()));
		codeLength.setCaption(msg.getMessage("CredentialResetSettings.codeLength"));
		Label confirmationMode = new Label(
				msg.getMessage("CredentialResetSettings.confirmationMode"
						+ initial.getConfirmationMode().toString()));
		confirmationMode.setCaption(
				msg.getMessage("CredentialResetSettings.confirmationMode"));
		
		Label emailCodeTemplate = new Label(initial.getEmailSecurityCodeMsgTemplate());
		emailCodeTemplate.setCaption(msg.getMessage("CredentialResetSettings.emailMessageTemplate"));
		
		Label mobileCodeTemplate = new Label(initial.getMobileSecurityCodeMsgTemplate());
		mobileCodeTemplate.setCaption(msg.getMessage("CredentialResetSettings.mobileMessageTemplate"));
		
		Label requireQuestionConfirmation = new Label(initial.isRequireSecurityQuestion() ? 
				msg.getMessage("yes") : msg.getMessage("no"));
		requireQuestionConfirmation.setCaption(msg.getMessage(
				"CredentialResetSettings.requireQuestionConfirmation"));
		parent.addComponents(codeLength, confirmationMode, emailCodeTemplate, mobileCodeTemplate, requireQuestionConfirmation);
		
		if (!initial.isRequireSecurityQuestion())
			return;
		
		Label questions = new Label(String.valueOf(initial.getQuestions().get(0)));
		questions.setCaption(msg.getMessage("CredentialResetSettings.questions"));
		parent.addComponent(questions);
		for (int i=1; i<initial.getQuestions().size(); i++)
			parent.addComponent(new Label(initial.getQuestions().get(i)));
	}
	
	public void addEditorToLayout(FormLayout parent)
	{
		initUI();
		setValue(initial);		
		parent.addComponents(enable, codeLength, confirmationMode, 
				emailCodeMessageTemplate, mobileCodeMessageTemplate, requireQuestionConfirmation, questionAdder, questions);
	}
	
	private void initUI()
	{
		enable = new CheckBox(msg.getMessage("CredentialResetSettings.enable"));
		enable.addValueChangeListener(event -> setEnabled(enable.getValue()));
		
		codeLength = new IntStepper(msg.getMessage("CredentialResetSettings.codeLength"));
		codeLength.setMinValue(2);
		codeLength.setMaxValue(10);
		codeLength.setWidth(3, Unit.EM);
	
		confirmationMode = new ComboBox<>(msg.getMessage("CredentialResetSettings.confirmationMode"));
		confirmationMode.setItems(ConfirmationMode.values());
		confirmationMode.addSelectionListener(e->{	
			emailCodeMessageTemplate.setEnabled(getEmailMessageTemplateState());
			mobileCodeMessageTemplate.setEnabled(getMobileMessageTemplateState());
		});
		confirmationMode.setItemCaptionGenerator(i -> msg.getMessage("CredentialResetSettings.confirmationMode" + i.toString()));
		
		confirmationMode.setEmptySelectionAllowed(false);
		
		
		requireQuestionConfirmation = new CheckBox(
				msg.getMessage("CredentialResetSettings.requireQuestionConfirmation"));
		
		emailCodeMessageTemplate = new CompatibleTemplatesComboBox(EmailPasswordResetTemplateDef.NAME, msgTplMan);
		emailCodeMessageTemplate.setCaption(msg.getMessage("CredentialResetSettings.emailMessageTemplate"));
		emailCodeMessageTemplate.setEmptySelectionAllowed(false);
		
		mobileCodeMessageTemplate = new CompatibleTemplatesComboBox(MobilePasswordResetTemplateDef.NAME, msgTplMan);
		mobileCodeMessageTemplate.setCaption(msg.getMessage("CredentialResetSettings.mobileMessageTemplate"));
		mobileCodeMessageTemplate.setEmptySelectionAllowed(false);
		
		confirmationMode.setValue(ConfirmationMode.NothingRequire);
		
		
		requireQuestionConfirmation.addValueChangeListener(event -> {
			boolean state = requireQuestionConfirmation.getValue();
			questionAdder.setEnabled(state);
			questions.setEnabled(state);
		});
		
		
		questionAdder = new TextFieldWithButton(
				msg.getMessage("CredentialResetSettings.defineNewQuestion"), 
				Images.add.getResource(), msg.getMessage("CredentialResetSettings.addQuestion"),
				new ButtonHandler()
				{
					@Override
					public String validate(String value)
					{
						if (value == null || value.trim().equals(""))
							return msg.getMessage("CredentialResetSettings.questionMustBeNonEmpty");
						return null;
					}
					
					@Override
					public boolean perform(String value)
					{
						questions.addEntry(value);
						return true;
					}
				});
		questions = new ListOfElements<>(msg, new ListOfElements.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new Label(value);
			}
		});
		questions.setRemoveHandler(new RemoveHandler<String>()
		{
			@Override
			public boolean remove(String value)
			{
				return true;
			}
		});
	}
	
	public boolean getEmailMessageTemplateState()
	{
		return confirmationMode.getValue().equals(ConfirmationMode.RequireEmail)
				|| confirmationMode.getValue()
						.equals(ConfirmationMode.RequireEmailAndMobile)
				|| confirmationMode.getValue()
						.equals(ConfirmationMode.RequireEmailOrMobile);
	}

	public boolean getMobileMessageTemplateState()
	{
		return confirmationMode.getValue().equals(ConfirmationMode.RequireMobile)
				|| confirmationMode.getValue()
						.equals(ConfirmationMode.RequireEmailAndMobile)
				|| confirmationMode.getValue()
						.equals(ConfirmationMode.RequireEmailOrMobile);
	}
	
	private void setEnabled(boolean how)
	{
		codeLength.setEnabled(how);
		confirmationMode.setEnabled(how);
		requireQuestionConfirmation.setEnabled(how);
		emailCodeMessageTemplate.setEnabled(getEmailMessageTemplateState());
		mobileCodeMessageTemplate.setEnabled(getMobileMessageTemplateState());
		
		if (how)
		{
			boolean state = requireQuestionConfirmation.getValue();
			questionAdder.setEnabled(state);
			questions.setEnabled(state);
		} else
		{
			questionAdder.setEnabled(false);
			questions.setEnabled(false);
		}
			
	}
	
	private void setValue(CredentialResetSettings initial)
	{
		enable.setValue(initial.isEnabled());
		codeLength.setValue(initial.getCodeLength());
		confirmationMode.setValue(initial.getConfirmationMode());
		requireQuestionConfirmation.setValue(initial.isRequireSecurityQuestion());
		for (String question: initial.getQuestions())
			questions.addEntry(question);
		emailCodeMessageTemplate.setValue(initial.getEmailSecurityCodeMsgTemplate());
		mobileCodeMessageTemplate.setValue(initial.getMobileSecurityCodeMsgTemplate());
		setEnabled(initial.isEnabled());
	}
	
	public CredentialResetSettings getValue()
	{
		CredentialResetSettings ret = new CredentialResetSettings();
		ret.setEnabled(enable.getValue());
		ret.setCodeLength((int)(double)codeLength.getValue());
		ret.setRequireSecurityQuestion(requireQuestionConfirmation.getValue());
		ret.setConfirmationMode(confirmationMode.getValue());
		ret.setQuestions(questions.getElements());
		if (emailCodeMessageTemplate.getValue() != null)
			ret.setEmailSecurityCodeMsgTemplate(emailCodeMessageTemplate.getValue().toString());
		if (mobileCodeMessageTemplate.getValue() != null)
			ret.setMobileSecurityCodeMsgTemplate(mobileCodeMessageTemplate.getValue().toString());
		return ret;
	}
	
}
