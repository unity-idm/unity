/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLayoutElement;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.types.registration.layout.FormSeparatorElement;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Editor of {@link FormLayout}. The editor allows for incremental updating of
 * the current layout with changes in the upstream form: all required parameter
 * fields are added and all non existing parameter fields are removed.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class FormLayoutEditor extends CustomComponent
{
	private UnityMessageSource msg;
	private Supplier<FormLayout> layoutProvider;
	
	private Component layoutControls;
	private VerticalLayout main;
	private List<EntryComponent> entries;
	private VerticalLayout entriesLayout;

	public FormLayoutEditor(UnityMessageSource msg, Supplier<FormLayout> layoutProvider)
	{
		super();
		this.msg = msg;
		this.layoutProvider = layoutProvider;
		this.entries = new ArrayList<>();
		initUI();
	}

	private void initUI()
	{
		main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(true);
		
		layoutControls = getLayoutControls();
		main.addComponent(layoutControls);
		
		setCompositionRoot(main);
	}
	
	private Component getLayoutControls()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		
		main.addComponent(getAddMetaElementControl());
		main.addComponent(HtmlTag.horizontalLine());
		
		entriesLayout = new VerticalLayout();
		entriesLayout.setSpacing(true);
		entriesLayout.setMargin(false);
		main.addComponent(entriesLayout);
		return main;
	}

	private Component getAddMetaElementControl()
	{
		HorizontalLayout addElementLayout = new HorizontalLayout();
		addElementLayout.setSpacing(true);
		addElementLayout.setMargin(false);
		
		Label label = new Label(msg.getMessage("FormLayoutEditor.addCaption"));
		addElementLayout.addComponent(label);
		addElementLayout.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
		ComboBox<FormLayoutElement> elementSelector = new ComboBox<>();
		elementSelector.setItems(FormLayoutElement.SEPARATOR, FormLayoutElement.CAPTION);
		elementSelector.setSelectedItem(FormLayoutElement.CAPTION);
		elementSelector.setEmptySelectionAllowed(false);
		
		Button add = new Button();
		add.setIcon(Images.add.getResource());
		add.addClickListener(event -> {
			FormElement added = createExtraElementOfType(elementSelector.getValue());
			addComponentFor(added, 0);
			refreshComponents();
		});
		addElementLayout.addComponents(elementSelector, add);
		return addElementLayout;
	}
	
	protected FormElement createExtraElementOfType(FormLayoutElement type)
	{
		switch (type)
		{
		case CAPTION:
			return new FormCaptionElement(new I18nString());
		case SEPARATOR:
			return new FormSeparatorElement();
		default: 
			throw new IllegalStateException("Unsupported extra layout element type " + type);
		}
	}
	
	
	protected void addComponentFor(FormElement formElement, int index)
	{
		FormElementEditor<?> elementEditor = getElementEditor(formElement);
		EntryComponent component = new EntryComponent(index, 
				msg, elementEditor, new CallbackImpl());
		entries.add(index, component);
		entriesLayout.addComponent(component, index);
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
			entriesLayout.replaceComponent(moved, oldAtTarget);
			refreshComponents();
		}

		@Override
		public void remove(int oldPosition)
		{
			EntryComponent removed = entries.remove(oldPosition);
			entriesLayout.removeComponent(removed);
			refreshComponents();
		}
	}
	
	protected FormElementEditor<?> getElementEditor(FormElement formElement)
	{
		switch (formElement.getType())
		{
		case CAPTION:
			return new CaptionElementEditor(msg, (FormCaptionElement) formElement);
		case AGREEMENT:
		case ATTRIBUTE:
		case CREDENTIAL:
		case REMOTE_SIGNUP:
		case REMOTE_SIGNUP_GRID:
		case GROUP:
		case IDENTITY:
			return new FormParameterElementEditor((FormParameterElement) formElement);
			
		default: 
			return new DefaultElementEditor(formElement);
		}
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
		entriesLayout.removeAllComponents();
		
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
				.map(entry -> entry.getEditor().getElement())
				.collect(Collectors.toList()));
	}
}
