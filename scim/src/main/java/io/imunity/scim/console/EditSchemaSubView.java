/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

class EditSchemaSubView extends CustomComponent implements UnitySubView
{
	private MessageSource msg;
	private Binder<SchemaWithMappingBean> binder;
	private boolean editMode = false;
	private boolean onlyMappingEdit;

	EditSchemaSubView(MessageSource msg, List<String> alreadyUseIds, SchemaWithMappingBean toEdit,
			boolean onlyMappingEdit, Consumer<SchemaWithMappingBean> onConfirm, Runnable onCancel)
	{
		this.msg = msg;
		this.onlyMappingEdit = onlyMappingEdit;
		editMode = toEdit != null;
		binder = new Binder<>(SchemaWithMappingBean.class);
		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(buildHeaderSection(alreadyUseIds));
		mainView.addComponent(buildAttributesSection());
		Runnable onConfirmR = () ->
		{
			SchemaWithMappingBean schema;
			try
			{
				schema = getSchema();
			} catch (FormValidationException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("EditSchemaSubView.invalidConfiguration"), e);
				return;
			}
			onConfirm.accept(schema);
		};
		mainView.addComponent(editMode ? StandardButtonsHelper.buildConfirmEditButtonsBar(msg, onConfirmR, onCancel)
				: StandardButtonsHelper.buildConfirmNewButtonsBar(msg, onConfirmR, onCancel));

		binder.setBean(editMode ? toEdit.clone() : new SchemaWithMappingBean());
		setCompositionRoot(mainView);
	}

	private FormLayoutWithFixedCaptionWidth buildHeaderSection(List<String> alreadyUseIds)
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);

		TextField id = new TextField(msg.getMessage("EditSchemaSubView.id"));
		id.setReadOnly(editMode);
		id.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		header.addComponent(id);
		binder.forField(id)
				.withValidator((s, c) -> !editMode && alreadyUseIds.contains(s)
						? ValidationResult.error(msg.getMessage("EditSchemaSubView.idExists"))
						: ValidationResult.ok())
				.bind("id");

		TextField name = new TextField(msg.getMessage("EditSchemaSubView.name"));
		header.addComponent(name);
		name.setReadOnly(onlyMappingEdit);
		binder.forField(name).bind("name");

		TextField desc = new TextField(msg.getMessage("EditSchemaSubView.description"));
		header.addComponent(desc);
		desc.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		desc.setReadOnly(onlyMappingEdit);
		binder.forField(desc).bind("description");

		CheckBox enable = new CheckBox(msg.getMessage("EditSchemaSubView.enable"));
		header.addComponent(enable);
		binder.forField(enable).bind("enable");

		return header;
	}

	private Component buildAttributesSection()
	{
		VerticalLayout attributesL = new VerticalLayout();
		attributesL.setMargin(false);

		AttributeDefinitionConfigurationList attributesList = new AttributeDefinitionConfigurationList(msg,
				msg.getMessage("AttributeDefinitionConfigurationList.addAttribute"), false, onlyMappingEdit);
		binder.forField(attributesList)
				.withValidator((value, context) -> (value == null || value.stream().filter(a -> a == null).count() > 0)
						? ValidationResult.error(msg.getMessage("fieldRequired"))
						: ValidationResult.ok())
				.bind("attributes");
		attributesL.addComponent(attributesList);

		CollapsibleLayout attributesSection = new CollapsibleLayout(msg.getMessage("EditSchemaSubView.attributes"),
				attributesL);
		attributesSection.expand();
		return attributesSection;
	}

	private SchemaWithMappingBean getSchema() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		return binder.getBean();
	}

	@Override
	public List<String> getBredcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditSchemaSubView.schema"), binder.getBean().getId());
		else
			return Arrays.asList(msg.getMessage("EditSchemaSubView.newSchema"));
	}
}
