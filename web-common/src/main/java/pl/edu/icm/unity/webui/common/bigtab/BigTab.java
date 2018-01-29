/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.bigtab;

import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * A single clickable big tab. {@link BigTabs} groups this object instances. 
 * @author K. Benedyczak
 */
public class BigTab extends VerticalLayout
{
	private final TabCallback callback;
	
	public BigTab(int width, Unit unit, String label, String description, Images image, TabCallback callback)
	{
		this.callback = callback;
		if (image != null)
		{
			Label img = new Label(image.getHtml());
			img.setContentMode(ContentMode.HTML);
			img.addStyleName(Styles.veryLargeIcon.toString());
			addComponent(img);
			setComponentAlignment(img, Alignment.TOP_CENTER);
		}
		if (description != null)
			setDescription(description);
		if (label != null)
		{
			Label info = new Label(label);
			info.setWidth(width, unit);
			info.addStyleName(Styles.textCenter.toString());
			addComponent(info);
			setComponentAlignment(info, Alignment.TOP_CENTER);
		}
		
		addStyleName(Styles.bigTab.toString());
		addLayoutClickListener(new LayoutClickListener()
		{
			@Override
			public void layoutClick(LayoutClickEvent event)
			{
				select();
			}
		});
		setMargin(new MarginInfo(true, false, true, false));
		setSpacing(false);
	}
	
	public void deselect()
	{
		addStyleName(Styles.bigTab.toString());
		removeStyleName(Styles.bigTabSelected.toString());
	}
	
	public void select()
	{
		callback.onSelection(this);
		removeStyleName(Styles.bigTab.toString());
		addStyleName(Styles.bigTabSelected.toString());
	}
	
	public interface TabCallback
	{
		public void onSelection(BigTab src);
	}
}
