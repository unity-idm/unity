/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.invitations;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.upman.front.model.GroupTreeNode;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.vaadin.elements.FormLayoutLabel;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.utils.EmailUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static java.util.Optional.ofNullable;

class InvitationForm extends FormLayout
{
	private static final Locale EUROPEAN_FORMAT_LOCALE = new Locale("DE");

	private final MessageSource msg;

	private final Binder<InvitationRequest> binder;
	private final TextArea emailsTextArea;
	private final GroupMultiComboBox groupsComboBox;
	private final Checkbox allowModifyGroupsCheckbox;
	private final DateTimePicker expirationDateTimePicker;

	public InvitationForm(MessageSource msg, ProjectGroup projectGroup, List<GroupTreeNode> groups, HtmlContainer container)
	{
		this.msg = msg;

		binder = new BeanValidationBinder<>(InvitationRequest.class);
		binder.setBean(new InvitationRequest(projectGroup));

		emailsTextArea = new TextArea();
		emailsTextArea.setPlaceholder(msg.getMessage("NewInvitationDialog.emailsPrompt"));
		emailsTextArea.setWidth("24em");
		emailsTextArea.focus();
		emailsTextArea.setTooltipText(msg.getMessage("NewInvitationDialog.emailsDesc"));

		allowModifyGroupsCheckbox = new Checkbox(msg.getMessage("NewInvitationDialog.allowModifyGroups"));
		allowModifyGroupsCheckbox.setEnabled(false);

		groupsComboBox = new GroupMultiComboBox();
		groupsComboBox.setWidth("24em");

		groupsComboBox.setItems(groups);
		groupsComboBox.addValueChangeListener(e -> {
			if (e.getValue() == null || e.getValue().isEmpty())
			{
				allowModifyGroupsCheckbox.setEnabled(false);
				allowModifyGroupsCheckbox.setValue(false);
			} else
				allowModifyGroupsCheckbox.setEnabled(true);
		});
		groupsComboBox.setTooltipText(msg.getMessage("NewInvitationDialog.groupsDesc"));

		expirationDateTimePicker = new DateTimePicker();
		expirationDateTimePicker.setLocale(EUROPEAN_FORMAT_LOCALE);

		setWidth("35em");
		setLabels();
		setBinder();

		expirationDateTimePicker.setValue(LocalDateTime.now().plusDays(3));
	}

	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}

	public InvitationRequest getInvitationRequest()
	{
		return binder.getBean();
	}

	private void setBinder()
	{
		binder.forField(emailsTextArea)
				.asRequired(msg.getMessage("fieldRequired"))
				.withConverter(
						value -> ofNullable(value)
								.map(val -> val.split("\n"))
								.map(Set::of)
								.orElseGet(Set::of),
						value -> ofNullable(value)
								.map(val -> String.join("\n", val))
								.orElse("")
				)
				.withValidator(
						value -> value.stream().allMatch(email -> EmailUtils.validate(email.trim()) == null),
						msg.getMessage("NewInvitationDialog.incorrectEmail")
				)
				.bind(model -> model.emails, (model, addresses) -> model.emails = addresses);

		binder.forField(groupsComboBox)
				.bind(model -> model.groups, (model, groups) -> model.groups = groups);

		binder.forField(allowModifyGroupsCheckbox)
				.bind(model -> model.allowModifyGroups, (model, allowModifyGroups) -> model.allowModifyGroups = allowModifyGroups);

		binder.forField(expirationDateTimePicker)
				.asRequired(msg.getMessage("fieldRequired"))
				.withConverter(
						date -> ofNullable(date)
								.map(val -> val.atZone(ZoneId.systemDefault()).toInstant())
								.orElse(null),
						date -> ofNullable(date)
								.map(val -> LocalDateTime.ofInstant(val, ZoneId.systemDefault()))
								.orElse(null)
				)
				.withValidator(
						value -> value != null && value.isAfter(Instant.now()),
						msg.getMessage("NewInvitationDialog.invalidLifeTime")
				)
				.bind(model -> model.expiration, (model, expiration) -> model.expiration = expiration);
	}

	private void setLabels()
	{
		addFormItem(emailsTextArea, new FormLayoutLabel(msg.getMessage("NewInvitationDialog.emails")));
		addFormItem(groupsComboBox, new FormLayoutLabel(msg.getMessage("NewInvitationDialog.groups")));
		addFormItem(allowModifyGroupsCheckbox, "");
		addFormItem(expirationDateTimePicker, new FormLayoutLabel(msg.getMessage("NewInvitationDialog.invitationLivetime")));
	}
}
