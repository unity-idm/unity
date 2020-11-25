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
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.renderers.HtmlRenderer;

import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

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
			return menu;

		}).setCaption(caption).setWidth(80).setResizable(false).setSortable(false);
	}

	public static <T> Column<T, String> createDateTimeColumn(GridWithActionColumn<T> grid, Function<T, Instant> timeProvider,
			String caption)
	{
		return grid.addSortableColumn(
				t -> timeProvider.apply(t) != null ? TimeUtil.formatStandardInstant(timeProvider.apply(t))
						: ""
				,caption, 3).setResizable(true);
	}

	public static <T> void createAttrsColumns(GridWithActionColumn<T> grid, Function<T, Map<String, String>> attributesProvider,
			Map<String, String> additionalAttributes)
	{
		for (Map.Entry<String, String> attribute : additionalAttributes.entrySet())
		{
			Column<T, String> column = grid.addSortableColumn(r -> attributesProvider.apply(r).get(attribute.getKey()), attribute.getValue(), 3);
			column.setId(ATTR_COL_PREFIX + attribute.getKey());
			column.setResizable(true);
		}
	}

	public static <T> Column<T, String> createGroupsColumn(GridWithActionColumn<T> grid, Function<T, List<String>> groups,
			String caption)
	{
		return grid.addSortableColumn(r -> {
			return (groups.apply(r) != null) ? String.join(", ", groups.apply(r)) : "";
		}, caption, 3).setResizable(true);
	}

	public static <T> Column<T, String> createEmailColumn(GridWithActionColumn<T> grid,
			Function<T, VerifiableElementBase> emailProvider, String caption,
			ConfirmationInfoFormatter formatter)
	{

		Column<T, String> emailColumn = grid.addSortableColumn(t -> emailProvider.apply(t) != null
				? ((emailProvider.apply(t).getConfirmationInfo().isConfirmed() ? Images.ok.getHtml()
						: Images.warn.getHtml()) + " " + emailProvider.apply(t).getValue())
				: "", caption, 1);
		emailColumn.setRenderer(new HtmlRenderer());
		emailColumn.setResizable(true);
		emailColumn.setDescriptionGenerator(t -> emailProvider.apply(t) != null ? formatter
				.getSimpleConfirmationStatusString(emailProvider.apply(t).getConfirmationInfo()) : "");
		return emailColumn;
	}
}
