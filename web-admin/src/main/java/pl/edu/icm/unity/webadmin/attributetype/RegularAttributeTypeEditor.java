/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.SortedSet;
import java.util.TreeSet;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataHandlerRegistry;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Allows to edit an attribute type. Can be configured to edit an existing attribute (name is fixed)
 * or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
public class RegularAttributeTypeEditor extends FormLayout implements AttributeTypeEditor
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry registry;
	private AttributeMetadataHandlerRegistry attrMetaHandlerReg;
	
	private Binder<AttributeType> binder;
	
	private AbstractTextField name;
	private I18nTextField displayedName;
	private I18nTextArea typeDescription;
	private TextField min;
	private IntegerBoundEditor max;
	private CheckBox uniqueVals;
	private CheckBox selfModificable;
	private ComboBox<String> syntax;
	private VerticalLayout syntaxPanel;
	private AttributeSyntaxEditor<?> editor;
	private MetadataEditor metaEditor;
	private AttributeTypeSupport atSupport;
	
	public RegularAttributeTypeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			AttributeMetadataHandlerRegistry attrMetaHandlerReg, AttributeTypeSupport atSupport)
	{
		this(msg, registry, null, attrMetaHandlerReg, atSupport);
	}

	public RegularAttributeTypeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, AttributeType toEdit, 
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
		setWidth(100, Unit.PERCENTAGE);

		name = new TextField(msg.getMessage("AttributeType.name"));
		if (toEdit != null)
			name.setReadOnly(true);
		addComponent(name);
		
		displayedName = new I18nTextField(msg, msg.getMessage("AttributeType.displayedName"));
		addComponent(displayedName);
		
		typeDescription = new I18nTextArea(msg, msg.getMessage("AttributeType.description"));
		addComponent(typeDescription);
		
		min = new TextField(msg.getMessage("AttributeType.min"));
		addComponent(min);

		max = new IntegerBoundEditor(msg, msg.getMessage("AttributeType.maxUnlimited"), 
				msg.getMessage("AttributeType.max"), Integer.MAX_VALUE, 0, null);
		addComponent(max);
		
		uniqueVals = new CheckBox(msg.getMessage("AttributeType.uniqueValuesCheck"));
		addComponent(uniqueVals);
		
		selfModificable = new CheckBox(msg.getMessage("AttributeType.selfModificableCheck"));
		addComponent(selfModificable);
		
		syntax = new ComboBox<>(msg.getMessage("AttributeType.type"));
		syntax.setEmptySelectionAllowed(false);
		SortedSet<String> syntaxes = new TreeSet<>(registry.getSupportedSyntaxes());
		syntax.setItems(syntaxes);
		addComponent(syntax);
		
		Panel syntaxPanelP = new SafePanel();
		syntaxPanel = new VerticalLayout();
		syntaxPanel.setSpacing(false);
		syntaxPanelP.setContent(syntaxPanel);
		
		addComponent(syntaxPanelP);
		
		syntax.addValueChangeListener(event ->
		{
			String syntaxId = (String)syntax.getValue();
			editor = registry.getSyntaxEditor(syntaxId, null);
			syntaxPanel.removeAllComponents();
			syntaxPanel.addComponent(editor.getEditor());
		});
		
		metaEditor = new MetadataEditor(msg, attrMetaHandlerReg);
		metaEditor.setMargin(true);
		Panel metaPanel = new SafePanel(msg.getMessage("AttributeType.metadata"), metaEditor);
		addComponent(metaPanel);
		
		binder = new Binder<>(AttributeType.class);
		binder.forField(name)
			.asRequired(msg.getMessage("fieldRequired"))
			.bind("name");
		binder.bind(displayedName, "displayedName");
		binder.bind(typeDescription, "description");
		binder.forField(min)
			.asRequired(msg.getMessage("fieldRequired"))
			.withConverter(new StringToIntegerConverter(msg.getMessage("IntegerBoundEditor.notANumber")))
			.withValidator(new IntegerRangeValidator(msg.getMessage("AttributeType.invalidNumber"), 
					0, Integer.MAX_VALUE))
			.bind("minElements");
		max.configureBinding(binder, "maxElements");
		binder.bind(uniqueVals, "uniqueValues");
		binder.bind(selfModificable, "selfModificable");
		binder.bind(syntax, "valueSyntax");
		//note syntax editor and metadata are not bound.
		
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
			binder.setBean(def);
		}
	}
	
	public void setCopyMode()
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
		syntaxPanel.removeAllComponents();
		syntaxPanel.addComponent(editor.getEditor());
		metaEditor.setInput(aType.getMetadata());
	}
	
	@Override
	public AttributeType getAttributeType() throws IllegalAttributeTypeException
	{
		if (!binder.isValid())
			throw new IllegalAttributeTypeException("");
		AttributeValueSyntax<?> syntax = editor.getCurrentValue();
		AttributeType ret = binder.getBean();
		if (ret.getDisplayedName().getMap().isEmpty())
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
