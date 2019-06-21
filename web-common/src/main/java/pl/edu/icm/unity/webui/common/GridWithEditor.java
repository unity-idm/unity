/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.server.Setter;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Grid with row editor. By default action column with delete button is added as
 * last grid column.
 * 
 * @author P.Piernik
 *
 * @param <T>
 */
public class GridWithEditor<T> extends CustomField<List<T>>
{
	private GridWithActionColumn<T> grid;
	private Class<T> type;
	private T newElement;
	private UnityMessageSource msg;

	public GridWithEditor(UnityMessageSource msg, Class<T> type)
	{
		this.type = type;
		this.msg = msg;

		SingleActionHandler<T> remove = SingleActionHandler.builder4Delete(msg, type).withHandler(r -> {
			grid.removeElement(r.iterator().next());
			fireChange();
		}).build();

		grid = new GridWithActionColumn<>(msg, Arrays.asList(remove));
		grid.setMinHeightByRow(3);
		grid.getEditor().addSaveListener(e -> {
			fireChange();
			resetNewElement();
		});
		grid.getEditor().addCancelListener(e -> {
			fireChange();
			if (newElement != null)
			{
				grid.removeElement(newElement);
			}
			resetNewElement();
		});
		grid.getEditor().setBinder(new Binder<>(type));
	}

	private void resetNewElement()
	{
		newElement = null;
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
			return null;
		}
		return myObject;
	}

	public Column<T, ?> addTextColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter,
			String caption, int expandRatio, boolean required)
	{
		return addTextColumn(valueProvider, setter, caption, expandRatio, required, Optional.empty());
	}

	public Column<T, ?> addTextColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter,
			String caption, int expandRatio, boolean required, Optional<Validator<String>> validator)
	{

		Binder<T> binder = grid.getEditor().getBinder();
		Column<T, String> column = grid.addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio)
				.setResizable(false).setSortable(false)
				.setEditorBinding(binder.forField(new TextField()).asRequired((v, c) -> {
					if (required && (v == null || v.isEmpty()))
					{
						return ValidationResult.error(msg.getMessage("fieldRequired"));
					} else
					{
						return ValidationResult.ok();
					}

				}).withValidator(validator.orElse((v, c) -> ValidationResult.ok())).bind(valueProvider,
						setter));

		grid.refreshActionColumn();
		return column;
	}

	public Column<T, ?> addIntColumn(ValueProvider<T, Integer> valueProvider, Setter<T, Integer> setter,
			String caption, int expandRatio, Optional<Validator<Integer>> validator)
	{

		Binder<T> binder = grid.getEditor().getBinder();
		Column<T, ?> column = grid.addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio)
				.setResizable(false).setSortable(false)
				.setEditorBinding(binder.forField(new TextField())
						.asRequired(msg.getMessage("fieldRequired"))
						.withConverter(new StringToIntegerConverter(
								msg.getMessage("notANumber")))
						.withValidator(validator.orElse((v, c) -> ValidationResult.ok()))
						.bind(valueProvider, setter));

		grid.refreshActionColumn();
		return column;
	}

	public Column<T, ?> addComboColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter, String caption,
			List<String> items, int expandRatio, boolean emptyAllowed)
	{
		ComboBox<String> field = new ComboBox<String>();
		field.setItems(items);
		field.setEmptySelectionAllowed(emptyAllowed);

		Binder<T> binder = grid.getEditor().getBinder();
		Column<T, ?> column = grid.addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio)
				.setResizable(false).setSortable(false)
				.setEditorBinding(binder.forField(field).bind(valueProvider, setter));

		grid.refreshActionColumn();
		return column;
	}

	public <V extends Enum<?>> Column<T, V> addComboColumn(ValueProvider<T, V> valueProvider, Setter<T, V> setter,
			Class<V> enumClass, String caption, int expandRatio)
	{
		ComboBox<V> field = new ComboBox<V>();
		V[] consts = enumClass.getEnumConstants();
		field.setItems(Arrays.asList(consts));
		field.setEmptySelectionAllowed(false);

		Binder<T> binder = grid.getEditor().getBinder();
		Column<T, V> column = grid.addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio)
				.setResizable(false).setSortable(false)
				.setEditorBinding(binder.forField(field).asRequired(msg.getMessage("fieldRequired"))
						.bind(valueProvider, setter));

		grid.refreshActionColumn();
		return column;
	}

	public <V> Column<T, V> addCustomColumn(ValueProvider<T, V> valueProvider,
			ValueProvider<V, String> presentationProvider, Setter<T, V> setter, HasValue<V> field,
			String caption, int expandRatio)
	{
		Binder<T> binder = grid.getEditor().getBinder();
		Column<T, V> column = grid.addColumn(valueProvider, presentationProvider).setCaption(caption)
				.setExpandRatio(expandRatio).setResizable(false).setSortable(false)
				.setEditorBinding(binder.forField(field).asRequired(msg.getMessage("fieldRequired"))
						.bind(valueProvider, setter));

		grid.refreshActionColumn();
		return column;
	}

	public void addElement(T el)
	{
		grid.addElement(el);
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
		HorizontalLayout buttonBar = new HorizontalLayout();
		buttonBar.setWidth(100, Unit.PERCENTAGE);
		buttonBar.setMargin(false);

		Button add = new Button();
		add.setIcon(Images.add.getResource());
		add.addClickListener(e -> {
			if (!grid.getEditor().isOpen())
			{
				newElement = newInstance(type);
				grid.addElement(newElement);
				grid.focus();
				grid.getEditor().editRow(grid.getElements().size() - 1);

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

}