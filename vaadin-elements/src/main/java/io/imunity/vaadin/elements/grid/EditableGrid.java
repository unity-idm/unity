/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements.grid;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.END;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;

public class EditableGrid<T> extends CustomField<List<T>>
{
	private final Grid<T> grid = new Grid<>();
	private final Editor<T> editor = grid.getEditor();
	private final GridListDataView<T> gridListDataView;
	private final Function<String, String> msg;
	private final VerticalLayout layout;
	private Grid.Column<T> actions;
	private T draggedItem;

	public EditableGrid(Function<String, String> msg, Supplier<T> supplier)
	{
		this.msg = msg;
		this.gridListDataView = grid.setItems(new ArrayList<>());
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		editor.setBinder(new Binder<>());

		Button add = new Button(msg.apply("add"), VaadinIcon.PLUS_CIRCLE_O.create(), e ->
		{
			if (editor.isOpen())
				editor.cancel();
			T element = supplier.get();
			gridListDataView.addItem(element);
			editor.editItem(element);
		});
		add.addThemeVariants(LUMO_PRIMARY);

		layout = new VerticalLayout(add, grid);
		layout.setPadding(false);
		layout.setAlignItems(FlexComponent.Alignment.END);
		add(layout);
		addActionColumn();
		enableRowReordering();
	}

	private void addActionColumn()
	{
		actions = grid.addComponentColumn(bean ->
				{
					Icon removeIcon = VaadinIcon.TRASH.create();
					removeIcon.addClassName(POINTER.getName());
					removeIcon.addClickListener(event ->
					{
						gridListDataView.removeItem(bean);
						updateValue();
					});
					Icon moveIcon = VaadinIcon.RESIZE_H.create();
					moveIcon.addClassName(POINTER.getName());
					HorizontalLayout layout = new HorizontalLayout(moveIcon, removeIcon);
					layout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
					return layout;
				})
				.setHeader(msg.apply("actions"))
				.setEditorComponent(addEditButtons())
				.setAutoWidth(true)
				.setTextAlign(END);
	}

	public Grid.Column<T> addColumn(ValueProvider<T, String> get, Setter<T, String> set, boolean req)
	{
		Grid.Column<T> tColumn = grid.addColumn(get)
				.setEditorComponent(getBaseEditorComponent(get, set, req, null));
		putActionColumnToEnd(tColumn);
		return tColumn;
	}

	public Grid.Column<T> addColumn(ValueProvider<T, String> get, Setter<T, String> set, Validator<String> validator)
	{
		Grid.Column<T> tColumn = grid.addColumn(get)
				.setEditorComponent(getBaseEditorComponent(get, set, true, validator));
		putActionColumnToEnd(tColumn);
		return tColumn;
	}

	private void putActionColumnToEnd(Grid.Column<T> tColumn)
	{
		List<Grid.Column<T>> columns = new ArrayList<>(grid.getColumns());
		columns.set(columns.size() - 2, tColumn);
		columns.set(columns.size() - 1, actions);
		grid.setColumnOrder(columns);
	}

	public Grid.Column<T> addComboBoxColumn(ValueProvider<T, String> get, Setter<T, String> set, List<String> items)
	{
		Grid.Column<T> tColumn = grid.addColumn(get)
				.setEditorComponent(getSelectEditorComponent(get, set, items));
		putActionColumnToEnd(tColumn);
		return tColumn;
	}

	public <F> Grid.Column<T> addCustomColumn(ValueProvider<T, F> get, Setter<T, F> set, HasValue<?, F> component)
	{
		editor.getBinder().forField(component).bind(get, set);
		Grid.Column<T> tColumn = grid.addColumn(get)
				.setEditorComponent((Component) component);
		putActionColumnToEnd(tColumn);
		return tColumn;
	}

	public Grid.Column<T> addIntColumn(ValueProvider<T, Integer> get, Setter<T, Integer> set)
	{
		Grid.Column<T> tColumn = grid.addColumn(get)
				.setEditorComponent(getIntegerEditorComponent(get, set));
		putActionColumnToEnd(tColumn);
		return tColumn;
	}

	private TextField getBaseEditorComponent(ValueProvider<T, String> get, Setter<T, String> set, boolean req, Validator<String> validator)
	{
		TextField field = new TextField();
		field.setValueChangeMode(ValueChangeMode.EAGER);
		Binder.BindingBuilder<T, String> bindingBuilder = editor.getBinder().forField(field);
		if(req)
			bindingBuilder = bindingBuilder.asRequired(msg.apply("fieldRequired"));
		if(validator != null)
			bindingBuilder = bindingBuilder.withValidator(validator);
		bindingBuilder.bind(get, set);
		return field;
	}

	private IntegerField getIntegerEditorComponent(ValueProvider<T, Integer> get, Setter<T, Integer> set)
	{
		IntegerField field = new IntegerField();
		field.setMin(1);
		field.setMax(65535);
		field.setValueChangeMode(ValueChangeMode.EAGER);
		editor.getBinder()
				.forField(field)
				.asRequired(msg.apply("fieldRequired"))
				.bind(get, set);
		return field;
	}

	private Select<String> getSelectEditorComponent(ValueProvider<T, String> get, Setter<T, String> set, List<String> items)
	{
		Select<String> field = new Select<>();
		field.setItems(items);
		editor.getBinder().forField(field).bind(get, set);
		return field;
	}

	private void enableRowReordering()
	{
		grid.setDropMode(GridDropMode.BETWEEN);
		grid.setRowsDraggable(true);

		grid.addDragStartListener(e -> draggedItem = e.getDraggedItems().get(0));

		grid.addDropListener(e ->
		{
			T targetPerson = e.getDropTargetItem().orElse(null);
			GridDropLocation dropLocation = e.getDropLocation();
			if (targetPerson == null || draggedItem.equals(targetPerson))
				return;

			gridListDataView.removeItem(draggedItem);

			if (dropLocation == GridDropLocation.BELOW)
				gridListDataView.addItemAfter(draggedItem, targetPerson);
			else
				gridListDataView.addItemBefore(draggedItem, targetPerson);
			updateValue();
		});

		grid.addDragEndListener(e -> draggedItem = null);
	}

	private Component addEditButtons()
	{
		Button save = new Button(msg.apply("save"), e ->
		{
			if(editor.getBinder().validate().isOk())
			{
				editor.save();
				editor.cancel();
			}
		});
		save.addThemeVariants(LUMO_TERTIARY);
		save.addClickShortcut(Key.ENTER);

		Button cancel = new Button(msg.apply("cancel"), e ->
		{
			gridListDataView.removeItem(editor.getItem());
			updateValue();
			editor.cancel();
		});
		cancel.addThemeVariants(LUMO_TERTIARY);

		editor.getBinder()
				.addStatusChangeListener(status -> save.setEnabled(status.getBinder().isValid()));
		editor.addOpenListener(e -> save.setEnabled(false));

		return new Div(save, cancel);
	}

	@Override
	protected List<T> generateModelValue()
	{
		return getValue();
	}

	@Override
	public List<T> getValue()
	{
		return gridListDataView.getItems().toList();
	}

	@Override
	protected void setPresentationValue(List<T> ts)
	{
		gridListDataView.removeItems(gridListDataView.getItems().toList());
		gridListDataView.addItems(ts);
	}

	@Override
	public void setHeight(String height)
	{
		layout.setHeight(height);
	}
}
