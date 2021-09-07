/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.Setter;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.Editor;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;

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
	private MessageSource msg;

	public GridWithEditor(MessageSource msg, Class<T> type)
	{
		this(msg, type, t -> false, true);
	}
	
	
	
	public GridWithEditor(MessageSource msg, Class<T> type, Predicate<T> disableRemovePredicate, boolean enableDrag)
	{
		this.type = type;
		this.msg = msg;

		SingleActionHandler<T> remove = SingleActionHandler.builder4Delete(msg, type)
				.withDisabledPredicate(disableRemovePredicate).withHandler(r -> {
			grid.removeElement(r.iterator().next());
			fireChange();
		}).build();

		grid = new GridWithActionColumn<>(msg, Arrays.asList(remove), enableDrag);
		if (enableDrag)
		{
			grid.getRowDragger().getGridDragSource().addDragStartListener(e -> {
				if (isEditMode())
				{
					grid.getEditor().cancel();
				}
			});
		}
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
		setComponentError(null);
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

	public Column<T, ?> addTextColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter,
			String caption, int expandRatio, boolean required)
	{
		return addTextColumn(valueProvider, setter, caption, expandRatio, required, Optional.empty());
	}

	public Column<T, ?> addTextColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter,
			String caption, int expandRatio, boolean required, Optional<Validator<String>> validator)
	{

		return addTextColumn(valueProvider, setter, caption, expandRatio, required, validator, new TextField());
	}
	
	public Column<T, ?> addTextColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter,
			String caption, int expandRatio, boolean required, Optional<Validator<String>> validator, TextField field)
	{

		Binder<T> binder = grid.getEditor().getBinder();
		Column<T, String> column = grid.addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio)
				.setResizable(false).setSortable(false)
				.setEditorBinding(binder.forField(field).asRequired((v, c) -> {
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

	public Column<T, ?> addComboColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter,
			String caption, List<String> items, int expandRatio, boolean emptyAllowed)
	{
		ComboBox<String> field = new ComboBox<String>();
		field.setItems(items);
		field.setEmptySelectionAllowed(emptyAllowed);

		return addComboColumn(valueProvider, setter, caption, field, expandRatio, emptyAllowed);
	}
	
	public Column<T, ?> addComboColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter,
			String caption, ComboBox<String> field, int expandRatio, boolean emptyAllowed)
	{
		field.setEmptySelectionAllowed(emptyAllowed);
		Binder<T> binder = grid.getEditor().getBinder();
		Column<T, ?> column = grid.addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio)
				.setResizable(false).setSortable(false)
				.setEditorBinding(binder.forField(field).asRequired((v, c) -> {
					if (!emptyAllowed && (v == null || v.isEmpty()))
					{
						return ValidationResult.error(msg.getMessage("fieldRequired"));
					} else
					{
						return ValidationResult.ok();
					}
				}).bind(valueProvider, setter));

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

	public Column<T, ?> addCheckBoxColumn(ValueProvider<T, Boolean> valueProvider, Setter<T, Boolean> setter,
			String caption, int expandRatio)
	{
		CheckBox field = new CheckBox();

		Binder<T> binder = grid.getEditor().getBinder();
		Column<T, ?> column = grid.addComponentColumn(t -> {
			CheckBox val = new CheckBox();
			val.setValue(valueProvider.apply(t));
			val.setReadOnly(true);
			return val;
		}).setCaption(caption).setExpandRatio(expandRatio).setResizable(false).setSortable(false)
				.setEditorBinding(binder.forField(field).bind(valueProvider, setter));

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
	
	public void removeElement(T el)
	{
		grid.removeElement(el);
		fireChange();
	}
	
	public void removeAllElements()
	{
		grid.removeAllElements();
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

		Button add = new Button(msg.getMessage("addNew"));
		add.addStyleName(Styles.buttonAction.toString());
		add.setIcon(Images.add.getResource());
		add.addClickListener(e -> {
			grid.setComponentError(null);
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

	@Override
	public void setComponentError(ErrorMessage componentError)
	{	
		if (grid != null)
		{
			grid.setComponentError(componentError);
		}else
		{
			super.setComponentError(componentError);
		}
	}
	
	public Editor<T> getEditor()
	{
		return grid.getEditor();
	}
	
	public boolean isEditMode()
	{
		return grid.getEditor().isOpen();
	}	
}