/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.groupbrowser;

import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.UserError;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupProperty;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.GridWithEditor;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

class GroupEditDialog extends AbstractDialog
{
	private Callback callback;
	private TextField path;
	private I18nTextField displayedName;
	private I18nTextArea description;
	private CheckBox isPublic;
	private Group originalGroup;
	private GridWithEditor<GroupPropertyBean> propertiesEditor;
	private GroupPropertyBean edited;
	
	GroupEditDialog(MessageSource msg, Group group, Callback callback)
	{
		super(msg, msg.getMessage("GroupEditDialog.editCaption"), msg.getMessage("ok"), msg.getMessage("cancel"));
		this.originalGroup = group;
		this.callback = callback;
		setSizeEm(60, 40);
	}

	@Override
	protected Component getContents()
	{
		FormLayout fl = new CompactFormLayout();
		fl.setMargin(true);
		fl.setSpacing(true);

		path = new TextField(msg.getMessage("GroupEditDialog.groupPath"));
		path.setValue(originalGroup.getPathEncoded());
		path.setReadOnly(true);
		path.setWidthFull();
		fl.addComponent(path);

		displayedName = new I18nTextField(msg, msg.getMessage("displayedNameF"));
		displayedName.setValue(originalGroup.getDisplayedName());

		description = new I18nTextArea(msg, msg.getMessage("GroupEditDialog.groupDesc"));
		description.setValue(originalGroup.getDescription());

		isPublic = new CheckBox(msg.getMessage("GroupEditDialog.public"));
		isPublic.setValue(originalGroup.isPublic());

		propertiesEditor = new GridWithEditor<>(msg, GroupPropertyBean.class, t -> false, false, false);
		propertiesEditor.setCaption(msg.getMessage("GroupEditDialog.groupProperties"));
		propertiesEditor.getEditor().addOpenListener(e -> edited = e.getBean());
		
		propertiesEditor.addTextColumn(s -> s.key, (t, v) -> t.setKey(v),
				msg.getMessage("GroupEditDialog.propertyName"), 10, true, Optional.of((value, context) ->
				{
					if (propertiesEditor.getValue().stream().filter(k -> !k.key.equals(edited.key)).map(k -> k.key)
							.anyMatch(k -> k.equals(value)))
					{
						return ValidationResult.error(msg.getMessage("GroupEditDialog.propertyNameExists"));
					}
					return new StringLengthValidator(msg.getMessage("maxLength", GroupProperty.MAX_KEY_LENGTH), 0,
							GroupProperty.MAX_KEY_LENGTH).apply(value, context);
				}));
		propertiesEditor.addTextColumn(s -> s.getValue(), (t, v) -> t.setValue(v),
				msg.getMessage("GroupEditDialog.propertyValue"), 40, false,
				Optional.of(new StringLengthValidator(msg.getMessage("maxLength", GroupProperty.MAX_VALUE_LENGHT), 0,
						GroupProperty.MAX_VALUE_LENGHT)));
		propertiesEditor.setValue(originalGroup.getProperties().values().stream().map(p -> new GroupPropertyBean(p))
				.collect(Collectors.toList()));

		fl.addComponents(displayedName, description, isPublic, propertiesEditor);
		description.focus();
		return fl;
	}

	@Override
	protected void onConfirm()
	{
		if(propertiesEditor.isEditMode())
		{
			propertiesEditor.setComponentError(new UserError(msg.getMessage("GroupEditDialog.saveFirst")));
			return;
		}
		
		
		try
		{
			Group group = originalGroup.clone();
			group.setDescription(description.getValue());
			I18nString dispName = displayedName.getValue();
			dispName.setDefaultValue(group.toString());
			group.setDisplayedName(dispName);
			group.setPublic(isPublic.getValue());
			group.setProperties(propertiesEditor.getValue().stream()
					.map(p -> new GroupProperty(p.getKey(), p.getValue())).collect(Collectors.toList()));
			close();
			callback.onConfirm(group);
		} catch (Exception e)
		{
			path.setComponentError(new UserError(msg.getMessage("GroupEditDialog.invalidGroup")));
		}
	}

	interface Callback
	{
		public void onConfirm(Group newGroup);
	}

	public static class GroupPropertyBean
	{
		private String key;
		private String value;

		public GroupPropertyBean()
		{
			key = "";
			value = "";
		}

		private GroupPropertyBean(GroupProperty org)
		{
			key = org.key;
			value = org.value;
		}

		private String getKey()
		{
			return key;
		}

		private void setKey(String key)
		{
			this.key = key;
		}

		private String getValue()
		{
			return value;
		}

		private void setValue(String value)
		{
			this.value = value;
		}
	}
}
