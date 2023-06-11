/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.attributeClasses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vaadin.ui.TwinColSelect;

import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.webui.common.AbstractDialog;


/**
 * Base of dialogs allowing for editing group or entity {@link AttributesClass}es. 
 */
public abstract class AbstractAttributesClassesDialog extends AbstractDialog 
{
	protected final static Set<String> EMPTY = new HashSet<>(0);
	protected AttributeClassManagement acMan;
	protected GroupsManagement groupsMan;
	protected String groupPath;
	protected TwinColSelect<String> acs;
	protected EffectiveAttrClassViewer effective;
	protected Map<String, AttributesClass> allClasses;
	
	public AbstractAttributesClassesDialog(MessageSource msg, String group, 
			AttributeClassManagement acMan, GroupsManagement groupsMan, String caption)
	{
		super(msg, caption);
		this.acMan = acMan;
		this.groupsMan = groupsMan;
		this.groupPath = group;
		setSize(50, 90);
	}

	protected void loadACsData() throws EngineException
	{
		allClasses = acMan.getAttributeClasses();
		acs.setItems(allClasses.keySet());
	}
	
	protected void updateEffective()
	{
		String rootClass = msg.getMessage("AttributesClasses.metaEffectiveClassName");
		while (allClasses.keySet().contains(rootClass))
			rootClass = " " + rootClass;
		Set<String> parents = acs.getValue();
		AttributesClass virtual = new AttributesClass(rootClass, "", EMPTY, EMPTY, 
				parents.isEmpty() ? true : false, parents);
		Map<String, AttributesClass> tmpClasses = new HashMap<>(allClasses);
		tmpClasses.put(rootClass, virtual);
		effective.setInput(rootClass, tmpClasses);
	}
}
