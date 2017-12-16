/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman.layout;

import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Allows for editing a single entry of layout. The contents is provided by a provided LayoutEntryEditor 
 * this class adds a generic functionality: up/down arrows and optional remove button.
 * 
 * @author K. Benedyczak
 */
public class EntryComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private int position;
	private Button up;
	private Button down;
	private Button remove;
	private Callback callback;
	private FormElementEditor<?> elementEditor;
	
	
	public EntryComponent(int position, UnityMessageSource msg,
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
		up = new Button();
		up.setDescription(msg.getMessage("TranslationProfileEditor.moveUp"));
		up.setIcon(Images.upArrow.getResource());
		up.addStyleName(Styles.vButtonLink.toString());
		up.addStyleName(Styles.toolbarButton.toString());
		up.addClickListener(event -> move(-1));
		
		down = new Button();
		down.setDescription(msg.getMessage("TranslationProfileEditor.moveDown"));
		down.setIcon(Images.downArrow.getResource());
		down.addStyleName(Styles.vButtonLink.toString());
		down.addStyleName(Styles.toolbarButton.toString());
		down.addClickListener(event -> move(1));
		
		remove = new Button();
		remove.setDescription(msg.getMessage("TranslationProfileEditor.remove"));
		remove.setIcon(Images.delete.getResource());
		remove.addStyleName(Styles.vButtonLink.toString());
		remove.addStyleName(Styles.toolbarButton.toString());
		remove.addClickListener(event -> callback.remove(position));
		remove.setVisible(!elementEditor.getElement().isFormContentsRelated());
		
		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.setMargin(false);
		toolbar.setSpacing(false);
		Label space0 = new Label();
		Label space = new Label();
		toolbar.addComponents(up, down, remove, space0);
		toolbar.setExpandRatio(space0, 3);
		toolbar.setWidth(16*3+60, Unit.PIXELS);
		
		HorizontalLayout main = new HorizontalLayout();
		main.setSpacing(false);
		main.setMargin(false);
		main.addComponent(toolbar);
		main.addComponent(elementEditor);
		main.addComponent(space);
		main.setExpandRatio(space, 3);
		
		setCompositionRoot(main);
	}

	private void move(int shift)
	{
		callback.moveTo(position, position+shift);
	}
	
	public void setPosition(int position, int totalElements)
	{
		this.position = position;
		down.setVisible(position < totalElements -1);
		up.setVisible(position > 0);
	}
	
	public FormElementEditor<? extends FormElement> getEditor()
	{
		return elementEditor;
	}
	
	public interface Callback
	{
		void moveTo(int oldPosition, int newPosition);
		void remove(int oldPosition);
	}
}
