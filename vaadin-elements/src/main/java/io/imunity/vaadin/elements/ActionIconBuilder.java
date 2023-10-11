/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;


public class ActionIconBuilder
{
	private VaadinIcon icon;
	private boolean garyIcon;
	private String tooltipText;
	private Class<? extends UnityViewComponent> navigationTarget;
	private String navigationParameter;
	private Runnable listener;

	public Icon build()
	{
		Icon targetIcon = icon.create();
		targetIcon.setTooltipText(tooltipText);
		if(navigationTarget != null || navigationParameter != null)
			targetIcon.addClickListener(e -> UI.getCurrent().navigate(navigationTarget, navigationParameter));
		if(listener != null)
			targetIcon.addClickListener(e -> listener.run());
		if(garyIcon)
			targetIcon.getStyle().set("opacity", "0.5");
		targetIcon.getStyle().set("cursor", "pointer");
		targetIcon.getElement().setAttribute("onclick", "event.stopPropagation();");
		return targetIcon;
	}

	public ActionIconBuilder setIcon(VaadinIcon icon)
	{
		this.icon = icon;
		return this;
	}

	public ActionIconBuilder setTooltipText(String tooltipText)
	{
		this.tooltipText = tooltipText;
		return this;
	}

	public ActionIconBuilder setNavigation(Class<? extends UnityViewComponent> navigationTarget, String navigationParameter)
	{
		this.navigationTarget = navigationTarget;
		this.navigationParameter = navigationParameter;
		return this;
	}

	public ActionIconBuilder setClickListener(Runnable task)
	{
		this.listener = task;
		return this;
	}

	public ActionIconBuilder grayIcon(boolean garyIcon)
	{
		this.garyIcon = garyIcon;
		return this;
	}
}
