/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.vaadin.data.ValueProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.server.SerializableSupplier;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.FetchItemsCallback;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.webui.common.grid.FilterableGrid;

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
	private Class<T> type;
	private T newElement;
	private Predicate<T> disableEdit;
	private HorizontalLayout addButtonBar;
	private Consumer<T> valueChangeListener;
	private MessageSource msg;
	
	public GridWithEditorInDetails(MessageSource msg, Class<T> type,
			Supplier<EmbeddedEditor<T>> gridEditorSupplier, Predicate<T> disableEditAndRemove, boolean enableDrag)
	
	{
		this(msg, type, gridEditorSupplier, disableEditAndRemove, disableEditAndRemove, enableDrag);		
	}
	
	public GridWithEditorInDetails(MessageSource msg, Class<T> type,
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
		grid = new GridWithActionColumn<>(msg, new ArrayList<>(Arrays.asList(edit, remove)), enableDrag);
		grid.setDetailsGenerator(t -> {
			VerticalLayout wrapper = new VerticalLayout();
			wrapper.setMargin(false);
			wrapper.setSpacing(false);
			EmbeddedEditor<T> editor = gridEditorSupplier.get();
			editor.setValue(t);
			editor.addStyleName("u-gridEmbeddedEditor");
			wrapper.addComponent(editor);
			HorizontalLayout buttons;
			if (newElement != null && newElement.equals(t))
			{

				buttons = StandardButtonsHelper.buildConfirmNewButtonsBar(msg, () -> {
					T value;
					try
					{
						value = editor.getValue();
					} catch (FormValidationException e)
					{
						return;
					}

					grid.setDetailsVisible(t, false);
					grid.replaceElement(t, value);
					fireChange();
					resetNewElement();
				}, () -> {
					grid.removeElement(t);
					fireChange();
					resetNewElement();
				}, Alignment.MIDDLE_RIGHT);

			} else
			{
				buttons = StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> {
					T value;
					try
					{
						value = editor.getValue();
					} catch (FormValidationException e)
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

				}, () -> {
					grid.setDetailsVisible(t, false);
				}, Alignment.MIDDLE_RIGHT);

			}
			
			buttons.setMargin(false);
			wrapper.addComponent(buttons);
			return wrapper;
		});
		
		addButtonBar = new HorizontalLayout();
		setHeight(100, Unit.PERCENTAGE);
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

	public void setMinHeightByRow(int minRow)
	{
		grid.setHeightByRows(true);
		grid.setMinHeightByRow(minRow);
	}
	
	public void setHeightByRow(boolean byRow)
	{
		grid.setHeightByRows(byRow);
		if (!byRow)
		{
			grid.setHeight(100, Unit.PERCENTAGE);
		}
	}

	private void fireChange()
	{
		fireEvent(new ValueChangeEvent<List<T>>(this, grid.getElements(), true));
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
			throw new InternalException(e.getMessage());
		}
	}
	
	public Column<T, ?> addTextColumn(ValueProvider<T, String> valueProvider, String caption, int expandRatio)
	{
		return addTextColumn(valueProvider, caption, expandRatio, false, false);
	}
	
	public Column<T, ?> addCheckboxColumn(ValueProvider<T, Boolean> valueProvider, String caption, int expandRatio)
	{
		Column<T, CheckBox> column = grid.addCheckboxColumn(valueProvider, caption, expandRatio);
		grid.refreshActionColumn();
		return column;
	}

	public Column<T, ?> addTextColumn(ValueProvider<T, String> valueProvider, String caption, int expandRatio, boolean sortable, boolean hideable)

	{
		Column<T, String> column = grid.addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio)
				.setResizable(false).setSortable(sortable).setHidable(hideable).setHidden(hideable);
		grid.refreshActionColumn();
		return column;
	}

	public Column<T, Component> addGotoEditColumn(ValueProvider<T, String> valueProvider, String caption,
			int expandRatio)
	{
		Column<T, Component> column = grid
				.addComponentColumn(
						p -> !disableEdit.test(p)
								? StandardButtonsHelper.buildLinkButton(
										valueProvider.apply(p), e -> edit(p))
								: new Label(valueProvider.apply(p)),

						caption, expandRatio);
		grid.refreshActionColumn();
		return column;
	}
	
	public void sort(String columnId)
	{
		grid.sort(columnId);
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
	
	@Override
	protected Component initContent()
	{

		VerticalLayout main = new VerticalLayout();
		main.setHeight(100, Unit.PERCENTAGE);
		main.setMargin(false);
	
		addButtonBar.setWidth(100, Unit.PERCENTAGE);
		addButtonBar.setMargin(false);

		Button add = new Button(msg.getMessage("addNew"));
		add.addStyleName(Styles.buttonAction.toString());
		add.setIcon(Images.add.getResource());
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
				grid.scrollTo(grid.getElements().indexOf(newElement));
			}

		});

		addButtonBar.addComponent(add);
		addButtonBar.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);
		
		main.addComponent(addButtonBar);
		main.setExpandRatio(addButtonBar, 0);
		main.addComponent(grid);
		main.setExpandRatio(grid, 2);
		grid.getEditor().setEnabled(false);
		return main;

	}

	@Override
	protected void doSetValue(List<T> value)
	{
		grid.setItems(value);
		resetNewElement();
		grid.getElements().forEach(e -> grid.setDetailsVisible(e, false));
	}
	
	public void setDataProvider(FetchItemsCallback<T> fetchItems,
		            SerializableSupplier<Integer> sizeCallback)
	{
		grid.setDataProvider(fetchItems, sizeCallback);
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

	public static interface EmbeddedEditor<T> extends Component
	{
		T getValue() throws FormValidationException;

		void setValue(T t);
	}
}