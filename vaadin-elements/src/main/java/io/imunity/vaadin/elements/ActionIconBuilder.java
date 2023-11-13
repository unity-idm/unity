/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import static io.imunity.vaadin.elements.VaadinClassNames.DISABLED_ICON;
import static io.imunity.vaadin.elements.VaadinClassNames.POINTER;


public class ActionIconBuilder
{
	private VaadinIcon icon;
	private boolean enabled = true;
	private String tooltipText;
	private Class<? extends UnityViewComponent> navigationTarget;
	private String navigationParameter;
	private Runnable listener;
	private boolean visible = true;

	public Icon build()
	{
		Icon targetIcon = icon.create();
		targetIcon.setTooltipText(tooltipText);
		if(navigationTarget != null && navigationParameter != null)
			targetIcon.addClickListener(e -> UI.getCurrent().navigate(navigationTarget, navigationParameter));
		if(listener != null)
			targetIcon.addClickListener(e -> run(listener));
		if(enabled)
			targetIcon.setClassName(POINTER.getName());
		else
			targetIcon.setClassName(DISABLED_ICON.getName());
		targetIcon.getElement().setAttribute("onclick", "event.stopPropagation();");
		targetIcon.setVisible(visible);
		return targetIcon;
	}

	public ActionIconBuilder icon(VaadinIcon icon)
	{
		this.icon = icon;
		return this;
	}
	
	private void run(Runnable action)
	{
		if (enabled)
			action.run();
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

	public ActionIconBuilder disabled()
	{
		this.enabled = false;
		return this;
	}
	
	public ActionIconBuilder setEnable(boolean enable)
	{
		this.enabled = enable;
		return this;
	}

	public ActionIconBuilder setVisible(boolean visible)
	{
		this.visible = visible;
		return this;
	}
}
