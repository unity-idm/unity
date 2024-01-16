/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Editor of {@link FormLayout}. The editor allows for incremental updating of
 * the current layout with changes in the upstream form: all required parameter
 * fields are added and all non existing parameter fields are removed.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class FormLayoutEditor extends VerticalLayout
{
	private final MessageSource msg;
	private final Supplier<FormLayout> layoutProvider;
	private final List<EntryComponent> entries;
	private VerticalLayout entriesLayout;

	public FormLayoutEditor(MessageSource msg, Supplier<FormLayout> layoutProvider)
	{
		super();
		this.msg = msg;
		this.layoutProvider = layoutProvider;
		this.entries = new ArrayList<>();
		initUI();
	}

	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(false);
		main.setPadding(false);

		Component layoutControls = getLayoutControls();
		main.add(layoutControls);
		
		add(main);
	}
	
	private Component getLayoutControls()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(false);
		main.setPadding(false);

		main.add(getAddMetaElementControl());
		main.add(new Hr());
		
		entriesLayout = new VerticalLayout();
		entriesLayout.setPadding(false);
		main.add(entriesLayout);
		return main;
	}

	private Component getAddMetaElementControl()
	{
		HorizontalLayout addElementLayout = new HorizontalLayout();

		Span label = new Span(msg.getMessage("FormLayoutEditor.addCaption"));
		addElementLayout.add(label);
		addElementLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		Select<FormLayoutElement> elementSelector = new Select<>();
		elementSelector.setItems(FormLayoutElement.SEPARATOR, FormLayoutElement.CAPTION);
		elementSelector.setValue(FormLayoutElement.CAPTION);

		Button add = new Button();
		add.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
		add.addClickListener(event -> {
			FormElement added = createExtraElementOfType(elementSelector.getValue());
			addComponentFor(added, 0);
			refreshComponents();
		});
		addElementLayout.add(elementSelector, add);
		return addElementLayout;
	}
	
	protected FormElement createExtraElementOfType(FormLayoutElement type)
	{
		return switch (type)
		{
			case CAPTION -> new FormCaptionElement(new I18nString());
			case SEPARATOR -> new FormSeparatorElement();
			default -> throw new IllegalStateException("Unsupported extra layout element type " + type);
		};
	}
	
	
	protected void addComponentFor(FormElement formElement, int index)
	{
		FormElementEditor<?> elementEditor = getElementEditor(formElement);
		EntryComponent component = new EntryComponent(index, 
				msg, elementEditor, new CallbackImpl());
		entries.add(index, component);
		entriesLayout.addComponentAtIndex(index, component);
	}
	
	private class CallbackImpl implements EntryComponent.Callback
	{
		@Override
		public void moveTo(int oldPosition, int newPosition)
		{
			EntryComponent moved = entries.get(oldPosition);
			EntryComponent oldAtTarget = entries.get(newPosition); 
			entries.set(oldPosition, oldAtTarget);
			entries.set(newPosition, moved);
			entriesLayout.replace(moved, oldAtTarget);
			refreshComponents();
		}

		@Override
		public void remove(int oldPosition)
		{
			EntryComponent removed = entries.remove(oldPosition);
			entriesLayout.remove(removed);
			refreshComponents();
		}
	}
	
	protected FormElementEditor<?> getElementEditor(FormElement formElement)
	{
		return switch (formElement.getType())
		{
			case CAPTION -> new CaptionElementEditor(msg, (FormCaptionElement) formElement);
			case AGREEMENT, ATTRIBUTE, CREDENTIAL, REMOTE_SIGNUP, REMOTE_SIGNUP_GRID, GROUP, IDENTITY ->
					new FormParameterElementEditor((FormParameterElement) formElement);
			default -> new DefaultElementEditor(formElement);
		};
	}
	
	private void refreshComponents()
	{
		for (int i=0; i<entries.size(); i++)
			entries.get(i).setPosition(i, entries.size());
	}
	
	
	public void setLayoutFromProvider()
	{
		if (layoutProvider == null)
			return;
		FormLayout formLayout = layoutProvider.get();
		setLayout(formLayout);
	}
	
	
	public void setLayout(FormLayout formLayout)
	{
		entries.clear();
		entriesLayout.removeAll();
		
		if (formLayout == null)
			return;
		
		for (int i = 0; i < formLayout.getElements().size(); i++)
		{
			FormElement formElement = formLayout.getElements().get(i);
			addComponentFor(formElement, i);
		}
		
		refreshComponents();
	}

	public FormLayout getLayout()
	{
		if (entries.isEmpty())
			return null;
		return new FormLayout(entries.stream()
				.map(entry -> entry.getEditor().getComponent())
				.collect(Collectors.toList()));
	}
}
