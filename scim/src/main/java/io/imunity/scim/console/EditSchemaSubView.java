/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import static io.imunity.vaadin.elements.CssClassNames.EDIT_VIEW_ACTION_BUTTONS_LAYOUT;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;

import io.imunity.scim.console.mapping.AttributeDefinitionWithMappingBean;
import io.imunity.scim.console.mapping.SchemaWithMappingBean;
import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.Panel;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

class EditSchemaSubView extends VerticalLayout implements UnitySubView
{
	private final MessageSource msg;
	private final HtmlTooltipFactory htmlTooltipFactory;
	private Binder<SchemaWithMappingBean> binder;
	private boolean editMode = false;
	private AttributesEditMode attributesEditMode;
	private final IdentityTypeSupport identityTypeSupport;
	private final AttributeTypeManagement attributeTypeManagement;

	private EditSchemaSubView(MessageSource msg,NotificationPresenter notificationPresenter, HtmlTooltipFactory htmlTooltipFactory, IdentityTypeSupport identityTypeSupport,
			AttributeTypeManagement attributeTypeManagement, List<String> alreadyUseIds, SchemaWithMappingBean toEdit,
			AttributesEditMode maapingEditMode, Consumer<SchemaWithMappingBean> onConfirm, Runnable onCancel)
			throws EngineException
	{
		this.msg = msg;
		this.htmlTooltipFactory = htmlTooltipFactory;
		this.attributesEditMode = maapingEditMode;
		this.identityTypeSupport = identityTypeSupport;
		this.attributeTypeManagement = attributeTypeManagement;

		editMode = toEdit != null;
		binder = new Binder<>(SchemaWithMappingBean.class);
		
		add(buildHeaderSection(alreadyUseIds));
		add(buildAttributesSection());
		Button cancelButton = new Button(msg.getMessage("cancel"), event -> onCancel.run());
		cancelButton.setWidthFull();
		Button updateButton = new Button(editMode ? msg.getMessage("update") :  msg.getMessage("create"), event ->
		{
			SchemaWithMappingBean schema;
			try
			{
				schema = getSchema();
			} catch (FormValidationException e)
			{
				notificationPresenter.showError(msg.getMessage("EditSchemaSubView.invalidConfiguration"), e.getMessage());
				return;
			}
			onConfirm.accept(schema);
		});
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, updateButton);
		buttonsLayout.setClassName(EDIT_VIEW_ACTION_BUTTONS_LAYOUT.getName());
		add(buttonsLayout);

		binder.setBean(editMode ? toEdit.clone() : new SchemaWithMappingBean());
		
		setSizeFull();
	}

	private FormLayout buildHeaderSection(List<String> alreadyUseIds)
	{
		FormLayout header = new FormLayout();
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());


		TextField id = new TextField();
		id.setReadOnly(!attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
		id.setWidth(CSSVars.TEXT_FIELD_BIG.value());
		header.addFormItem(id, msg.getMessage("EditSchemaSubView.id"));
		binder.forField(id)
				.withValidator((s, c) -> !editMode && alreadyUseIds.contains(s)
						? ValidationResult.error(msg.getMessage("EditSchemaSubView.idExists"))
						: ValidationResult.ok())
				.asRequired(msg.getMessage("fieldRequired")).bind("id");

		TextField name = new TextField();
		name.setWidth(CSSVars.TEXT_FIELD_BIG.value());
		header.addFormItem(name, msg.getMessage("EditSchemaSubView.name"));
		name.setReadOnly(!attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
		binder.forField(name).bind("name");

		TextField desc = new TextField();
		header.addFormItem(desc, msg.getMessage("EditSchemaSubView.description"));
		desc.setWidth(CSSVars.TEXT_FIELD_BIG.value());
		desc.setReadOnly(!attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
		binder.forField(desc).bind("description");

		Checkbox enable = new Checkbox();
		enable.setLabel(msg.getMessage("EditSchemaSubView.enable"));
		header.addFormItem(enable, "");
		binder.forField(enable).bind("enable");
		enable.setReadOnly(attributesEditMode.equals(AttributesEditMode.HIDE_MAPPING));

		return header;
	}

	private Component buildAttributesSection() throws EngineException
	{
		VerticalLayout attributesL = new VerticalLayout();
		attributesL.setMargin(false);
		NativeLabel invalidMappingInfo = new NativeLabel();
		invalidMappingInfo.setWidthFull();
		VerticalLayout wrapper =  new VerticalLayout(invalidMappingInfo);
		HorizontalLayout headerLayout = new HorizontalLayout();
		headerLayout.setWidthFull();
		Span label = new Span(msg.getMessage("AttributeDefinitionConfigurationList.invalidMappingAttributes"));
		headerLayout.addClassName("u-unsaved-banner");
		headerLayout.add(VaadinIcon.EXCLAMATION_CIRCLE_O.create(), label);
		Panel invalidMappingPanel = new Panel(headerLayout);
		invalidMappingPanel.setMargin(false);
		invalidMappingPanel.add(wrapper);
		invalidMappingPanel.setWidthFull();
			
		AttributeDefinitionConfigurationList attributesList = new AttributeDefinitionConfigurationList(msg, htmlTooltipFactory,
				msg.getMessage("AttributeDefinitionConfigurationList.addAttribute"),
				AttributeEditContext.builder().withDisableComplexAndMulti(false)
						.withAttributesEditMode(attributesEditMode).build(),
				AttributeEditorData.builder().withIdentityTypes(getIdentityTypes())
						.withAttributeTypes(getAttributeTypes()).build());
		attributesList.setRequiredIndicatorVisible(false);
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
			invalidMappingInfo.setText(String.join(", ", invalidMappingAttr));

			return (value == null || value.stream().filter(a -> a == null).count() > 0)
					? ValidationResult.error("")
					: ValidationResult.ok();
		}).bind("attributes");
		attributesL.add(invalidMappingPanel);
		attributesL.add(attributesList);

		AccordionPanel attributesSection = new AccordionPanel(msg.getMessage("EditSchemaSubView.attributes"),
				attributesL);
		attributesSection.setOpened(true);
		attributesSection.setWidthFull();
		return attributesSection;
	}

	private List<String> getAttributeTypes() throws EngineException
	{
		return attributeTypeManagement.getAttributeTypes().stream().map(a -> a.getName()).collect(Collectors.toList());
	}

	private List<String> getIdentityTypes() throws EngineException
	{
		return identityTypeSupport.getIdentityTypes().stream()
				.filter(t -> !identityTypeSupport.getTypeDefinition(t.getName()).isTargeted()).map(t -> t.getName())
				.collect(Collectors.toList());
	}

	private SchemaWithMappingBean getSchema() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		return binder.getBean();
	}

	@Override
	public List<String> getBreadcrumbs()
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
		final NotificationPresenter notificationPresenter;
		final HtmlTooltipFactory htmlTooltipFactory;
		final AttributeTypeManagement attributeTypeManagement;
		final IdentityTypeSupport identityTypeSupport;

		EditSchemaSubViewFactory(MessageSource msg, NotificationPresenter notificationPresenter, HtmlTooltipFactory htmlTooltipFactory, AttributeTypeManagement attributeTypeManagement,
				IdentityTypeSupport identityTypeSupport)
		{
			this.msg = msg;
			this.notificationPresenter = notificationPresenter;
			this.htmlTooltipFactory = htmlTooltipFactory;
			this.attributeTypeManagement = attributeTypeManagement;
			this.identityTypeSupport = identityTypeSupport;
		}

		EditSchemaSubView getSubView(List<String> alreadyUseIds, SchemaWithMappingBean toEdit,
				AttributesEditMode attributesEditMode, Consumer<SchemaWithMappingBean> onConfirm, Runnable onCancel)
				throws EngineException
		{
			return new EditSchemaSubView(msg, notificationPresenter, htmlTooltipFactory, identityTypeSupport, attributeTypeManagement, alreadyUseIds, toEdit,
					attributesEditMode, onConfirm, onCancel);
		}

	}

}
