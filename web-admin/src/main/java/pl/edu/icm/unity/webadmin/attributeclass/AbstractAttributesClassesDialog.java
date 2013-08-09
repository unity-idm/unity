/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vaadin.ui.TwinColSelect;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.webadmin.attributeclass.EffectiveAttrClassViewer;
import pl.edu.icm.unity.webadmin.groupdetails.GroupAttributesClassesDialog;
import pl.edu.icm.unity.webadmin.identities.EntityAttributesClassesDialog;
import pl.edu.icm.unity.webui.common.AbstractDialog;


/**
 * Base of dialogs allowing for editing group or entity {@link AttributesClass}es. 
 * See {@link GroupAttributesClassesDialog} and {@link EntityAttributesClassesDialog}.
 * 
 * @author K. Benedyczak
 */
public abstract class AbstractAttributesClassesDialog extends AbstractDialog 
{
	protected final static Set<String> EMPTY = new HashSet<String>(0);
	protected AttributesManagement attrMan;
	protected GroupsManagement groupsMan;
	protected String groupPath;
	protected TwinColSelect acs;
	protected EffectiveAttrClassViewer effective;
	protected Map<String, AttributesClass> allClasses;
	
	public AbstractAttributesClassesDialog(UnityMessageSource msg, String group, 
			AttributesManagement attrMan, GroupsManagement groupsMan, String caption)
	{
		super(msg, caption);
		this.attrMan = attrMan;
		this.groupsMan = groupsMan;
		this.groupPath = group;
		setWidth(60, Unit.PERCENTAGE);
		setHeight(90, Unit.PERCENTAGE);
	}

	protected void loadACsData() throws EngineException
	{
		allClasses = attrMan.getAttributeClasses();
		
		for (String ac: allClasses.keySet())
			acs.addItem(ac);
	}
	
	protected void updateEffective()
	{
		String rootClass = msg.getMessage("AttributesClasses.metaEffectiveClassName");
		while (allClasses.keySet().contains(rootClass))
			rootClass = " " + rootClass;
		@SuppressWarnings("unchecked")
		Set<String> parents = (Set<String>) acs.getValue();
		AttributesClass virtual = new AttributesClass(rootClass, "", EMPTY, EMPTY, 
				parents.isEmpty() ? true : false, parents);
		Map<String, AttributesClass> tmpClasses = new HashMap<>(allClasses);
		tmpClasses.put(rootClass, virtual);
		effective.setInput(rootClass, tmpClasses);
	}
}
