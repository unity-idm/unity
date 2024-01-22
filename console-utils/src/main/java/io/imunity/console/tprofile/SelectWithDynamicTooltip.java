/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.tprofile;

import java.util.function.Consumer;

import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.Tooltip;

class SelectWithDynamicTooltip<T> extends Select<T>
{
	Consumer<String> tooltipChangeListener;
	
	void setTooltipChangeListener(Consumer<String> tooltipChangeListener)
	{
		this.tooltipChangeListener = tooltipChangeListener;
	}
	
	@Override
	public Tooltip setTooltipText(String text)
	{
		if (tooltipChangeListener != null)
		{
			tooltipChangeListener.accept(text);
			return getTooltip();
		}
		
		return super.setTooltipText(text);
	}
}
