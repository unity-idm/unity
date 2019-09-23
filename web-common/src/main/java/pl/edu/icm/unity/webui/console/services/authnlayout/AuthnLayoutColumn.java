/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.dnd.DropTargetExtension;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutColumn extends CustomComponent
{
	private UnityMessageSource msg;
	private List<ColumnElement> elements;
	private VerticalLayout elementsLayout;
	private Consumer<ColumnElement> removeElementListener;
	private Consumer<AuthnLayoutColumn> removeColumnListener;
	private List<Component> dropElements;
	private Button removeButton;
	private IntStepper columnWidthField;
	private I18nTextField columnTitleField;
	private Runnable valueChange;

	public AuthnLayoutColumn(UnityMessageSource msg, Consumer<AuthnLayoutColumn> removeListener,
			Consumer<ColumnElement> removeElementListener, Runnable valueChange)
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
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.setWidth(100, Unit.PERCENTAGE);
		setStyleName("u-border");

		HorizontalLayout headerBar = new HorizontalLayout();
		headerBar.setHeight(2, Unit.EM);
		headerBar.setMargin(false);
		headerBar.setWidth(100, Unit.PERCENTAGE);
		headerBar.addStyleName("u-columnHeader");
		
		HorizontalLayout spacing = new HorizontalLayout();
		spacing.setMargin(false);
		headerBar.addComponent(spacing);
		headerBar.setComponentAlignment(spacing, Alignment.MIDDLE_LEFT);
		
		Label captionL = new Label();
		captionL.addStyleName(Styles.bold.toString());
		captionL.setValue(msg.getMessage("LayoutColumn.column"));
		headerBar.addComponent(captionL);
		headerBar.setComponentAlignment(captionL, Alignment.MIDDLE_CENTER);
		
		HorizontalLayout removeWrapper = new HorizontalLayout();
		removeWrapper.setMargin(new MarginInfo(false, true));
		removeButton = new Button();
		removeButton.setDescription(msg.getMessage("LayoutColumn.removeColumn"));
		removeButton.addStyleName(Styles.vButtonLink.toString());
		removeButton.setIcon(Images.close_small.getResource());
		removeButton.addClickListener(e -> removeColumnListener.accept(this));
		removeWrapper.addComponent(removeButton);
		
		headerBar.addComponent(removeWrapper);
		headerBar.setComponentAlignment(removeWrapper, Alignment.MIDDLE_RIGHT);	
	
		main.addComponent(headerBar);

		columnTitleField = new I18nTextField(msg);
		columnTitleField.addValueChangeListener(e -> valueChange.run());
		columnTitleField.setCaption(msg.getMessage("LayoutColumn.title"));
		columnTitleField.setWidth(100, Unit.PERCENTAGE);
		columnTitleField.setWidth(17, Unit.EM);

		columnWidthField = new IntStepper();
		columnWidthField.setCaption(msg.getMessage("LayoutColumn.width"));
		columnWidthField.setWidth(3, Unit.EM);
		columnWidthField.setStyleName("u-maxWidth3");
		columnWidthField.setValue(15);
		columnWidthField.setMinValue(1);
		columnWidthField.addValueChangeListener(e -> valueChange.run());

		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(true);
		wrapper.addComponent(columnTitleField);
		wrapper.addComponent(columnWidthField);
		
		HorizontalLayout center = new HorizontalLayout();
		center.setMargin(false);
		center.setSpacing(false);
		center.setWidth(100, Unit.PERCENTAGE);
		center.addComponent(wrapper);
		center.setComponentAlignment(wrapper, Alignment.TOP_CENTER);
				
		main.addComponent(center);
		main.addComponent(HtmlTag.horizontalLine());

		elementsLayout = new VerticalLayout();
		elementsLayout.setMargin(true);
		elementsLayout.setWidth(100, Unit.PERCENTAGE);
		elementsLayout.setStyleName(Styles.vDropLayout.toString());
		main.addComponent(elementsLayout);
		
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
			
		refreshElements();
		dragOff();
	}
	
	public void removeElement(ColumnElement e)
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
			c.addStyleName("drop-on");
		}
	}

	public void dragOff()
	{
		for (Component c : dropElements)
		{
			c.setVisible(false);
			c.removeStyleName("drop-on");
		}
	}

	public List<ColumnElement> getElements()
	{
		return elements;
	}

	public void setElements(List<ColumnElement> elements)
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
		return columnTitleField.getValue();
	}

	public void setColumnTitle(I18nString columnTitle)
	{
		this.columnTitleField.setValue(columnTitle);
	}

	public void refreshElements()
	{
		elementsLayout.removeAllComponents();
		Component drop = getDropElement(0);
		dropElements.add(drop);
		elementsLayout.addComponent(drop);

		if (elements.size() == 0)
		{
			return;
		}

		for (ColumnElement r : elements)
		{
			if (r instanceof ColumnElementWithValue<?>)
			{
				ColumnElementWithValue<?> rv = (ColumnElementWithValue<?>) r;
				rv.refresh();
			}
			elementsLayout.addComponent(r);
			drop = getDropElement(elements.indexOf(r) + 1);
			dropElements.add(drop);
			elementsLayout.addComponent(drop);
		}
	}

	private HorizontalLayout getDropElement(int pos)
	{
		HorizontalLayout drop = new HorizontalLayout();
		drop.setWidth(100, Unit.PERCENTAGE);
		drop.setHeight(1, Unit.EM);
		drop.setVisible(false);

		DropTargetExtension<HorizontalLayout> dropTarget = new DropTargetExtension<>(drop);
		dropTarget.setDropEffect(DropEffect.MOVE);
		dropTarget.addDropListener(event -> {
			Optional<AbstractComponent> dragSource = event.getDragSourceComponent();
			if (dragSource.isPresent())
			{
				if (dragSource.get() instanceof PalleteElement)
				{
					event.getDragData().ifPresent(data -> {
						Supplier<?> sup =  (Supplier<?>) data;
						elements.add(pos, (ColumnElement) sup.get());
						refreshElements();

					});
				} else if (dragSource.get() instanceof ColumnElement)
				{
					event.getDragData().ifPresent(data -> {
						ColumnElement e = (ColumnElement) data;

						if (elements.contains(e))
						{
							elements.remove(e);
						} else
						{
							removeElementListener.accept(e);
						}

						elements.add(pos, e);
						refreshElements();

					});
				}
			}
		});

		return drop;
	}

	public void validateConfiguration() throws FormValidationException
	{
		for (ColumnElement e : elements)
		{
			if (e instanceof ColumnElementWithValue<?>)
			{
				((ColumnElementWithValue<?>) e).validate();
			}
		}
		
	}

}
