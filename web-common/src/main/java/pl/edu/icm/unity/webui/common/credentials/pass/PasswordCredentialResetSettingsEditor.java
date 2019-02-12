/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import java.util.Arrays;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.pass.EmailPasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.credential.pass.MobilePasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings.ConfirmationMode;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.Column;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn.Position;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.TextFieldWithButton;
import pl.edu.icm.unity.webui.common.TextFieldWithButton.ButtonHandler;

/**
 * Part of UI, insertable into FormLayout, useful for {@link PasswordCredentialResetSettings} editing or viewing.
 * @author K. Benedyczak
 */
public class PasswordCredentialResetSettingsEditor
{
	private UnityMessageSource msg;
	private PasswordCredentialResetSettings initial;
	private CheckBox enable;
	private IntStepper codeLength;
	private CheckBox requireQuestionConfirmation;
	private TextFieldWithButton questionAdder;
	private CompatibleTemplatesComboBox emailCodeMessageTemplate;
	private CompatibleTemplatesComboBox mobileCodeMessageTemplate;
	private ListOfElementsWithActions<String> questions;
	private MessageTemplateManagement msgTplMan;
	private ComboBox<ConfirmationMode> confirmationMode;
	
	public PasswordCredentialResetSettingsEditor(UnityMessageSource msg, MessageTemplateManagement msgTplMan)
	{
		this(msg, msgTplMan, new PasswordCredentialResetSettings());
	}
	
	public PasswordCredentialResetSettingsEditor(UnityMessageSource msg, MessageTemplateManagement msgTplMan,
			PasswordCredentialResetSettings initial)
	{
		this.msg = msg;
		this.initial = initial;
		this.msgTplMan = msgTplMan;
	}
	
	public void addViewerToLayout(FormLayout parent)
	{
		Label status = new Label(initial.isEnabled() ? msg.getMessage("yes") : msg.getMessage("no"));
		status.setCaption(msg.getMessage("PasswordCredentialResetSettings.enableRo"));
		parent.addComponent(status);
		if (!initial.isEnabled())
			return;
		
		Label codeLength = new Label(String.valueOf(initial.getCodeLength()));
		codeLength.setCaption(msg.getMessage("PasswordCredentialResetSettings.codeLength"));
		Label confirmationMode = new Label(
				msg.getMessage("PasswordCredentialResetSettings.confirmationMode"
						+ initial.getConfirmationMode().toString()));
		confirmationMode.setCaption(
				msg.getMessage("PasswordCredentialResetSettings.confirmationMode"));
		
		Label emailCodeTemplate = new Label(initial.getEmailSecurityCodeMsgTemplate());
		emailCodeTemplate.setCaption(msg.getMessage("PasswordCredentialResetSettings.emailMessageTemplate"));
		
		Label mobileCodeTemplate = new Label(initial.getMobileSecurityCodeMsgTemplate());
		mobileCodeTemplate.setCaption(msg.getMessage("PasswordCredentialResetSettings.mobileMessageTemplate"));
		
		Label requireQuestionConfirmation = new Label(initial.isRequireSecurityQuestion() ? 
				msg.getMessage("yes") : msg.getMessage("no"));
		requireQuestionConfirmation.setCaption(msg.getMessage(
				"PasswordCredentialResetSettings.requireQuestionConfirmation"));
		parent.addComponents(codeLength, confirmationMode, emailCodeTemplate, mobileCodeTemplate, requireQuestionConfirmation);
		
		if (!initial.isRequireSecurityQuestion())
			return;
		
		Label questions = new Label(String.valueOf(initial.getQuestions().get(0)));
		questions.setCaption(msg.getMessage("PasswordCredentialResetSettings.questions"));
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
		enable = new CheckBox(msg.getMessage("PasswordCredentialResetSettings.enable"));
		enable.addValueChangeListener(event -> setEnabled(enable.getValue()));
		
		codeLength = new IntStepper(msg.getMessage("PasswordCredentialResetSettings.codeLength"));
		codeLength.setMinValue(2);
		codeLength.setMaxValue(10);
		codeLength.setWidth(3, Unit.EM);
	
		confirmationMode = new ComboBox<>(msg.getMessage("PasswordCredentialResetSettings.confirmationMode"));
		confirmationMode.setItems(ConfirmationMode.values());
		confirmationMode.addSelectionListener(e->{	
			emailCodeMessageTemplate.setEnabled(getEmailMessageTemplateState());
			mobileCodeMessageTemplate.setEnabled(getMobileMessageTemplateState());
		});
		confirmationMode.setItemCaptionGenerator(i -> msg.getMessage("PasswordCredentialResetSettings.confirmationMode" + i.toString()));
		
		confirmationMode.setEmptySelectionAllowed(false);
		
		
		requireQuestionConfirmation = new CheckBox(
				msg.getMessage("PasswordCredentialResetSettings.requireQuestionConfirmation"));
		
		emailCodeMessageTemplate = new CompatibleTemplatesComboBox(EmailPasswordResetTemplateDef.NAME, msgTplMan);
		emailCodeMessageTemplate.setCaption(msg.getMessage("PasswordCredentialResetSettings.emailMessageTemplate"));
		emailCodeMessageTemplate.setEmptySelectionAllowed(false);
		
		mobileCodeMessageTemplate = new CompatibleTemplatesComboBox(MobilePasswordResetTemplateDef.NAME, msgTplMan);
		mobileCodeMessageTemplate.setCaption(msg.getMessage("PasswordCredentialResetSettings.mobileMessageTemplate"));
		mobileCodeMessageTemplate.setEmptySelectionAllowed(false);
		
		confirmationMode.setValue(ConfirmationMode.NothingRequire);
		
		
		requireQuestionConfirmation.addValueChangeListener(event -> {
			boolean state = requireQuestionConfirmation.getValue();
			questionAdder.setEnabled(state);
			questions.setEnabled(state);
		});
		
		
		questionAdder = new TextFieldWithButton(
				msg.getMessage("PasswordCredentialResetSettings.defineNewQuestion"), 
				Images.add.getResource(), msg.getMessage("PasswordCredentialResetSettings.addQuestion"),
				new ButtonHandler()
				{
					@Override
					public String validate(String value)
					{
						if (value == null || value.trim().equals(""))
							return msg.getMessage("PasswordCredentialResetSettings.questionMustBeNonEmpty");
						return null;
					}
					
					@Override
					public boolean perform(String value)
					{
						questions.addEntry(value);
						return true;
					}
				});
		
		SingleActionHandler<String> remove = SingleActionHandler.builder4Delete(msg, String.class)
				.withHandler(r -> {

					questions.removeEntry(r.iterator().next());

				}

				).build();

		questions = new ListOfElementsWithActions<>(Arrays.asList(new Column<>(null, r -> new Label(r), 1)),
				new ActionColumn<>(null, Arrays.asList(remove), 0, Position.Left));
		
		
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
		emailCodeMessageTemplate.setEnabled(how);
		mobileCodeMessageTemplate.setEnabled(how);
		
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
	
	private void setValue(PasswordCredentialResetSettings initial)
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
	
	public PasswordCredentialResetSettings getValue()
	{
		PasswordCredentialResetSettings ret = new PasswordCredentialResetSettings();
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
