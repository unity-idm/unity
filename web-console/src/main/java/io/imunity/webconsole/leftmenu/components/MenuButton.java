/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.leftmenu.components;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.navigator.View;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

import io.imunity.webconsole.layout.MenuComponent;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Menu button
 * 
 * @author P.Piernik
 *
 */
public class MenuButton extends Button implements MenuComponent<MenuButton>
{

	private String toolTip;
	private String navigateTo;

	private List<MenuComponent<?>> components;

	public static MenuButton get()
	{
		return new MenuButton("");
	}

	public MenuButton(String caption)
	{
		build(caption, null, null);
	}

	public MenuButton(Resource icon)
	{
		build(null, icon, null);
	}

	public MenuButton(String caption, Resource icon)
	{
		build(caption, icon, null);
	}

	public MenuButton(String caption, ClickListener clickListener)
	{
		build(caption, null, clickListener);
	}

	public MenuButton(Resource icon, ClickListener clickListener)
	{
		build(null, icon, clickListener);
	}

	public MenuButton(String caption, Resource icon, ClickListener clickListener)
	{
		build(caption, icon, clickListener);

	}

	private void build(String caption, Resource icon, ClickListener clickListener)
	{

		components = new ArrayList<MenuComponent<?>>();
		withCaption(caption);
		withIcon(icon);
		if (clickListener != null)
		{
			withClickListener(clickListener);

		}
		setPrimaryStyleName(Styles.menuButton.toString());
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
		withStyleName(Styles.menuButtonClickable.toString());
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

	public <T extends View> MenuButton withNavigateTo(Class<T> viewClass)
	{
		withNavigateTo(viewClass.getSimpleName());
		return this;
	}

	// public <T extends View> MenuButton withNavigateTo(String viewName,
	// Class<T> viewClass) {
	// navigateTo = viewName;
	//
	// Navigator navigator = UI.getCurrent().getNavigator();
	//
	// //navigator.removeView(viewName);
	// //navigator.addView(viewName, viewClass);
	//
	// return this.withClickListener(e -> {
	// navigator.navigateTo(navigateTo);
	// });
	// }

	public MenuButton updateToolTip()
	{
		String toolTip = "";
		String caption = getCaption();
		if (caption != null && !caption.isEmpty())
		{
			toolTip += caption;
		}
		if (this.toolTip != null && !this.toolTip.isEmpty())
		{
			toolTip += "<div class=\"toolTip\">" + this.toolTip + "</div>";
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

	public MenuButton removeToolTip()
	{
		String caption = getCaption();
		if (toolTip != null && !toolTip.isEmpty() && caption != null && !caption.isEmpty())
		{
			setCaption(caption.replaceAll(
					"<div class=\"toolTip\">" + toolTip + "</div>", ""));
		}
		return this;
	}

	public boolean isActive()
	{
		return getStyleName().contains(Styles.menuButtonActive.toString());
	}

	public MenuButton setActive(boolean active)
	{
		if (active != isActive())
		{
			if (active)
			{
				addStyleName(Styles.menuButtonActive.toString());
			} else
			{
				removeStyleName(Styles.menuButtonActive.toString());
			}
		}
		return this;
	}

	public String getNavigateTo()
	{
		return navigateTo;
	}

	@Override
	public String getRootStyle()
	{
		return Styles.menuButton.toString();
	}

	@Override
	public <C extends MenuComponent<?>> C add(C c)
	{
		components.add(c);
		return c;
	}

	@Override
	public <C extends MenuComponent<?>> C addAsFirst(C c)
	{
		components.add(0, c);
		return c;
	}

	@Override
	public <C extends MenuComponent<?>> C addAt(C c, int index)
	{
		components.add(index, c);
		return c;
	}

	@Override
	public int count()
	{
		return components.size();
	}

	@Override
	public <C extends MenuComponent<?>> MenuButton remove(C c)
	{
		components.remove(c);
		return this;
	}

	@Override
	public List<MenuComponent<?>> getList()
	{
		return components;
	}
}