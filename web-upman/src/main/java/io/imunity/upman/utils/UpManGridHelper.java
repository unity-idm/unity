/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.utils;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.vaadin.ui.Grid;

import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * A collection of methods useful for creating UpMan grids
 * 
 * @author P.Piernik
 *
 */
public class UpManGridHelper
{
	public static final String ATTR_COL_PREFIX = "a::";

	public static <T> void createActionColumn(Grid<T> grid, List<SingleActionHandler<T>> rowActionHandlers,
			String caption)
	{
		grid.addComponentColumn(t -> {
			HamburgerMenu<T> menu = new HamburgerMenu<T>();
			HashSet<T> target = new HashSet<>();
			target.add(t);
			menu.setTarget(target);
			menu.addActionHandlers(rowActionHandlers);
			menu.addStyleName(SidebarStyles.sidebar.toString());
			return menu;

		}).setCaption(caption).setWidth(80).setResizable(false);
	}

	public static <T> void createDateTimeColumn(Grid<T> grid, Function<T, Instant> timeProvider, String caption)
	{
		grid.addColumn(t -> timeProvider.apply(t) != null ? TimeUtil.formatMediumInstant(timeProvider.apply(t)) : "")
				.setCaption(caption).setExpandRatio(3);
	}

	public static <T> void createAttrsColumns(Grid<T> grid, Function<T, Map<String, String>> attributesProvider,
			Map<String, String> additionalAttributes)
	{
		for (Map.Entry<String, String> attribute : additionalAttributes.entrySet())
		{
			grid.addColumn(r -> attributesProvider.apply(r).get(attribute.getKey())).setCaption(attribute.getValue())
					.setExpandRatio(3).setId(ATTR_COL_PREFIX + attribute.getKey());
		}
	}

	public static <T> void createGroupsColumn(Grid<T> grid, Function<T, List<String>> groups, String caption)
	{
		grid.addColumn(r -> {
			return (groups.apply(r) != null) ? String.join(", ", groups.apply(r)) : "";
		}).setCaption(caption).setExpandRatio(3);
	}
}
