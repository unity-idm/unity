/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.menu;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.webui.common.SidebarStyles;

/**
 * Menu button
 * 
 * @author P.Piernik
 *
 */
public class MenuButton extends Button implements MenuElement
{

	private String toolTip;
	private String navigateTo;
	private String id;

	public static MenuButton get(String id)
	{
		return new MenuButton(id, "");
	}

	public MenuButton(String id, String caption)
	{
		build(id, caption, null, null);
	}

	private void build(String id, String caption, Resource icon, ClickListener clickListener)
	{

		this.id = id;
		withCaption(caption);
		withIcon(icon);
		if (clickListener != null)
		{
			withClickListener(clickListener);

		}
		setPrimaryStyleName(SidebarStyles.menuButton.toString());
	}

	public MenuButton withStyleName(String style)
	{
		addStyleName(style);
		return this;
	}

	public String getToolTip()
	{
		return toolTip;
	}

	public MenuButton setToolTip(String toolTip)
	{
		this.toolTip = toolTip;
		return this;
	}

	public MenuButton withCaption(String caption)
	{
		super.setCaption(caption);
		removeToolTip();
		updateToolTip();
		return this;
	}

	public MenuButton clickable()
	{
		withStyleName(SidebarStyles.menuButtonClickable.toString());
		return this;
	}

	public MenuButton withIcon(Resource icon)
	{
		super.setIcon(icon);
		return this;
	}

	public MenuButton withClickListener(ClickListener clickListener)
	{
		super.addClickListener(clickListener);
		return this;
	}

	public MenuButton withDescription(String description)
	{
		super.setDescription(description);
		return this;
	}

	public MenuButton withNavigateTo(String link)
	{
		navigateTo = link;
		return this.withClickListener(e -> {
			UI.getCurrent().getNavigator().navigateTo(link);
		});
	}

	private MenuButton updateToolTip()
	{
		String toolTip = "";
		String caption = getCaption();
		if (caption != null && !caption.isEmpty())
		{
			toolTip += caption;
		}
		if (this.toolTip != null && !this.toolTip.isEmpty())
		{
			toolTip += "<div class=\"u-toolTip\">" + this.toolTip + "</div>";
		}
		setCaption(toolTip);
		return this;
	}

	public MenuButton withToolTip(String toolTip)
	{
		setCaptionAsHtml(true);
		removeToolTip();
		if (toolTip == null || toolTip.isEmpty())
		{
			this.toolTip = null;
		} else
		{
			this.toolTip = toolTip;
		}
		updateToolTip();
		return this;
	}

	private MenuButton removeToolTip()
	{
		String caption = getCaption();
		if (toolTip != null && !toolTip.isEmpty() && caption != null && !caption.isEmpty())
		{
			setCaption(caption.replaceAll(
					"<div class=\"u-toolTip\">" + toolTip + "</div>", ""));
		}
		return this;
	}

	private boolean isActive()
	{
		return getStyleName().contains(SidebarStyles.menuButtonActive.toString());
	}

	@Override
	public void setActive(boolean active)
	{
		if (active != isActive())
		{
			if (active)
			{
				addStyleName(SidebarStyles.menuButtonActive.toString());
			} else
			{
				removeStyleName(SidebarStyles.menuButtonActive.toString());
			}
		}
	}

	public String getNavigateTo()
	{
		return navigateTo;
	}

	@Override
	public String getMenuElementId()
	{
		return id;
	}
}