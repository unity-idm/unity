/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.layout.FormElement;

/**
 * Allows for editing a single entry of layout. The contents is provided by a provided LayoutEntryEditor 
 * this class adds a generic functionality: up/down arrows and optional remove button.
 * 
 * @author K. Benedyczak
 */
class EntryComponent extends VerticalLayout
{
	private MessageSource msg;
	private int position;
	private Icon up;
	private Icon down;
	private Icon remove;
	private Callback callback;
	private FormElementEditor<?> elementEditor;
	
	
	EntryComponent(int position, MessageSource msg,
			FormElementEditor<?> elementEditor, Callback callback)
	{
		this.position = position;
		this.msg = msg;
		this.elementEditor = elementEditor;
		this.callback = callback;
		
		initUI();
	}
	
	private void initUI()
	{
		setPadding(false);
		up = VaadinIcon.ANGLE_UP.create();
		up.setTooltipText(msg.getMessage("TranslationProfileEditor.moveUp"));
		up.addClickListener(event -> move(-1));
		
		down = VaadinIcon.ANGLE_DOWN.create();
		down.setTooltipText(msg.getMessage("TranslationProfileEditor.moveDown"));
		down.addClickListener(event -> move(1));
		
		remove = VaadinIcon.TRASH.create();
		remove.setTooltipText(msg.getMessage("TranslationProfileEditor.remove"));
		remove.addClickListener(event -> callback.remove(position));
		remove.setVisible(!elementEditor.getComponent().isFormContentsRelated());
		
		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.setPadding(false);
		toolbar.setSpacing(false);
		Span space0 = new Span();
		Span space = new Span();
		toolbar.add(up, down, remove, space0);
		toolbar.setWidth(16*3+60, Unit.PIXELS);
		
		HorizontalLayout main = new HorizontalLayout();
		main.setSpacing(false);
		main.setMargin(false);
		main.add(toolbar);
		main.add((Component) elementEditor);
		main.add(space);

		add(main);
	}

	private void move(int shift)
	{
		callback.moveTo(position, position+shift);
	}
	
	void setPosition(int position, int totalElements)
	{
		this.position = position;
		down.setVisible(position < totalElements -1);
		up.setVisible(position > 0);
	}
	
	FormElementEditor<? extends FormElement> getEditor()
	{
		return elementEditor;
	}
	
	interface Callback
	{
		void moveTo(int oldPosition, int newPosition);
		void remove(int oldPosition);
	}
}
