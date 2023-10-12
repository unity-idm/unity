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
	private boolean enabled = true;
	private String tooltipText;
	private Class<? extends UnityViewComponent> navigationTarget;
	private String navigationParameter;
	private Runnable listener;

	public Icon build()
	{
		Icon targetIcon = icon.create();
		targetIcon.setTooltipText(tooltipText);
		if(navigationTarget != null && navigationParameter != null)
			targetIcon.addClickListener(e -> UI.getCurrent().navigate(navigationTarget, navigationParameter));
		if(listener != null)
			targetIcon.addClickListener(e -> listener.run());
		if(enabled)
			targetIcon.setClassName("pointer");
		else
			targetIcon.setClassName("disabled-icon");
		targetIcon.getElement().setAttribute("onclick", "event.stopPropagation();");
		return targetIcon;
	}

	public ActionIconBuilder icon(VaadinIcon icon)
	{
		this.icon = icon;
		return this;
	}

	public ActionIconBuilder tooltipText(String tooltipText)
	{
		this.tooltipText = tooltipText;
		return this;
	}

	public ActionIconBuilder navigation(Class<? extends UnityViewComponent> navigationTarget, String navigationParameter)
	{
		this.navigationTarget = navigationTarget;
		this.navigationParameter = navigationParameter;
		return this;
	}

	public ActionIconBuilder clickListener(Runnable task)
	{
		this.listener = task;
		return this;
	}

	public ActionIconBuilder enabled(boolean enabled)
	{
		this.enabled = enabled;
		return this;
	}
}
