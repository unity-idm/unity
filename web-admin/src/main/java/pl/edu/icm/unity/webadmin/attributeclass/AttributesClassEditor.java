/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;

import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Editing component of an {@link AttributesClass} instance.
 * @author K. Benedyczak
 */
public class AttributesClassEditor extends CompactFormLayout
{
	private UnityMessageSource msg;
	private Map<String, AttributesClass> allClasses;
	private Map<String, AttributeType> types;

	private TextField name;
	private DescriptionTextArea typeDescription;
	private TwinColSelect<String> parents;
	private TwinColSelect<String> allowed;
	private CheckBox allowArbitrary;
	private TwinColSelect<String> mandatory;
	private EffectiveAttrClassViewer effectiveViewer;
	
	private Binder<AttributesClass> binder;
	
	public AttributesClassEditor(UnityMessageSource msg, Map<String, AttributesClass> allClasses,
			Collection<AttributeType> allTypes)
	{
		this.msg = msg;
		this.allClasses = new TreeMap<>(allClasses);
		this.types = new TreeMap<>();
		for (AttributeType at: allTypes)
			if (!at.isInstanceImmutable())
				types.put(at.getName(), at);
		initUI();
	}

	public void setEditedClass(AttributesClass ac)
	{
		binder.setBean(ac);
		name.setReadOnly(true);
		updateEffective();
	}
	
	private void initUI()
	{
		name = new TextField(msg.getMessage("AttributesClass.name"));
		
		typeDescription = new DescriptionTextArea(msg.getMessage("AttributesClass.description"));
		
		parents = new ACTwinColSelect(msg.getMessage("AttributesClass.parents"),
				msg.getMessage("AttributesClass.availableACs"),
				msg.getMessage("AttributesClass.selectedACs"));
		
		allowed = new ACTwinColSelect(msg.getMessage("AttributesClass.allowed"),
				msg.getMessage("AttributesClass.availableAttributes"),
				msg.getMessage("AttributesClass.selectedAttributes"));
		
		allowArbitrary = new CheckBox(msg.getMessage("AttributesClass.allowArbitrary"));
		
		mandatory = new ACTwinColSelect(msg.getMessage("AttributesClass.mandatory"),
				msg.getMessage("AttributesClass.availableAttributes"),
				msg.getMessage("AttributesClass.selectedAttributes"));
		
		effectiveViewer = new EffectiveAttrClassViewer(msg);
		Panel effectiveWrapper = new SafePanel(effectiveViewer);
		effectiveWrapper.setCaption(msg.getMessage("AttributesClass.resultingClass"));
		
		parents.setItems(allClasses.keySet());
		allowed.setItems(types.keySet());
		mandatory.setItems(types.keySet());
		
		binder = new Binder<>(AttributesClass.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(typeDescription, "description");
		binder.bind(allowArbitrary, "allowArbitrary");
		binder.bind(allowed, "allowed");
		binder.bind(mandatory, "mandatory");
		binder.bind(parents, "parentClasses");
		AttributesClass def = new AttributesClass();
		def.setName(msg.getMessage("AttributesClass.defaultName"));
		binder.setBean(def);
		addComponents(name, typeDescription, parents, allowed, allowArbitrary, mandatory, effectiveWrapper);
		effectiveViewer.setVisible(false);
		
		name.addValueChangeListener(event -> updateEffective());
		parents.addValueChangeListener(event -> updateEffective());
		allowed.addValueChangeListener(event -> updateEffective());
		allowArbitrary.addValueChangeListener(event -> updateEffective());
		allowArbitrary.addValueChangeListener(event -> allowed.setEnabled(!allowArbitrary.getValue()));
		mandatory.addValueChangeListener(event -> updateEffective());
	}
	
	private void updateEffective()
	{
		String root = name.getValue();
		if (root == null || root.isEmpty())
			effectiveViewer.setInput(null, allClasses);
		else
		{
			Map<String, AttributesClass> tmp = new HashMap<>(allClasses);
			AttributesClass cur = binder.getBean();
			try
			{
				AttributeClassHelper.cleanupClass(cur, allClasses);
			} catch (IllegalTypeException e)
			{
				throw new IllegalArgumentException(e);
			}
			tmp.put(root, cur);
			effectiveViewer.setInput(root, tmp);
		}
	}
	
	public AttributesClass getAttributesClass() throws FormValidationException
	{
		if (!binder.isValid())
			throw new FormValidationException();
		return binder.getBean();
	}
}
