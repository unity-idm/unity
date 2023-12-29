/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements.grid;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.function.ValueProvider;

import io.imunity.vaadin.elements.ActionIconBuilder;

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
	private T newElement;
	private final Function<String, String> msg;
	private Button add;

	public GridWithEditor(Function<String, String> msg, Class<T> type)
	{
		this(msg, type, t -> false, true);
	}

	public GridWithEditor(Function<String, String> msg, Class<T> type, Predicate<T> disableRemovePredicate,
			boolean enableDrag)
	{
		this(msg, type, disableRemovePredicate, enableDrag, true, msg.apply("addNew"));
	}

	public GridWithEditor(Function<String, String> msg, Class<T> type, Predicate<T> disableRemovePredicate,
			boolean enableDrag, boolean addAsActionButton)
	{
		this(msg, type, disableRemovePredicate, enableDrag, addAsActionButton, msg.apply("addNew"));
	}

	public GridWithEditor(Function<String, String> msg, Class<T> type, Predicate<T> disableRemovePredicate,
			boolean enableDrag, boolean addAsActionButton, String addCaption)
	{
		this.msg = msg;

		add = new Button(addCaption);
		if (addAsActionButton)
			add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		add.setIcon(new Icon(VaadinIcon.PLUS_CIRCLE_O));

		SingleActionHandler<T> remove = SingleActionHandler.builder4Delete(msg, type)
				.withDisabledPredicate(disableRemovePredicate)
				.withHandler(r ->
				{
					grid.removeElement(r.iterator()
							.next());
					fireChange();
				})
				.build();

		grid = new GridWithActionColumn<>(msg, Arrays.asList(remove));// , enableDrag);
		grid.addDragEndListener(e -> fireChange());
		grid.setRowsDraggable(enableDrag);
		grid.getEditor()
				.addSaveListener(e ->
				{
					fireChange();
					resetNewElement();
				});
		grid.getEditor()
				.addCancelListener(e ->
				{
					fireChange();
					resetNewElement();
				});
		grid.getEditor()
				.setBinder(new Binder<>(type));
		grid.getEditor()
				.setBuffered(true);

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		HorizontalLayout buttonBar = new HorizontalLayout();
		buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		buttonBar.setWidthFull();
		buttonBar.setMargin(false);
		main.setSizeFull();

		add.addClickListener(e ->
		{
			if (!grid.getEditor()
					.isOpen())
			{
				newElement = newInstance(type);
				grid.addElement(newElement);
				grid.focus();
				grid.getEditor()
						.editItem(grid.getElements()
								.get(grid.getElements()
										.size() - 1));

			}
		});

		grid.addItemDoubleClickListener(e -> grid.getEditor()
				.editItem(e.getItem()));
		buttonBar.add(add);

		main.add(buttonBar);
		main.add(grid);
		grid.setWidthFull();
		setWidthFull();
		add(main);
	}

	private void resetNewElement()
	{
		newElement = null;
	}

	private void fireChange()
	{
		fireEvent(new ComponentValueChangeEvent<>(this, this, null, true));
	}

	public T newInstance(Class<T> cls)
	{
		try
		{
			return cls.getDeclaredConstructor()
					.newInstance();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public Column<T> addTextColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter, String caption,
			int expandRatio, boolean required)
	{
		return addTextColumn(valueProvider, setter, caption, expandRatio, required, Optional.empty());
	}

	public Column<T> addTextColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter, String caption,
			int expandRatio, boolean required, Optional<Validator<String>> validator)
	{

		return addTextColumn(valueProvider, setter, caption, expandRatio, required, validator, new TextField());
	}

	public Column<T> addTextColumn(ValueProvider<T, String> valueProvider, Setter<T, String> setter, String caption,
			int expandRatio, boolean required, Optional<Validator<String>> validator, TextField field)
	{

		Binder<T> binder = grid.getEditor()
				.getBinder();
		Column<T> column = grid.addColumn(valueProvider)
				.setHeader(caption)
				.setResizable(false)
				.setSortable(false)
				.setEditorComponent(field);
		binder.forField(field)
				.withValidator(validator.orElse((v, c) -> ValidationResult.ok()))
				.bind(valueProvider, setter);

		grid.refreshActionColumn();
		refreshEditor();

		return column;
	}

	private void refreshEditor()
	{
		Icon saveButton = new ActionIconBuilder().icon(VaadinIcon.CHECK)
				.tooltipText(msg.apply("save"))
				.clickListener(() -> grid.getEditor()
						.save())
				.build();
		Icon cancelButton = new ActionIconBuilder().icon(VaadinIcon.TRASH)
				.tooltipText(msg.apply("cancel"))
				.clickListener(() ->
				{
					grid.getEditor()
							.cancel();
					grid.removeElement(grid.getElements()
							.get(grid.getElements()
									.size() - 1));
				})
				.build();

		HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
		actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		grid.getActionColumn()
				.setEditorComponent(actions);

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

	protected List<T> generateModelValue()
	{
		return grid.getElements();
	}

	@Override
	protected void setPresentationValue(List<T> newPresentationValue)
	{
		if (newPresentationValue != null)
			grid.setItems(newPresentationValue);

	}

	public void setAllRowsVisible(boolean allRowsVisible)
	{
		grid.setAllRowsVisible(allRowsVisible);
	}

}