/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.attribute_types;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.elements.LocalizedTextAreaDetails;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.Panel;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeSyntaxEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors.IntegerBoundEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.metadata.AttributeMetadataHandlerRegistry;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;

/**
 * Allows to edit an attribute type. Can be configured to edit an existing
 * attribute (name is fixed) or to create a new one (name can be chosen).
 * 
 * @author P.Piernik
 */
class RegularAttributeTypeEditor extends FormLayout implements AttributeTypeEditor
{
	private final MessageSource msg;
	private final AttributeHandlerRegistry registry;
	private final AttributeMetadataHandlerRegistry attrMetaHandlerReg;
	
	private Binder<AttributeType> binder;

	private TextField name;
	private LocalizedTextFieldDetails displayedName;
	private LocalizedTextAreaDetails typeDescription;
	private IntegerField min;
	private IntegerBoundEditor max;
	private Checkbox uniqueVals;
	private Checkbox selfModificable;
	private Checkbox global;
	private ComboBox<String> syntax;
	private VerticalLayout syntaxPanel;
	private AttributeSyntaxEditor<?> editor;
	private MetadataEditor metaEditor;
	private AttributeTypeSupport atSupport;

	RegularAttributeTypeEditor(MessageSource msg, AttributeHandlerRegistry registry,
			AttributeMetadataHandlerRegistry attrMetaHandlerReg, AttributeTypeSupport atSupport)
	{
		this(msg, registry, null, attrMetaHandlerReg, atSupport);
	}

	RegularAttributeTypeEditor(MessageSource msg, AttributeHandlerRegistry registry, AttributeType toEdit,
			AttributeMetadataHandlerRegistry attrMetaHandlerReg, AttributeTypeSupport atSupport)
	{
		super();
		this.msg = msg;
		this.registry = registry;
		this.attrMetaHandlerReg = attrMetaHandlerReg;
		this.atSupport = atSupport;
		
		initUI(toEdit);
	}

	private void initUI(AttributeType toEdit)
	{
		setWidthFull();
		setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		addClassName("u-big-vaadin-form-item");
		
		
		name = new TextField();
		if (toEdit != null)
			name.setReadOnly(true);
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		addFormItem(name, msg.getMessage("AttributeType.name"));

		displayedName = new LocalizedTextFieldDetails(new HashSet<>(msg.getEnabledLocales()
				.values()), msg.getLocale(), Optional.empty(), locale -> "");
		displayedName.setWidth(TEXT_FIELD_BIG.value());
		addFormItem(displayedName, msg.getMessage("AttributeType.displayedName"));

		typeDescription = new LocalizedTextAreaDetails(new HashSet<>(msg.getEnabledLocales()
				.values()), msg.getLocale(), Optional.empty(), locale -> "");
		typeDescription.setWidthFull();

		addFormItem(typeDescription, msg.getMessage("AttributeType.description"));

		min = new IntegerField();
		min.setMin(0);
		min.setStepButtonsVisible(true);
		addFormItem(min, msg.getMessage("AttributeType.min"));

		max = new IntegerBoundEditor(msg, msg.getMessage("AttributeType.maxUnlimited"), Integer.MAX_VALUE, 0, null);
		addFormItem(max, msg.getMessage("AttributeType.max"));

		uniqueVals = new Checkbox(msg.getMessage("AttributeType.uniqueValuesCheck"));
		addFormItem(uniqueVals, "");

		selfModificable = new Checkbox(msg.getMessage("AttributeType.selfModificableCheck"));
		addFormItem(selfModificable, "");

		global = new Checkbox(msg.getMessage("AttributeType.global"));
		addFormItem(global, "");

		syntax = new ComboBox<>();
		SortedSet<String> syntaxes = new TreeSet<>(registry.getSupportedSyntaxes());
		syntax.setItems(syntaxes);
		addFormItem(syntax, msg.getMessage("AttributeType.type"));

		Panel syntaxPanelP = new Panel();
		syntaxPanelP.setMargin(false);

		syntaxPanel = new VerticalLayout();
		syntaxPanel.setSpacing(false);
		syntaxPanel.setMargin(false);
		syntaxPanelP.add(syntaxPanel);

		addFormItem(syntaxPanelP, "");

		syntax.addValueChangeListener(event ->
		{
			String syntaxId = (String) syntax.getValue();
			editor = registry.getSyntaxEditor(syntaxId, null);
			syntaxPanel.removeAll();
			syntaxPanel.add(editor.getEditor());
		});

		metaEditor = new MetadataEditor(msg, attrMetaHandlerReg);
		metaEditor.setMargin(true);
		Panel metaPanel = new Panel();
		metaPanel.add(metaEditor);
		metaPanel.setMargin(false);
		
		addFormItem(metaPanel, msg.getMessage("AttributeType.metadata"));

		binder = new Binder<>(AttributeType.class);
		binder.forField(name)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind("name");
		binder.forField(displayedName)
				.bind(i18nMessage -> i18nMessage.getDisplayedName()
						.getLocalizedMap(),
						(localizedValues, localizedValues2) -> localizedValues
								.setDisplayedName(convert(localizedValues2)));

		binder.forField(typeDescription)
				.bind(i18nMessage -> i18nMessage.getDescription()
						.getLocalizedMap(),
						(localizedValues, localizedValues2) -> localizedValues
								.setDescription(convert(localizedValues2)));

		binder.forField(min)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind("minElements");
		max.configureBinding(binder, "maxElements");
		binder.bind(uniqueVals, "uniqueValues");
		binder.bind(selfModificable, "selfModificable");
		binder.bind(global, "global");
		binder.bind(syntax, "valueSyntax");
		// note syntax editor and metadata are not bound.

		if (toEdit != null)
		{
			binder.setBean(toEdit);
			setInitialValues(toEdit);
		} else
		{
			AttributeType def = new AttributeType();
			def.setName(msg.getMessage("AttributeType.defaultName"));
			def.setMaxElements(1);
			def.setMinElements(1);
			def.setValueSyntax(syntaxes.first());
			def.setDescription(new I18nString());
			binder.setBean(def);
		}
	}

	private I18nString convert(Map<Locale, String> localizedValues)
	{
		I18nString i18nString = new I18nString();
		i18nString.addAllValues(localizedValues.entrySet()
				.stream()
				.collect(Collectors.toMap(x -> x.getKey()
						.toString(), Map.Entry::getValue)));
		return i18nString;
	}

	void setCopyMode()
	{
		name.setReadOnly(false);
		String old = name.getValue();
		name.setValue(msg.getMessage("AttributeType.nameCopy", old));
	}

	private void setInitialValues(AttributeType aType)
	{
		String syntaxId = aType.getValueSyntax();
		AttributeValueSyntax<?> syntaxObj = atSupport.getSyntax(aType);
		editor = registry.getSyntaxEditor(syntaxId, syntaxObj);
		syntaxPanel.removeAll();
		syntaxPanel.add(editor.getEditor());
		metaEditor.setInput(aType.getMetadata());
	}

	@Override
	public AttributeType getAttributeType() throws IllegalAttributeTypeException
	{
		if (!binder.isValid())
			throw new IllegalAttributeTypeException("");
		AttributeValueSyntax<?> syntax = editor.getCurrentValue();
		AttributeType ret = binder.getBean();
		if (ret.getDisplayedName()
				.getMap()
				.isEmpty())
			ret.setDisplayedName(new I18nString(ret.getName()));
		ret.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		ret.setMetadata(metaEditor.getValue());
		return ret;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}
}
