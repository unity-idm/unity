/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements.grid;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Grid with row editor. By default action column with delete button is added as
 * last grid column.
 * 
 * @author P.Piernik
 *
 * @param <T>
 */
public class GridWithEditorInDetails<T> extends CustomField<List<T>> implements FilterableGrid<T>
{
	private GridWithActionColumn<T> grid;
	private final Class<T> type;
	private T newElement;
	private final Predicate<T> disableEdit;
	private final HorizontalLayout addButtonBar;
	private Consumer<T> valueChangeListener;
	private Function<String, String> msg;
	
	public GridWithEditorInDetails(Function<String, String> msg, Class<T> type,
			Supplier<EmbeddedEditor<T>> gridEditorSupplier, Predicate<T> disableEditAndRemove, boolean enableDrag)
	
	{
		this(msg, type, gridEditorSupplier, disableEditAndRemove, disableEditAndRemove, enableDrag);		
	}
	
	public GridWithEditorInDetails(Function<String, String> msg, Class<T> type,
			Supplier<EmbeddedEditor<T>> gridEditorSupplier, 
			Predicate<T> disableEdit,  Predicate<T> disableRemove, boolean enableDrag)
	{
		this.type = type;
		this.disableEdit = disableEdit;
		this.msg = msg;
		
		SingleActionHandler<T> remove = SingleActionHandler.builder4Delete(msg, type)
				.withDisabledPredicate(disableRemove)
				.hideIfInactive().withHandler(r -> {
					T element = r.iterator().next();
					grid.removeElement(element);
					if (newElement == element)
					{
						resetNewElement();
					}
					fireChange();
				}).build();

		SingleActionHandler<T> edit = SingleActionHandler.builder4Edit(msg, type)
				.withDisabledPredicate(
						t -> grid.isDetailsVisible(t) || disableEdit.test(t))
				.withHandler(r -> edit(r.iterator().next())).build();
		grid = new GridWithActionColumn<>(msg, new ArrayList<>(Arrays.asList(edit, remove)));
		grid.addThemeVariants(GridVariant.LUMO_COMPACT);
		grid.setWidthFull();
		grid.setDetailsVisibleOnClick(false);
		grid.setAllRowsVisible(true);
		grid.setItemDetailsRenderer(new ComponentRenderer<>((T t) -> {
			VerticalLayout wrapper = new VerticalLayout();
			wrapper.setPadding(false);
			wrapper.setSpacing(false);
			EmbeddedEditor<T> editor = gridEditorSupplier.get();
			editor.setValue(t);
			wrapper.add((Component) editor);
			HorizontalLayout buttons;
			if (newElement != null && newElement.equals(t))
			{
				Button cancelButton = new Button(msg.apply("cancel"), event ->
				{
					grid.removeElement(t);
					fireChange();
					resetNewElement();
				});
				Button createButton = new Button(msg.apply("create"), event ->
				{
					T value;
					try
					{
						value = editor.getValidValue();
					} catch (Exception e)
					{
						return;
					}

					grid.setDetailsVisible(t, false);
					grid.replaceElement(t, value);
					fireChange();
					resetNewElement();
				});
				createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
				buttons = new HorizontalLayout(cancelButton, createButton);
			}
			else
			{
				Button cancelButton = new Button(msg.apply("cancel"), event -> grid.setDetailsVisible(t, false));
				Button updateButton = new Button(msg.apply("update"), event ->
				{
					T value;
					try
					{
						value = editor.getValidValue();
					} catch (Exception e)
					{
						return;
					}

					grid.setDetailsVisible(t, false);
					grid.replaceElement(t, value);
					if (valueChangeListener != null)
					{
						valueChangeListener.accept(value);
					}
					fireChange();
				});
				updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
				buttons = new HorizontalLayout(cancelButton, updateButton);
			}

			buttons.setWidthFull();
			buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
			wrapper.add(buttons);
			return wrapper;
		}));
		
		addButtonBar = new HorizontalLayout();
		add(initContent());
	}

	private void edit(T toEdit)
	{
		for (T t : grid.getElements())
		{
			grid.setDetailsVisible(t, false);
		}
		if (newElement != null)
		{
			grid.removeElement(newElement);
		}
		resetNewElement();
		grid.setDetailsVisible(toEdit, true);
	}
	
	public void addActionHandler(SingleActionHandler<T> actionHandler)
	{
		grid.addActionHandler(0, actionHandler);
	}
	private void resetNewElement()
	{
		newElement = null;
	}


	private void fireChange()
	{
		fireEvent(new ComponentValueChangeEvent<>(this, this, grid.getElements(), true));
	}
	
	public void addUpdateListener(Consumer<T> valueChange)
	{
		valueChangeListener = valueChange;
	}

	public T newInstance(Class<T> cls)
	{
		try
		{
			return cls.getDeclaredConstructor().newInstance();
		} catch (Exception e)
		{
			throw new RuntimeException(e.getMessage());
		}
	}
	
	public Grid.Column<T> addCheckboxColumn(ValueProvider<T, Boolean> valueProvider)
	{
		Grid.Column<T> column = grid.addComponentColumn(e ->
				{
					Checkbox checkbox = new Checkbox(valueProvider.apply(e));
					checkbox.setReadOnly(true);
					return checkbox;
				});
		grid.refreshActionColumn();
		return column;
	}

	public Grid.Column<T> addTextColumn(ValueProvider<T, String> valueProvider)

	{
		Grid.Column<T> column = grid.addColumn(valueProvider);
		grid.refreshActionColumn();
		return column;
	}

	public Grid.Column<T> addGotoEditColumn(ValueProvider<T, String> valueProvider)
	{
		Grid.Column<T> column = grid
				.addComponentColumn(
						p ->
						{
							if(disableEdit.test(p))
								return new Span(valueProvider.apply(p));
							else
							{
								Button button = new Button(valueProvider.apply(p), e -> edit(p));
								button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
								return button;
							}
						});
		grid.refreshActionColumn();
		return column;
	}
	
	public void sort(Grid.Column<T> column)
	{
		grid.sort(GridSortOrder.asc(column).build());
	}

	public void addElement(T el)
	{
		grid.addElement(el);
		fireChange();
	}

	public void removeElement(T el)
	{
		grid.removeElement(el);
		fireChange();
	}
	
	public void replaceElement(T old, T newElement)
	{
		grid.setDetailsVisible(old, false);
		grid.replaceElement(old, newElement);
		resetNewElement();
		fireChange();
	}

	@Override
	public List<T> getValue()
	{
		return grid.getElements();
	}
	
	public void setAddVisible(boolean visible)
	{
		addButtonBar.setVisible(visible);
	}
	
	private Component initContent()
	{
		VerticalLayout main = new VerticalLayout();
		main.setHeightFull();
		main.setPadding(false);
		main.setSpacing(false);

		addButtonBar.setWidthFull();
		addButtonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		Button add = new Button(msg.apply("addNew"));
		add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		add.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
		add.addClickListener(e -> {

			if (newElement == null)
			{
				newElement = newInstance(type);
				grid.addElement(newElement);
				for (T t : grid.getElements())
				{
					grid.setDetailsVisible(t, false);
				}
				grid.setDetailsVisible(newElement, true);
			}

		});

		addButtonBar.add(add);

		main.add(addButtonBar);
		main.add(grid);
		grid.getEditor();
		return main;

	}

	@Override
	public void setValue(List<T> value)
	{
		grid.setItems(value);
		resetNewElement();
		grid.getElements().forEach(e -> grid.setDetailsVisible(e, false));
	}

	@Override
	public void clearFilters()
	{
		grid.clearFilters();
		
	}

	@Override
	public void addFilter(SerializablePredicate<T> filter)
	{
		grid.addFilter(filter);

	}

	@Override
	public void removeFilter(SerializablePredicate<T> filter)
	{
		grid.removeFilter(filter);
		
	}

	@Override
	protected List<T> generateModelValue()
	{
		return null;
	}

	@Override
	protected void setPresentationValue(List<T> ts)
	{

	}

	public interface EmbeddedEditor<T> extends HasValueAndElement<ComponentValueChangeEvent<CustomField<T>, T>, T>
	{
		T getValidValue() throws Exception;
	}
}