/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.chips;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Component allowing to show a label on a gray background with a 'x' button on right. 
 * Button can have action listener attached. Chip may carry arbitrary data value.
 *  
 * @author K. Benedyczak
 */
public class Chip<T> extends CustomComponent
{
	private Button remove;
	private T data;

	public Chip(String label, T value)
	{
		this.data = value;
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(false);
		wrapper.setSpacing(false);
		wrapper.setWidthUndefined();
		
		Label labelC = new Label(label);
		wrapper.addComponent(labelC);
		
		remove = new Button();
		remove.addStyleName(Styles.vButtonLink.toString());
		remove.setIcon(Images.close_small.getResource());
		remove.setData(value);
		wrapper.addComponent(remove);
		
		addStyleName("u-chip");
		setWidthUndefined();
		setCompositionRoot(wrapper);
	}
	
	public void addRemovalListener(ClickListener listener)
	{
		remove.addClickListener(listener);
	}
	
	public T getValue()
	{
		return data;
	}
	
	public void setReadOnly(boolean readOnly)
	{
		remove.setVisible(!readOnly);
	}
}
