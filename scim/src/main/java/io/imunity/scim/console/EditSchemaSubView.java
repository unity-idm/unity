/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

class EditSchemaSubView extends CustomComponent implements UnitySubView
{
	private MessageSource msg;
	private Binder<SchemaWithMappingBean> binder;
	private boolean editMode = false;
	private AttributesEditMode attributesEditMode;
	private final IdentityTypeSupport identityTypeSupport;
	private final AttributeTypeManagement attributeTypeManagement;

	private EditSchemaSubView(MessageSource msg, IdentityTypeSupport identityTypeSupport,
			AttributeTypeManagement attributeTypeManagement, List<String> alreadyUseIds, SchemaWithMappingBean toEdit,
			AttributesEditMode maapingEditMode, Consumer<SchemaWithMappingBean> onConfirm, Runnable onCancel)
			throws EngineException
	{
		this.msg = msg;
		this.attributesEditMode = maapingEditMode;
		this.identityTypeSupport = identityTypeSupport;
		this.attributeTypeManagement = attributeTypeManagement;

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
				.asRequired(msg.getMessage("fieldRequired")).bind("id");

		TextField name = new TextField(msg.getMessage("EditSchemaSubView.name"));
		header.addComponent(name);
		name.setReadOnly(!attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
		binder.forField(name).bind("name");

		TextField desc = new TextField(msg.getMessage("EditSchemaSubView.description"));
		header.addComponent(desc);
		desc.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		desc.setReadOnly(!attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
		binder.forField(desc).bind("description");

		CheckBox enable = new CheckBox(msg.getMessage("EditSchemaSubView.enable"));
		header.addComponent(enable);
		binder.forField(enable).bind("enable");
		enable.setReadOnly(attributesEditMode.equals(AttributesEditMode.HIDE_MAPPING));

		return header;
	}

	private Component buildAttributesSection() throws EngineException
	{
		VerticalLayout attributesL = new VerticalLayout();
		attributesL.setMargin(false);
		Label invalidMappingInfo = new Label();
		invalidMappingInfo.setWidth(100, Unit.PERCENTAGE);
		invalidMappingInfo.addStyleName(Styles.wordWrap.toString());
		VerticalLayout wrapper =  new VerticalLayout(invalidMappingInfo);
		wrapper.addStyleName(Styles.background.toString());
		Panel invalidMappingPanel = new Panel(wrapper);
		invalidMappingPanel.addStyleName(Styles.warnBackground.toString());
		invalidMappingPanel.addStyleName(Styles.vPanelWell.toString());	
		invalidMappingPanel.setWidth(100, Unit.PERCENTAGE);
		invalidMappingPanel.setCaption(msg.getMessage("AttributeDefinitionConfigurationList.invalidMappingAttributes"));
		invalidMappingPanel.setIcon(Images.warn.getResource());
			
		AttributeDefinitionConfigurationList attributesList = new AttributeDefinitionConfigurationList(msg,
				msg.getMessage("AttributeDefinitionConfigurationList.addAttribute"),
				AttributeEditContext.builder().withDisableComplexAndMulti(false)
						.withAttributesEditMode(attributesEditMode).build(),
				AttributeEditorData.builder().withIdentityTypes(getIdentityTypes())
						.withAttributeTypes(getAttributeTypes()).build());
		binder.forField(attributesList).withValidator((value, context) ->
		{
			List<String> invalidMappingAttr = new ArrayList<>();
			if (value != null)
			{
				for (AttributeDefinitionWithMappingBean bean : value)
				{
					if (bean != null)
					{
						invalidMappingAttr.addAll(bean.inferAttributeNamesWithInvalidMapping());
					}
				}
			}
			invalidMappingPanel.setVisible(
					!invalidMappingAttr.isEmpty() && !attributesEditMode.equals(AttributesEditMode.HIDE_MAPPING));
			invalidMappingInfo.setValue(String.join(", ", invalidMappingAttr));

			return (value == null || value.stream().filter(a -> a == null).count() > 0)
					? ValidationResult.error(msg.getMessage("fieldRequired"))
					: ValidationResult.ok();
		}).bind("attributes");
		attributesL.addComponent(invalidMappingPanel);
		attributesL.addComponent(attributesList);

		CollapsibleLayout attributesSection = new CollapsibleLayout(msg.getMessage("EditSchemaSubView.attributes"),
				attributesL);
		attributesSection.expand();
		return attributesSection;
	}

	private List<String> getAttributeTypes() throws EngineException
	{
		return attributeTypeManagement.getAttributeTypes().stream().map(a -> a.getName()).collect(Collectors.toList());
	}

	private List<String> getIdentityTypes() throws EngineException
	{
		return identityTypeSupport.getIdentityTypes().stream()
				.filter(t -> !identityTypeSupport.getTypeDefinition(t.getName()).isDynamic()).map(t -> t.getName())
				.collect(Collectors.toList());
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

	@org.springframework.stereotype.Component
	static class EditSchemaSubViewFactory
	{
		final MessageSource msg;
		final AttributeTypeManagement attributeTypeManagement;
		final IdentityTypeSupport identityTypeSupport;

		EditSchemaSubViewFactory(MessageSource msg, AttributeTypeManagement attributeTypeManagement,
				IdentityTypeSupport identityTypeSupport)
		{
			this.msg = msg;
			this.attributeTypeManagement = attributeTypeManagement;
			this.identityTypeSupport = identityTypeSupport;
		}

		EditSchemaSubView getSubView(List<String> alreadyUseIds, SchemaWithMappingBean toEdit,
				AttributesEditMode attributesEditMode, Consumer<SchemaWithMappingBean> onConfirm, Runnable onCancel)
				throws EngineException
		{
			return new EditSchemaSubView(msg, identityTypeSupport, attributeTypeManagement, alreadyUseIds, toEdit,
					attributesEditMode, onConfirm, onCancel);
		}

	}

}
