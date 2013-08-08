/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import java.util.Collections;
import java.util.Map;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.attributes.AttributeClassHelper;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributesClass;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Displays a tree with attribute classes (children are roots, parents are sub nodes).
 * The selected attributes class is evaluated to compute its effective settings which are displayed on right side. 
 * @author K. Benedyczak
 */
public class EffectiveAttrClassViewer extends HorizontalSplitPanel
{
	private Tree parents;
	private Label allAllowed;
	private Table allowed;
	private Table mandatory;
	private VerticalLayout left, right;
	private Map<String, AttributesClass> allClasses;
	
	public EffectiveAttrClassViewer(UnityMessageSource msg)
	{
		parents = new Tree(msg.getMessage("AttributesClass.parentsInEffective"));
		parents.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Node value = (Node)parents.getValue();
				if (value == null)
					setEmptyEffective();
				else
					setEffective(value.getName());
			}
		});
		parents.setImmediate(true);
		
		allAllowed = new Label(msg.getMessage("AttributesClass.allAllowed"));
		
		allowed = new Table();
		allowed.setWidth(90, Unit.PERCENTAGE);
		allowed.setHeight(9, Unit.EM);
		allowed.addContainerProperty(msg.getMessage("AttributesClass.allowed"), 
				String.class, null);
		mandatory = new Table();
		mandatory.setWidth(90, Unit.PERCENTAGE);
		mandatory.setHeight(9, Unit.EM);
		mandatory.addContainerProperty(msg.getMessage("AttributesClass.mandatory"), 
				String.class, null);
		
		FormLayout rightC = new FormLayout();
		rightC.addComponents(allAllowed, allowed, mandatory);
		rightC.setSizeFull();
		rightC.setCaption(msg.getMessage("AttributesClass.effectiveClass"));
		
		right = new VerticalLayout(rightC);
		right.setMargin(true);
		left = new VerticalLayout(parents);
		left.setSpacing(true);
		left.setMargin(true);
		
		
		setFirstComponent(left);
		setSecondComponent(right);
		setSplitPosition(30, Unit.PERCENTAGE);
		setHeight(27, Unit.EM);
		setStyleName(Reindeer.SPLITPANEL_SMALL);
	}
	
	public void setInput(String rootClass, Map<String, AttributesClass> allClasses)
	{
		this.allClasses = allClasses;
		parents.removeAllItems();
		setEmptyEffective();
		if (rootClass == null)
		{
			setVisible(false);
			return;
		}
		setVisible(true);
		addRecursive(rootClass, null);
		parents.select(new Node(rootClass, "/"+rootClass+"/"));
	}
	
	private void addRecursive(String root, Node parent)
	{
		String myPath = (parent == null ? "/" : parent.getPath())
				+ root + "/";
		Node myNode = new Node(root, myPath);
		parents.addItem(myNode);
		if (parent != null)
			parents.setParent(myNode, parent);
		AttributesClass ac = allClasses.get(root);
		for (String myParent: ac.getParentClasses())
			addRecursive(myParent, myNode);
		if (ac.getParentClasses().isEmpty())
			parents.setChildrenAllowed(myNode, false);
	}
	
	private void setEmptyEffective()
	{
		right.setVisible(false);
	}
	
	private void setEffective(String ac)
	{
		right.setVisible(true);
		AttributeClassHelper helper;
		try
		{
			helper = new AttributeClassHelper(allClasses, Collections.singleton(ac));
		} catch (IllegalTypeException e)
		{
			throw new IllegalStateException("Got AC which is undefined", e);
		}
		if (helper.isEffectiveAllowArbitrary())
		{
			allAllowed.setVisible(true);
			allowed.setVisible(false);
		} else
		{
			allAllowed.setVisible(false);
			allowed.setVisible(true);
			allowed.removeAllItems();
			for (String al: helper.getEffectiveAllowed())
				allowed.addItem(new String[] {al}, al);
		}
		mandatory.removeAllItems();
		for (String al: helper.getEffectiveMandatory())
			mandatory.addItem(new String[] {al}, al);
		
	}
	
	private static class Node
	{
		private String name;
		private String path;
		
		public Node(String name, String path)
		{
			super();
			this.name = name;
			this.path = path;
		}
		
		public String toString()
		{
			return name;
		}

		public String getName()
		{
			return name;
		}

		public String getPath()
		{
			return path;
		}

		@Override
		public int hashCode()
		{
			return path.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof String)
				return path.equals(obj);
			if (obj instanceof Node)
				return path.equals(((Node)obj).path);
			return false;

		}
	}
}
