/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.attribute_classes;


import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class EffectiveAttrClassViewer extends VerticalLayout
{
	private final TreeGrid<Node> parents;
	private final Span allAllowed;
	private final Grid<String> allowed;
	private final Grid<String> mandatory;
	private final VerticalLayout rightMenu;
	private final SplitLayout mainLayout;
	private Map<String, AttributesClass> allClasses;
	
	public EffectiveAttrClassViewer(MessageSource msg)
	{
		setPadding(false);
		parents = new TreeGrid<>();
		parents.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
		parents.addHierarchyColumn(node -> node.name);
		parents.addSelectionListener(selection ->
		{
			Optional<Node> selected = selection.getFirstSelectedItem();
			if (selected.isEmpty())
				setEmptyEffective();
			else
				setEffective(selected.get().name());
		});
		
		allAllowed = new Span(msg.getMessage("AttributesClass.allAllowed"));
		
		allowed = new Grid<>();
		allowed.setWidth(90, Unit.PERCENTAGE);
		allowed.setHeight(9, Unit.EM);
		allowed.addColumn(attributeClass -> attributeClass)
				.setHeader(msg.getMessage("AttributesClass.allowed"));

		mandatory = new Grid<>();
		mandatory.setWidth(90, Unit.PERCENTAGE);
		mandatory.setHeight(9, Unit.EM);
		mandatory.addColumn(attributeClass -> attributeClass)
				.setHeader(msg.getMessage("AttributesClass.mandatory"));
		
		rightMenu = new VerticalLayout();
		rightMenu.add(new Span(msg.getMessage("AttributesClass.effectiveClass")), allAllowed, allowed, mandatory);
		rightMenu.setSizeFull();
		rightMenu.setPadding(false);

		VerticalLayout leftMenu = new VerticalLayout(new H5(msg.getMessage("AttributesClass.parentsInEffective")), parents);

		mainLayout = new SplitLayout(leftMenu, new VerticalLayout(rightMenu));
		mainLayout.setWidthFull();
		setSplitterPosition(30);
		add(mainLayout);
		setHeight(27, Unit.EM);
	}

	public void setSplitterPosition(double position)
	{
		mainLayout.setSplitterPosition(position);
	}

	public void setInput(String rootClass, Map<String, AttributesClass> allClasses)
	{
		this.allClasses = allClasses;
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
		String myPath = (parent == null ? "/" : parent.path())
				+ root + "/";
		Node myNode = new Node(root, myPath);
		treeData.addItem(parent, myNode);
		AttributesClass ac = allClasses.get(root);
		for (String myParent: ac.getParentClasses())
			addRecursive(treeData, myParent, myNode);
	}
	
	private void setEmptyEffective()
	{
		rightMenu.setVisible(false);
	}
	
	private void setEffective(String ac)
	{
		rightMenu.setVisible(true);
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
		allowed.sort(GridSortOrder.asc(allowed.getColumns().get(0)).build());

		mandatory.setItems(helper.getEffectiveMandatory());
		mandatory.sort(GridSortOrder.asc(mandatory.getColumns().get(0)).build());
	}

	private record Node(String name, String path)
		{

			public String toString()
			{
				return name;
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
					return path.equals(((Node) obj).path);
				return false;

			}
		}
}
