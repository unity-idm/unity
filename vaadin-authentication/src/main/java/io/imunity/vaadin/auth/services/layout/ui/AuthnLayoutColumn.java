/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Presents single column with elements.
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutColumn extends VerticalLayout
{
	private final  MessageSource msg;	
	private final Consumer<ColumnComponent> removeElementListener;
	private final Consumer<AuthnLayoutColumn> removeColumnListener;
	private final Runnable valueChange;
	private final List<Component> dropElements;
	
	private Icon removeButton;
	private IntegerField columnWidthField;
	private LocalizedTextFieldDetails columnTitleField;
	private VerticalLayout header;
	private List<ColumnComponent> elements;
	private VerticalLayout elementsLayout;
	public AuthnLayoutColumn(MessageSource msg, Consumer<AuthnLayoutColumn> removeListener,
			Consumer<ColumnComponent> removeElementListener, Runnable valueChange)
	{
		this.msg = msg;
		this.elements = new ArrayList<>();
		this.dropElements = new ArrayList<>();
		this.removeElementListener = removeElementListener;
		this.removeColumnListener = removeListener;
		this.valueChange = valueChange;
		initUI();
	}

	private void initUI()
	{
		
		setMargin(false);
		setSpacing(false);
		setPadding(false);
		setWidthFull();
		addClassName("u-border");

		HorizontalLayout headerBar = new HorizontalLayout();
		headerBar.setHeight(2, Unit.EM);
		headerBar.setWidthFull();
		headerBar.setPadding(true);
		headerBar.addClassName("u-columnHeader");

		NativeLabel captionL = new NativeLabel();
		captionL.addClassName(CssClassNames.BOLD.getName());
		captionL.setText(msg.getMessage("LayoutColumn.column"));
		headerBar.add(captionL);

		removeButton = new Icon(VaadinIcon.CLOSE_SMALL);
		removeButton.setTooltipText(msg.getMessage("LayoutColumn.removeColumn"));
		removeButton.addClickListener(e -> removeColumnListener.accept(this));

		headerBar.add(removeButton);
		headerBar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
		headerBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

		add(headerBar);

		columnTitleField = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		columnTitleField.addValueChangeListener(e -> valueChange.run());
		columnTitleField.setLabel(msg.getMessage("LayoutColumn.title"));
		columnTitleField.setWidthFull();
		columnTitleField.setWidth(17, Unit.EM);
		columnTitleField.centerIcons();
		
		columnWidthField = new IntegerField();
		columnWidthField.setLabel(msg.getMessage("LayoutColumn.width"));
		columnWidthField.setWidth(7, Unit.EM);
		columnWidthField.setValue(15);
		columnWidthField.setMin(1);
		columnWidthField.setStepButtonsVisible(true);
		columnWidthField.addValueChangeListener(e -> valueChange.run());

		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(false);
		wrapper.setPadding(true);
		wrapper.add(columnTitleField);
		wrapper.add(columnWidthField);

		HorizontalLayout center = new HorizontalLayout();
		center.setMargin(false);
		center.setSpacing(false);
		center.setWidthFull();
		center.add(wrapper);
		center.setJustifyContentMode(JustifyContentMode.CENTER);
		
		
		header = new VerticalLayout();
		header.setMargin(false);
		header.setPadding(false);
		header.add(center);
		Div bar = new Div();
		bar.addClassName("u-horizontalBar");
		header.add(bar);

		add(header);

		elementsLayout = new VerticalLayout();
		elementsLayout.setMargin(false);
		elementsLayout.setPadding(true);
		elementsLayout.setWidthFull();
		add(elementsLayout);


		refreshElements();
		dragOff();
	}

	public void removeElement(ColumnComponent e)
	{
		elements.remove(e);
		refreshElements();
	}

	public void setRemoveVisible(boolean visible)
	{
		removeButton.setVisible(visible);
	}

	public void dragOn()
	{
		for (Component c : dropElements)
		{
			c.setVisible(true);
			c.addClassName(CssClassNames.DROP_LAYOUT.getName());
		}
	}

	public void dragOff()
	{
		for (Component c : dropElements)
		{
			c.setVisible(false);
			c.removeClassName(CssClassNames.DROP_LAYOUT.getName());
		}
	}

	public List<ColumnComponent> getElements()
	{
		return elements;
	}

	public void setElements(List<ColumnComponent> elements)
	{
		this.elements = elements;
		refreshElements();
	}

	public int getColumnWidth()
	{
		return columnWidthField.getValue();
	}

	public void setColumnWidth(int columnWidth)
	{
		this.columnWidthField.setValue(columnWidth);
	}

	public I18nString getColumnTitle()
	{
		return new I18nString(columnTitleField.getValue());
	}

	public void setColumnTitle(I18nString columnTitle)
	{
		this.columnTitleField.setValue(columnTitle.getLocalizedMap());
	}

	public void refreshElements()
	{
		elementsLayout.removeAll();
		Component drop = getDropElement(0);
		dropElements.add(drop);
		elementsLayout.add(drop);

		for (int pos=0; pos<elements.size(); pos++)
		{
			ColumnComponent columnComponent = elements.get(pos);
			columnComponent.refresh();
			elementsLayout.add(columnComponent.getComponent());
			drop = getDropElement(pos + 1);
			dropElements.add(drop);
			elementsLayout.add(drop);
		}
	}

	private HorizontalLayout getDropElement(int pos)
	{
		HorizontalLayout drop = new HorizontalLayout();
		//drop.setClassName("u-dropOn");
		drop.setWidthFull();
		drop.setHeight(1, Unit.EM);
		drop.setVisible(false);

		DropTarget<HorizontalLayout> dropTarget = DropTarget.create(drop);
		dropTarget.setDropEffect(DropEffect.MOVE);
		dropTarget.addDropListener(event -> {
			Optional<Component> dragSource = event.getDragSourceComponent();
			if (!dragSource.isPresent())
				return;
			Component dragged = dragSource.get();
			if (dragged instanceof PaletteButton)
			{
				event.getDragData().ifPresent(data -> {
					Supplier<?> sup = (Supplier<?>) data;
					elements.add(pos, (ColumnComponent) sup.get());
					refreshElements();

				});
			} else if (dragged instanceof ColumnComponent)
			{
				event.getDragData().ifPresent(data -> 
				{
					ColumnComponent e = (ColumnComponent) data;

					int indexOfDragged = elements.indexOf(e);
					boolean moveUp = true;
					if (indexOfDragged != -1)
					{
						elements.remove(e);
						moveUp = indexOfDragged >= pos;
					} else
					{
						removeElementListener.accept(e);
					}

					int targetPosition = moveUp ? pos : pos - 1;
					elements.add(targetPosition, e);
					refreshElements();

				});
			}
		});

		return drop;
	}

	public void validateConfiguration() throws FormValidationException
	{
		for (ColumnComponent e : elements)
		{
			e.validate();
		}

	}

	public void setHeaderVisible(boolean visible)
	{
		header.setVisible(visible);

	}

}
