/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.attribute_classes;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.Panel;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

@PermitAll
@Route(value = "/attribute-classes/edit", layout = ConsoleMenu.class)
public class AttributeClassesEditView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AttributeClassController controller;
	private List<String> allAttributeTypes;
	private Map<String, AttributesClass> allAttributeClasses;
	private BreadCrumbParameter breadCrumbParameter;
	private Binder<AttributesClass> binder;
	private EffectiveAttrClassViewer effectiveViewer;
	private boolean edit;

	AttributeClassesEditView(MessageSource msg, AttributeClassController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String attributeClassName)
	{
		getContent().removeAll();

		AttributesClass attributesClass;
		if (attributeClassName == null)
		{
			attributesClass = new AttributesClass();
			breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
			edit = false;
		} else
		{
			attributesClass = controller.getAttributeClass(attributeClassName);
			breadCrumbParameter = new BreadCrumbParameter(attributeClassName, attributeClassName);
			edit = true;
		}
		allAttributeTypes = controller.getAllAttributeTypes();
		allAttributeClasses = controller.getAllAttributeClasses();
		initUI(attributesClass);
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	private void initUI(AttributesClass attributesClass)
	{
		TextField name = new TextField();
		name.setPlaceholder(msg.getMessage("AttributesClass.defaultName"));
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		name.setReadOnly(edit);

		TextField typeDescription = new TextField();
		typeDescription.setWidth(TEXT_FIELD_BIG.value());

		MultiSelectComboBox<String> parents = new MultiSelectComboBox<>();
		parents.setWidth(TEXT_FIELD_BIG.value());

		MultiSelectComboBox<String> allowed = new MultiSelectComboBox<>();
		allowed.setWidth(TEXT_FIELD_BIG.value());

		Checkbox allowArbitrary = new Checkbox(msg.getMessage("AttributesClass.allowArbitrary"));

		MultiSelectComboBox<String> mandatory = new MultiSelectComboBox<>();
		mandatory.setWidth(TEXT_FIELD_BIG.value());

		effectiveViewer = new EffectiveAttrClassViewer(msg);

		FormLayout mainLayout = new FormLayout();
		mainLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		mainLayout.addFormItem(name, msg.getMessage("AttributesClass.name"));
		mainLayout.addFormItem(typeDescription, msg.getMessage("AttributesClass.description"));
		mainLayout.addFormItem(parents, msg.getMessage("AttributesClass.parents"));
		mainLayout.addFormItem(allowed, msg.getMessage("AttributesClass.allowed"));
		mainLayout.addFormItem(allowArbitrary, "");
		mainLayout.addFormItem(mandatory, msg.getMessage("AttributesClass.mandatory"));

		Panel panel = new Panel(msg.getMessage("AttributesClass.resultingClass"));
		panel.setSizeFull();
		panel.setMargin(false);
		panel.add(effectiveViewer);
		mainLayout.addFormItem(panel, "");

		parents.setItems(allAttributeClasses.keySet());
		allowed.setItems(allAttributeTypes);
		mandatory.setItems(allAttributeTypes);

		binder = new Binder<>(AttributesClass.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(typeDescription, "description");
		binder.bind(allowArbitrary, "allowArbitrary");
		binder.bind(allowed, "allowed");
		binder.bind(mandatory, "mandatory");
		binder.bind(parents, "parentClasses");
		binder.setBean(attributesClass);
		effectiveViewer.setVisible(false);

		name.addValueChangeListener(event -> updateEffective());
		parents.addValueChangeListener(event -> updateEffective());
		allowed.addValueChangeListener(event -> updateEffective());
		allowArbitrary.addValueChangeListener(event -> updateEffective());
		allowArbitrary.addValueChangeListener(event -> allowed.setEnabled(!allowArbitrary.getValue()));
		mandatory.addValueChangeListener(event -> updateEffective());

		getContent().add(
				new VerticalLayout(mainLayout, createActionLayout(msg, edit, AttributeClassesView.class, this::onConfirm)));
		if(edit)
			updateEffective();
	}

	private void updateEffective()
	{
		String root = binder.getBean().getName();
		if (root == null || root.isEmpty())
			effectiveViewer.setInput(null, allAttributeClasses);
		else
		{
			Map<String, AttributesClass> tmp = new HashMap<>(allAttributeClasses);
			AttributesClass cur = binder.getBean();
			try
			{
				AttributeClassHelper.cleanupClass(cur, allAttributeClasses);
			} catch (IllegalTypeException e)
			{
				throw new IllegalArgumentException(e);
			}
			tmp.put(root, cur);
			effectiveViewer.setInput(root, tmp);
		}
	}

	private void onConfirm()
	{
		binder.validate();
		if (binder.isValid())
		{
			AttributesClass bean = binder.getBean();
			if (edit)
				controller.updateAttributeClass(bean);
			else
				controller.addAttributeClass(bean);
			UI.getCurrent().navigate(AttributeClassesView.class);
		}
	}
}
