/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_browser;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.validator.StringLengthValidator;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.LocalizedTextAreaDetails;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupProperty;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.END;
import static com.vaadin.flow.data.value.ValueChangeMode.EAGER;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;

class GroupEditDialog extends DialogWithActionFooter
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, GroupEditDialog.class);

	private final MessageSource msg;
	private final Callback callback;
	private final Group originalGroup;
	private TextField path;
	private LocalizedTextFieldDetails displayedName;
	private LocalizedTextAreaDetails description;
	private Checkbox isPublic;
	private List<GroupPropertyBean> groupPropertyBeans;
	private Grid<GroupPropertyBean> propertiesGrid;
	private GroupPropertyBean edited;
	
	GroupEditDialog(MessageSource msg, Group group, Callback callback)
	{
		super(msg::getMessage);
		this.msg = msg;
		this.originalGroup = group;
		this.callback = callback;
		setWidth("60em");
		setHeight("60em");
		setActionButton(msg.getMessage("ok"), this::onConfirm);
		add(getContents());
	}

	private Component getContents()
	{
		FormLayout fl = new FormLayout();
		fl.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		path = new TextField();
		path.setValue(originalGroup.getPathEncoded());
		path.setEnabled(false);
		path.setWidth(TEXT_FIELD_MEDIUM.value());

		displayedName = new LocalizedTextFieldDetails(new HashSet<>(msg.getEnabledLocales().values()), msg.getLocale());
		displayedName.setWidthFull();
		description = new LocalizedTextAreaDetails(new HashSet<>(msg.getEnabledLocales().values()), msg.getLocale());
		description.setWidthFull();

		isPublic = new Checkbox(msg.getMessage("GroupEditDialog.public"));
		isPublic.setValue(originalGroup.isPublic());

		propertiesGrid = new Grid<>();
		propertiesGrid.setHeight("16em");
		Editor<GroupPropertyBean> editor = propertiesGrid.getEditor();
		editor.addOpenListener(e -> edited = e.getItem());
		editor.setBinder(new Binder<>());

		propertiesGrid.addColumn(s -> s.key)
						.setHeader(msg.getMessage("GroupEditDialog.propertyName"))
						.setEditorComponent(getKeyEditorComponent(editor));
		propertiesGrid.addColumn(GroupPropertyBean::getValue)
						.setHeader(msg.getMessage("GroupEditDialog.propertyValue"))
						.setEditorComponent(getValueEditorComponent(editor));
		propertiesGrid.addComponentColumn(bean -> {
					Icon icon = VaadinIcon.TRASH.create();
					icon.addClassName(POINTER.getName());
					icon.addClickListener(event ->
					{
						groupPropertyBeans.remove(bean);
						propertiesGrid.getDataProvider().refreshAll();
					});
					return icon;
				})
				.setHeader(msg.getMessage("actions"))
				.setEditorComponent(addEditButtons(editor))
				.setTextAlign(END);

		groupPropertyBeans = originalGroup.getProperties().values().stream().map(GroupPropertyBean::new)
				.collect(Collectors.toList());
		propertiesGrid.setItems(groupPropertyBeans);

		fl.addFormItem(path, msg.getMessage("GroupEditDialog.groupPath"));
		fl.addFormItem(displayedName, msg.getMessage("displayedNameF"));
		fl.addFormItem(description, msg.getMessage("GroupEditDialog.groupDesc"));
		fl.addFormItem(isPublic, "");
		Button add = new Button(msg.getMessage("add"), VaadinIcon.PLUS_CIRCLE_O.create(), e ->
		{
			if (editor.isOpen())
				editor.cancel();
			edited = new GroupPropertyBean();
			groupPropertyBeans.add(edited);
			propertiesGrid.getDataProvider().refreshAll();
			editor.editItem(edited);
		});
		fl.addFormItem(getField(add), msg.getMessage("GroupEditDialog.groupProperties"));
		description.focus();
		description.setValue(originalGroup.getDescription().getLocalizedMap());
		displayedName.setValue(originalGroup.getDisplayedName().getLocalizedMap());

		return fl;
	}

	private VerticalLayout getField(Button add)
	{
		VerticalLayout verticalLayout = new VerticalLayout(add, propertiesGrid);
		verticalLayout.setAlignItems(FlexComponent.Alignment.END);
		return verticalLayout;
	}

	Component getKeyEditorComponent(Editor<GroupPropertyBean> editor)
	{
		TextField field = new TextField();
		field.setValueChangeMode(EAGER);
		editor.getBinder().forField(field)
				.withValidator((value, context) ->
				{
					if (groupPropertyBeans.stream().filter(bean -> !bean.key.equals(edited.key))
							.map(bean -> bean.key)
							.anyMatch(key -> key.equals(value)))
					{
						return ValidationResult.error(msg.getMessage("GroupEditDialog.propertyNameExists"));
					}
					return new StringLengthValidator(msg.getMessage("lengthBound", 1, GroupProperty.MAX_KEY_LENGTH), 1,
							GroupProperty.MAX_KEY_LENGTH).apply(value, context);
				})
				.bind(GroupPropertyBean::getKey, GroupPropertyBean::setKey);
		return field;
	}

	Component getValueEditorComponent(Editor<GroupPropertyBean> editor)
	{
		TextField field = new TextField();
		field.setValueChangeMode(EAGER);
		editor.getBinder().forField(field)
				.withValidator(new StringLengthValidator(msg.getMessage("maxLength", GroupProperty.MAX_VALUE_LENGHT), 0,
						GroupProperty.MAX_VALUE_LENGHT))
				.bind(GroupPropertyBean::getValue, GroupPropertyBean::setValue);
		return field;
	}

	private Component addEditButtons(Editor<GroupPropertyBean> editor)
	{
		Button save = new Button(msg.getMessage("save"), e ->
		{
			editor.save();
			editor.cancel();
		});
		save.addThemeVariants(LUMO_TERTIARY);
		save.addClickShortcut(Key.ENTER);

		Button cancel = new Button(msg.getMessage("cancel"), e ->
		{
			groupPropertyBeans.remove(edited);
			propertiesGrid.getDataProvider().refreshAll();
			edited = null;
			editor.cancel();
		});
		cancel.addThemeVariants(LUMO_TERTIARY);

		editor.getBinder()
				.addStatusChangeListener(status -> save.setEnabled(!status.hasValidationErrors()));
		editor.addOpenListener(e ->
		{
			save.setEnabled(false);
			setActionButtonVisible(false);
		});
		editor.addCloseListener(e -> setActionButtonVisible(true));

		return new Div(save, cancel);
	}

	private void onConfirm()
	{
		try
		{
			Group group = originalGroup.clone();
			I18nString i18nDescription = new I18nString();
			i18nDescription.addAllMapValues(description.getValue());
			group.setDescription(i18nDescription);

			I18nString dispName = new I18nString();
			dispName.addAllMapValues(displayedName.getValue());
			dispName.setDefaultValue(group.toString());
			group.setDisplayedName(dispName);

			group.setPublic(isPublic.getValue());
			group.setProperties(groupPropertyBeans.stream()
					.map(p -> new GroupProperty(p.getKey(), p.getValue())).collect(Collectors.toList()));
			close();
			callback.onConfirm(group);
		} catch (Exception e)
		{
			LOG.error(e);
			path.setInvalid(true);
			path.setErrorMessage(msg.getMessage("GroupEditDialog.invalidGroup"));
		}
	}

	interface Callback
	{
		void onConfirm(Group newGroup);
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
