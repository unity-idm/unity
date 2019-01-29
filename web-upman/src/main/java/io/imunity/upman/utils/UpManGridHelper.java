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

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.TextField;
import com.vaadin.ui.renderers.HtmlRenderer;

import io.imunity.upman.common.FilterableEntry;
import io.imunity.upman.common.UpManGrid;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
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
			menu.addStyleName(SidebarStyles.sidebar.toString());
			return menu;

		}).setCaption(caption).setWidth(80).setResizable(false);
	}

	public static <T> Column<T, String> createDateTimeColumn(Grid<T> grid, Function<T, Instant> timeProvider,
			String caption)
	{
		return grid.addColumn(
				t -> timeProvider.apply(t) != null ? TimeUtil.formatMediumInstant(timeProvider.apply(t))
						: "")
				.setCaption(caption).setExpandRatio(3);
	}

	public static <T> void createAttrsColumns(Grid<T> grid, Function<T, Map<String, String>> attributesProvider,
			Map<String, String> additionalAttributes)
	{
		for (Map.Entry<String, String> attribute : additionalAttributes.entrySet())
		{
			grid.addColumn(r -> attributesProvider.apply(r).get(attribute.getKey()))
					.setCaption(attribute.getValue()).setExpandRatio(3)
					.setId(ATTR_COL_PREFIX + attribute.getKey());
		}
	}

	public static <T> Column<T, String> createGroupsColumn(Grid<T> grid, Function<T, List<String>> groups,
			String caption)
	{
		return grid.addColumn(r -> {
			return (groups.apply(r) != null) ? String.join(", ", groups.apply(r)) : "";
		}).setCaption(caption).setExpandRatio(3);
	}

	public static TextField generateSearchField(UpManGrid<? extends FilterableEntry> grid, UnityMessageSource msg)
	{
		TextField searchText = new TextField();
		searchText.addStyleName(Styles.vSmall.toString());
		searchText.setWidth(10, Unit.EM);
		searchText.setPlaceholder(msg.getMessage("UpManGrid.search"));
		searchText.addValueChangeListener(event -> {
			String searched = event.getValue();
			grid.clearFilters();
			if (event.getValue() == null || event.getValue().isEmpty())
			{
				return;
			}
			grid.addFilter(e -> e.anyFieldContains(searched, msg));
		});
		return searchText;
	}

	public static <T> Column<T, String> createEmailColumn(Grid<T> grid,
			Function<T, VerifiableElementBase> emailProvider, String caption,
			ConfirmationInfoFormatter formatter)
	{

		Column<T, String> emailColumn = grid.addColumn(t -> emailProvider.apply(t) != null
				? ((emailProvider.apply(t).getConfirmationInfo().isConfirmed() ? Images.ok.getHtml()
						: Images.warn.getHtml()) + " " + emailProvider.apply(t).getValue())
				: "", new HtmlRenderer());
		emailColumn.setDescriptionGenerator(t -> emailProvider.apply(t) != null ? formatter
				.getSimpleConfirmationStatusString(emailProvider.apply(t).getConfirmationInfo()) : "");
		emailColumn.setCaption(caption);
		emailColumn.setExpandRatio(1);
		return emailColumn;
	}
}
