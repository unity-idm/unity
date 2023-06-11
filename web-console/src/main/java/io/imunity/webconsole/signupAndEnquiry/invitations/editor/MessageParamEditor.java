/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.vaadin.data.ValidationResult;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msgtemplate.MessageTemplateValidator;
import pl.edu.icm.unity.webui.common.GridWithEditor;
import pl.edu.icm.unity.webui.common.Styles;

class MessageParamEditor extends GridWithEditor<MessageParam>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, MessageParamEditor.class);
	private final Map<String, MessageTemplate> msgTemplates;
	private final MessageVariableNameTextField messageVariableNameTextField;

	MessageParamEditor(MessageSource msg, Map<String, MessageTemplate> msgTemplates)
	{
		super(msg, MessageParam.class, m -> !m.isNameEditable(), false);
		this.msgTemplates = msgTemplates;

		messageVariableNameTextField = new MessageVariableNameTextField();
		addTextColumn(p -> p.getName(), (p, v) -> p.setName(v), msg.getMessage("InvitationEditor.messageVariableName"),
				2, true, Optional.of((v, c) ->
				{
					if (!messageVariableNameTextField.isReadOnly() && !Pattern.matches("[a-zA-Z0-9_]+", v))
						return ValidationResult.error(msg.getMessage("InvitationEditor.messageVariableNameError"));
					return ValidationResult.ok();
				}), messageVariableNameTextField);
		addTextColumn(p -> p.getValue(), (p, v) -> p.setValue(v),
				msg.getMessage("InvitationEditor.messageVariableValue"), 2, false);
		setCaption(msg.getMessage("InvitationEditor.messageVariables"));

	}

	public void setMessageParams(BaseForm form1, BaseForm form2)
	{
		clear();
		removeAllElements();
		Set<String> variableSet = getVariableSet(form1);
		variableSet.addAll(getVariableSet(form2));
		setNotEditableVariables(variableSet);
	}

	public void setMessageParams(BaseForm form)
	{
		clear();
		removeAllElements();
		setNotEditableVariables(getVariableSet(form));
	}

	private Set<String> getVariableSet(BaseForm form)
	{
		if (form == null)
		{
			return Collections.emptySet();
		}
		String invitationTemplate = form.getNotificationsConfiguration().getInvitationTemplate();
		if (invitationTemplate == null)
		{
			return Collections.emptySet();
		}
		MessageTemplate msgTemplate = getMessageTemplate(invitationTemplate);
		if (msgTemplate == null)
		{
			return Collections.emptySet();
		}
		return MessageTemplateValidator.extractCustomVariables(msgTemplate.getMessage());
	}

	private MessageTemplate getMessageTemplate(String msgTemplate)
	{
		MessageTemplate template = msgTemplates.get(msgTemplate);
		if (template == null)
			log.error("Can not read invitation template of the form, won't fill any parameters");
		return template;
	}

	private void setNotEditableVariables(Set<String> variablesSet)
	{
		List<String> variables = new ArrayList<>(variablesSet);
		Collections.sort(variables);

		List<MessageParam> ret = new ArrayList<>();
		for (String variable : variables)
		{
			String caption = variable.startsWith(MessageTemplateDefinition.CUSTOM_VAR_PREFIX)
					? variable.substring(MessageTemplateDefinition.CUSTOM_VAR_PREFIX.length())
					: variable;
			ret.add(new MessageParam(caption, "", false));
		}

		setValue(ret);
		messageVariableNameTextField
				.setNotEditableNames(ret.stream().map(m -> m.getName()).collect(Collectors.toSet()));

	}

	public static class MessageVariableNameTextField extends TextField
	{
		private Set<String> notEditableNames;

		public MessageVariableNameTextField()
		{
			this.notEditableNames = new HashSet<>();
		}

		public Set<String> getNotEditableNames()
		{
			return notEditableNames;
		}

		public void setNotEditableNames(Set<String> notEditableNames)
		{
			this.notEditableNames.clear();
			this.notEditableNames.addAll(notEditableNames);
		}

		@Override
		public void setValue(String value)
		{
			super.setValue(value);
			setReadOnly(false);
			removeStyleName(Styles.background.toString());
			if (value != null && !value.isEmpty() && notEditableNames.contains(value))
			{
				setReadOnly(true);
				addStyleName(Styles.background.toString());
			}
		}
	}

	public Map<String, String> getParams()
	{
		return getValue().stream()
				.collect(Collectors.toMap(
						paramField -> MessageTemplateDefinition.CUSTOM_VAR_PREFIX + paramField.getName(),
						paramField -> paramField.getValue()));
	}

}
