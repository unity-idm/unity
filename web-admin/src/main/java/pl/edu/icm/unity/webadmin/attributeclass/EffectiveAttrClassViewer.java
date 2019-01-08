/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.SmallGrid;

/**
 * Displays a tree with attribute classes (children are roots, parents are sub nodes).
 * The selected attributes class is evaluated to compute its effective settings which are displayed on right side. 
 * @author K. Benedyczak
 */
public class EffectiveAttrClassViewer extends HorizontalSplitPanel
{
	private Tree<Node> parents;
	private Label allAllowed;
	private Grid<String> allowed;
	private Grid<String> mandatory;
	private VerticalLayout left, right;
	private Map<String, AttributesClass> allClasses;
	
	public EffectiveAttrClassViewer(UnityMessageSource msg)
	{
		parents = new Tree<>(msg.getMessage("AttributesClass.parentsInEffective"));
		parents.addSelectionListener(selection -> 
		{
			Optional<Node> selected = selection.getFirstSelectedItem();
			if (!selected.isPresent())
				setEmptyEffective();
			else
				setEffective(selected.get().getName());
		});
		
		allAllowed = new Label(msg.getMessage("AttributesClass.allAllowed"));
		
		allowed = new SmallGrid<>();
		allowed.setWidth(90, Unit.PERCENTAGE);
		allowed.setHeight(9, Unit.EM);
		allowed.addColumn(a -> a).setCaption(msg.getMessage("AttributesClass.allowed"));
		
		mandatory = new SmallGrid<>();
		mandatory.setWidth(90, Unit.PERCENTAGE);
		mandatory.setHeight(9, Unit.EM);
		mandatory.addColumn(a -> a).setCaption(msg.getMessage("AttributesClass.mandatory"));
		
		FormLayout rightC = new CompactFormLayout();
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
		setWidth(100, Unit.PERCENTAGE);
	}
	
	public void setInput(String rootClass, Map<String, AttributesClass> allClasses)
	{
		this.allClasses = allClasses;
		parents.setItems();
		setEmptyEffective();
		if (rootClass == null)
		{
			setVisible(false);
			return;
		}
		setVisible(true);
		TreeData<Node> treeData = new TreeData<>();
		addRecursive(treeData, rootClass, null);
		parents.setDataProvider(new TreeDataProvider<>(treeData));
	}
	
	private void addRecursive(TreeData<Node> treeData, String root, Node parent)
	{
		String myPath = (parent == null ? "/" : parent.getPath())
				+ root + "/";
		Node myNode = new Node(root, myPath);
		treeData.addItem(parent, myNode);
		AttributesClass ac = allClasses.get(root);
		for (String myParent: ac.getParentClasses())
			addRecursive(treeData, myParent, myNode);
	}
	
	private void setEmptyEffective()
	{
		right.setVisible(false);
	}
	
	private void setEffective(String ac)
	{
		right.setVisible(true);
		AttributeClassHelper helper;
		helper = new AttributeClassHelper(allClasses, Collections.singleton(ac));
		if (helper.isEffectiveAllowArbitrary())
		{
			allAllowed.setVisible(true);
			allowed.setVisible(false);
		} else
		{
			allAllowed.setVisible(false);
			allowed.setVisible(true);
			allowed.setItems(helper.getEffectiveAllowed());
		}
		allowed.sort(allowed.getColumns().get(0));
		
		mandatory.setItems(helper.getEffectiveMandatory());
		mandatory.sort(mandatory.getColumns().get(0));
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
