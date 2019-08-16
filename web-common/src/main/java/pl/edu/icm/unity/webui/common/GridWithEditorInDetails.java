/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.vaadin.data.ValueProvider;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Grid with row editor. By default action column with delete button is added as
 * last grid column.
 * 
 * @author P.Piernik
 *
 * @param <T>
 */
public class GridWithEditorInDetails<T> extends CustomField<List<T>>
{
	private GridWithActionColumn<T> grid;
	private Class<T> type;
	private T newElement;

	public GridWithEditorInDetails(UnityMessageSource msg, Class<T> type,
			Supplier<EmbeddedEditor<T>> gridEditorSupplier, Predicate<T> disableEditAndRemovePredicate)
	{
		this.type = type;
		SingleActionHandler<T> remove = SingleActionHandler.builder4Delete(msg, type)
			.withDisabledPredicate(disableEditAndRemovePredicate)	
			.withHandler(r -> {
			T element = r.iterator().next();
			grid.removeElement(element);
			if (newElement == element)
			{
				resetNewElement();
			}
			fireChange();
		}).build();

		SingleActionHandler<T> edit = SingleActionHandler.builder4Edit(msg, type)
				.withDisabledPredicate(t -> grid.isDetailsVisible(t) || disableEditAndRemovePredicate.test(t)).withHandler(r -> {
					for (T t : grid.getElements())
					{
						grid.setDetailsVisible(t, false);
					}
					if (newElement != null)
					{
						grid.removeElement(newElement);
					}
					resetNewElement();
					grid.setDetailsVisible(r.iterator().next(), true);
				}).build();

		grid = new GridWithActionColumn<>(msg, Arrays.asList(edit, remove));
		grid.setDetailsGenerator(t -> {
			VerticalLayout wrapper = new VerticalLayout();
			wrapper.setMargin(false);
			wrapper.setSpacing(false);
			EmbeddedEditor<T> editor = gridEditorSupplier.get();
			editor.setValue(t);
			editor.addStyleName("u-gridEmbeddedEditor");
			wrapper.addComponent(editor);
			HorizontalLayout buttons;
			if (newElement == t)
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
					fireChange();

				}, () -> {
					grid.setDetailsVisible(t, false);
				}, Alignment.MIDDLE_RIGHT);

			}
			buttons.setMargin(false);
			wrapper.addComponent(buttons);
			return wrapper;
		});
		setHeight(100, Unit.PERCENTAGE);
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

	private void fireChange()
	{
		fireEvent(new ValueChangeEvent<List<T>>(this, grid.getElements(), true));
	}

	public T newInstance(Class<T> cls)
	{
		T myObject;
		try
		{
			myObject = cls.newInstance();
		} catch (Exception e)
		{
			throw new InternalException(e.getMessage());
		}

		return myObject;
	}

	public Column<T, ?> addTextColumn(ValueProvider<T, String> valueProvider, String caption, int expandRatio)

	{
		Column<T, String> column = grid.addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio)
				.setResizable(false).setSortable(false);
		grid.refreshActionColumn();
		return column;
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

	@Override
	public List<T> getValue()
	{
		return grid.getElements();
	}

	@Override
	protected Component initContent()
	{

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		HorizontalLayout buttonBar = new HorizontalLayout();
		buttonBar.setWidth(100, Unit.PERCENTAGE);
		buttonBar.setMargin(false);

		Button add = new Button();
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
			}

		});

		buttonBar.addComponent(add);
		buttonBar.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);
		main.addComponent(buttonBar);
		main.addComponent(grid);
		grid.getEditor().setEnabled(true);
		return main;

	}

	@Override
	protected void doSetValue(List<T> value)
	{
		grid.setItems(value);
	}

	public static interface EmbeddedEditor<T> extends Component
	{
		T getValue() throws FormValidationException;

		void setValue(T t);
	}

}