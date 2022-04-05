/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

import java.util.function.Consumer;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.webui.common.Styles;

public class WarnComponent extends HorizontalLayout
{
	
	
	private Consumer<Boolean> visibleChangeListener;

	public WarnComponent()
	{
		setWidth(100, Unit.PERCENTAGE);
		setMargin(new MarginInfo(false, true));
		setSpacing(true);
		setStyleName(Styles.warnBar.toString());
		setVisible(false);
	}
	
	void setWarn(String info)
	{
		setVisible(true);
		removeAllComponents();
		addComponent(new Label(info));
	}

	public void addVisibleChangeListener(Consumer<Boolean> visibleChangeListener)
	{
		
		this.visibleChangeListener = visibleChangeListener;
	}
	
	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visibleChangeListener != null)
		{
			visibleChangeListener.accept(visible);
		}
	}
}
