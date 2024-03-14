/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.idp;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.imunity.vaadin.elements.ActionMenu;
import io.imunity.vaadin.elements.MenuButton;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.icon.VaadinIcon.*;

public class CollapsableGrid<T> extends CustomField<List<T>>
{
	private final MessageSource msg;
	private final Supplier<Editor<T>> editorProvider;
	private final Supplier<T> getter;

	private Grid<Editor<T>> grid;
	protected GridListDataView<Editor<T>> gridListDataView;
	private Editor<T> draggedItem;
	private final boolean readOnly;

	public CollapsableGrid(MessageSource msg, Supplier<Editor<T>> editorProvider, Supplier<T> getter, String caption)
	{
		this(msg, editorProvider, caption, getter, msg.getMessage("addNew"), false);
	}

	public CollapsableGrid(MessageSource msg, Supplier<Editor<T>> editorProvider, String caption,
			Supplier<T> getter, String addButtonCaption, boolean readOnly)
	{
		this.msg = msg;
		this.editorProvider = editorProvider;
		this.getter = getter;
		this.readOnly = readOnly;
		initUI(caption, addButtonCaption);
	}

	private void initUI(String caption, String addButtonCaption)
	{
		setWidthFull();
		getStyle().set("overflow-y", "hidden");
		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);

		HorizontalLayout elementsHeader = new HorizontalLayout();
		elementsHeader.setWidthFull();
		Button addElement = new Button(addButtonCaption);
		addElement.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
		addElement.addClickListener(event ->
		{
			Editor<T> tEditor = editorProvider.get();
			T value = getter.get();
			tEditor.setValue(value);
			tEditor.addValueChangeListener(e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient())));
			gridListDataView.addItem(tEditor);
			grid.select(tEditor);
			grid.setDetailsVisible(tEditor, true);
		});
		addElement.setVisible(!readOnly);

		Span captionLabel = new Span(caption);
		elementsHeader.add(captionLabel, addElement);
		elementsHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

		grid = new Grid<>();
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		grid.setAllRowsVisible(true);
		grid.addComponentColumn(e -> createNameWithDetailsArrow(grid, e));
		grid.setItemDetailsRenderer(new ComponentRenderer<>(e -> e));
		grid.addComponentColumn(this::createRowActionMenu)
				.setTextAlign(ColumnTextAlign.END);
		grid.setSelectionMode(Grid.SelectionMode.NONE);
		gridListDataView = grid.setItems(getEditors(new ArrayList<>()));
		enableRowReordering();
		main.add(elementsHeader, grid);
		add(main);
	}

	public void validate() throws FormValidationException
	{
		for (Editor<T> c : gridListDataView.getItems().toList())
			c.validate();
	}

	private List<Editor<T>> getEditors(List<T> elements)
	{
		if (elements == null)
			return Collections.emptyList();
		
		return elements.stream().map(item ->
		{
			Editor<T> tEditor = editorProvider.get();
			tEditor.setValue(item);
			tEditor.addValueChangeListener(e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient())));
			return tEditor;
		}).collect(Collectors.toList());
	}

	private void enableRowReordering()
	{
		grid.setDropMode(GridDropMode.BETWEEN);
		grid.addDropListener(e ->
		{
			Editor<T> targetPerson = e.getDropTargetItem().orElse(null);
			GridDropLocation dropLocation = e.getDropLocation();
			if (targetPerson == null || draggedItem.equals(targetPerson))
				return;

			gridListDataView.removeItem(draggedItem);

			if (dropLocation == GridDropLocation.BELOW)
			{
				gridListDataView.addItemAfter(draggedItem, targetPerson);
			}
			else
			{
				gridListDataView.addItemBefore(draggedItem, targetPerson);
			}
			updateValue();
		});

		grid.addDragEndListener(e -> draggedItem = null);
	}

	@Override
	protected List<T> generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(List<T> elements)
	{
		this.gridListDataView = grid.setItems(getEditors(elements));
	}

	
	@Override
	public void clear()
	{
		super.clear();
		this.gridListDataView = grid.setItems(new ArrayList<>());
	}
	private HorizontalLayout createNameWithDetailsArrow(Grid<Editor<T>> grid, Editor<T> entry)
	{
		Icon openIcon = VaadinIcon.ANGLE_DOWN.create();
		Icon closeIcon = VaadinIcon.ANGLE_UP.create();
		openIcon.setVisible(!grid.isDetailsVisible(entry));
		closeIcon.setVisible(grid.isDetailsVisible(entry));
		openIcon.addClickListener(e -> grid.setDetailsVisible(entry, true));
		closeIcon.addClickListener(e -> grid.setDetailsVisible(entry, false));
		Span header = new Span(entry.getHeaderText());
		entry.addValueChangeListener(event -> header.setText(entry.getHeaderText()));
		return new HorizontalLayout(openIcon, closeIcon, header);
	}

	private Component createRowActionMenu(Editor<T> entry)
	{
		Icon moveIcon = RESIZE_H.create();
		moveIcon.addClassName(POINTER.name());
		ActionMenu actionMenu = new ActionMenu();
		actionMenu.setVisible(!readOnly);
		DragSource<Icon> iconDragSource = DragSource.create(moveIcon);
		iconDragSource.setDraggable(true);
		iconDragSource.addDragStartListener(event ->
		{
			draggedItem = entry;
			grid.setRowsDraggable(true);
		});
		iconDragSource.addDragEndListener(event -> grid.setRowsDraggable(false));

		MenuButton topButton = new MenuButton(msg.getMessage("ListOfCollapsableElements.moveTop"), ANGLE_UP);
		actionMenu.addItem(topButton, e ->
		{
			gridListDataView.addItemBefore(entry, gridListDataView.getItem(0));
			updateValue();
		}).setVisible(!gridListDataView.getItem(0).equals(entry));

		MenuButton bottomButton = new MenuButton(msg.getMessage("ListOfCollapsableElements.moveBottom"), ANGLE_DOWN);
		actionMenu.addItem(bottomButton, e ->
		{
			gridListDataView.removeItem(entry);
			gridListDataView.addItem(entry);
			updateValue();
		}).setVisible(!gridListDataView.getItem(gridListDataView.getItemCount() - 1).equals(entry));

		MenuButton removeButton = new MenuButton(msg.getMessage("remove"), TRASH);
		actionMenu.addItem(removeButton, e ->
		{
			gridListDataView.removeItem(entry);
			updateValue();
		});

		HorizontalLayout horizontalLayout = new HorizontalLayout(moveIcon, actionMenu.getTarget());
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	public List<Editor<T>> getEditors()
	{
		return gridListDataView.getItems().toList();
	}
	
	@Override
	public List<T> getValue()
	{
		return gridListDataView.getItems().map(AbstractField::getValue).toList();
	}

	public static abstract class Editor<V> extends CustomField<V>
	{
		protected abstract String getHeaderText();

		protected abstract void validate() throws FormValidationException;
	}

}
