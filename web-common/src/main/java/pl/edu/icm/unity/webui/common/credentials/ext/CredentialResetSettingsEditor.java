/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.ext;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Slider;

import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.authn.CredentialResetSettings;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.PasswordResetTemplateDef;
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
	private Slider codeLength;
	private CheckBox requireEmailConfirmation;
	private CheckBox requireQuestionConfirmation;
	private TextFieldWithButton questionAdder;
	private ComboBox codeMessageTemplate;
	private ListOfElements<String> questions;
	private MessageTemplateManagement msgTplMan;
	
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
		Label requireEmailConfirmation = new Label(initial.isRequireEmailConfirmation() ? 
				msg.getMessage("yes") : msg.getMessage("no"));
		requireEmailConfirmation.setCaption(msg.getMessage("CredentialResetSettings.requireEmailConfirmation"));
		Label codeTemplate = new Label(initial.getSecurityCodeMsgTemplate());
		codeTemplate.setCaption(msg.getMessage("CredentialResetSettings.messageTemplate"));
		
		Label requireQuestionConfirmation = new Label(initial.isRequireSecurityQuestion() ? 
				msg.getMessage("yes") : msg.getMessage("no"));
		requireQuestionConfirmation.setCaption(msg.getMessage(
				"CredentialResetSettings.requireQuestionConfirmation"));
		parent.addComponents(codeLength, requireEmailConfirmation, codeTemplate, requireQuestionConfirmation);
		
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
		parent.addComponents(enable, codeLength, requireEmailConfirmation, 
				codeMessageTemplate, requireQuestionConfirmation, questionAdder, questions);
	}
	
	private void initUI()
	{
		enable = new CheckBox(msg.getMessage("CredentialResetSettings.enable"));
		enable.setImmediate(true);
		enable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				setEnabled(enable.getValue());
			}
		});
		
		codeLength = new Slider(msg.getMessage("CredentialResetSettings.codeLength"), 2, 10);
		codeLength.setWidth(100, Unit.PERCENTAGE);
		
		requireEmailConfirmation = new CheckBox(
				msg.getMessage("CredentialResetSettings.requireEmailConfirmation"));
		requireEmailConfirmation.setImmediate(true);
		requireEmailConfirmation.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				boolean state = requireEmailConfirmation.getValue();
				codeMessageTemplate.setEnabled(state);
			}
		});
		requireQuestionConfirmation = new CheckBox(
				msg.getMessage("CredentialResetSettings.requireQuestionConfirmation"));
		
		codeMessageTemplate = new CompatibleTemplatesComboBox(PasswordResetTemplateDef.NAME, msgTplMan);
		codeMessageTemplate.setCaption(msg.getMessage("CredentialResetSettings.messageTemplate"));
		
		requireQuestionConfirmation.setImmediate(true);
		requireQuestionConfirmation.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				boolean state = requireQuestionConfirmation.getValue();
				questionAdder.setEnabled(state);
				questions.setEnabled(state);
			}
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
	
	private void setEnabled(boolean how)
	{
		codeLength.setEnabled(how);
		requireEmailConfirmation.setEnabled(how);
		requireQuestionConfirmation.setEnabled(how);
		codeMessageTemplate.setEnabled(how);
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
		codeLength.setValue((double)initial.getCodeLength());
		requireEmailConfirmation.setValue(initial.isRequireEmailConfirmation());
		requireQuestionConfirmation.setValue(initial.isRequireEmailConfirmation());
		for (String question: initial.getQuestions())
			questions.addEntry(question);
		codeMessageTemplate.setValue(initial.getSecurityCodeMsgTemplate());
		setEnabled(initial.isEnabled());
	}
	
	public CredentialResetSettings getValue()
	{
		CredentialResetSettings ret = new CredentialResetSettings();
		ret.setEnabled(enable.getValue());
		ret.setCodeLength((int)(double)codeLength.getValue());
		ret.setRequireEmailConfirmation(requireEmailConfirmation.getValue());
		ret.setRequireSecurityQuestion(requireQuestionConfirmation.getValue());
		ret.setQuestions(questions.getElements());
		if (codeMessageTemplate.getValue() != null)
			ret.setSecurityCodeMsgTemplate(codeMessageTemplate.getValue().toString());
		return ret;
	}
	
}
