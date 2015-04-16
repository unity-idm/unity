/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import java.util.Map;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.SmallTable;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.ui.Label;
import com.vaadin.ui.Table;

/**
 * Displays a single {@link AttributesClass}: its settings and parents hierarchy.
 * @author K. Benedyczak
 */
public class AttributesClassViewer extends CompactFormLayout
{
	private UnityMessageSource msg;
	
	private Label name;
	private Label typeDescription;
	private Label allAllowed;
	private Table allowed;
	private Table mandatory;
	private SafePanel effectiveWrapper;
	private EffectiveAttrClassViewer effectiveViewer;
	
	
	public AttributesClassViewer(UnityMessageSource msg)
	{
		super();
		this.msg = msg;
		
		initUI();
	}
	
	private void initUI()
	{
		name = new Label();
		name.setCaption(msg.getMessage("AttributesClass.name"));
		
		typeDescription = new Label();
		typeDescription.setCaption(msg.getMessage("AttributesClass.description"));
		
		allAllowed = new Label(msg.getMessage("AttributesClass.allAllowed"));
		
		allowed = new SmallTable();
		allowed.addContainerProperty(msg.getMessage("AttributesClass.allowed"), 
				String.class, null);
		allowed.setWidth(90, Unit.PERCENTAGE);
		allowed.setHeight(12, Unit.EM);
		allowed.setSortContainerPropertyId(msg.getMessage("AttributesClass.allowed"));
		
		mandatory = new SmallTable();
		mandatory.addContainerProperty(msg.getMessage("AttributesClass.mandatory"), 
				String.class, null);
		mandatory.setWidth(90, Unit.PERCENTAGE);
		mandatory.setHeight(12, Unit.EM);
		mandatory.setSortContainerPropertyId(msg.getMessage("AttributesClass.mandatory"));
		
		effectiveViewer = new EffectiveAttrClassViewer(msg);
		effectiveWrapper = new SafePanel(effectiveViewer);
		effectiveWrapper.setCaption(msg.getMessage("AttributesClass.resultingClassInView"));
		
		addComponents(name, typeDescription, allAllowed, allowed, mandatory, effectiveWrapper);
	}
	
	public void setInput(String rootClass, Map<String, AttributesClass> allClasses)
	{
		if (rootClass == null)
		{
			setContentsVisible(false);
			return;
		}
		
		effectiveViewer.setInput(rootClass, allClasses);
		setContentsVisible(true);
		AttributesClass ac = allClasses.get(rootClass);
		name.setValue(ac.getName());
		typeDescription.setValue(ac.getDescription());
		if (ac.isAllowArbitrary())
		{
			allowed.setVisible(false);
		} else
		{
			allAllowed.setVisible(false);
			allowed.removeAllItems();
			for (String a: ac.getAllowed())
				allowed.addItem(new String[] {a}, a);
		}
		allowed.sort();
		
		mandatory.removeAllItems();
		for (String m: ac.getMandatory())
			mandatory.addItem(new String[] {m}, m);
		mandatory.sort();
	}
	
	private void setContentsVisible(boolean how)
	{
		name.setVisible(how);
		typeDescription.setVisible(how);
		allAllowed.setVisible(how);
		allowed.setVisible(how);
		mandatory.setVisible(how);
		effectiveWrapper.setVisible(how);
	}
}
