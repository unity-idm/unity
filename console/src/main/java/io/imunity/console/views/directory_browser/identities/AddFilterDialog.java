/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.NotEmptyComboBox;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeSearchService;

import java.util.Collection;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.SMALL_MARGIN;


class AddFilterDialog extends DialogWithActionFooter
{
	enum Operand {equal, notEqual, contain, notContain}
	private final Callback callback;
	private final MessageSource msg;
	private final Collection<String> columns;
	private final IdentitiesTreeGrid identitiesTable;
	private final AttributeSearchService attributeSearchService;

	private ComboBox<String> column;
	private ComboBox<Operand> operand;
	private TextField argument;

	AddFilterDialog(MessageSource msg, Collection<String> columns, IdentitiesTreeGrid identitiesTable,
			AttributeSearchService attributeSearchService, Callback callback)
	{
		super(msg::getMessage);
		this.msg = msg;
		this.columns = columns;
		this.identitiesTable = identitiesTable;
		this.attributeSearchService = attributeSearchService;
		this.callback = callback;
		setHeaderTitle(msg.getMessage("AddFilterDialog.caption"));
		setActionButton(msg.getMessage("ok"), this::onConfirm);
		setWidth("45em");
		setHeight("15em");
		add(getContents());
	}

	private Component getContents()
	{
		Span info = new Span(msg.getMessage("AddFilterDialog.column"));
		column = new NotEmptyComboBox<>();
		if (!columns.isEmpty())
		{
			column.setItems(
					columns.stream().filter(c -> !c.equals(IdentitiesGridColumnConstants.ACTION_COLUMN_ID))
							.collect(Collectors.toList()));
			column.setValue(columns.iterator().next());
		}
		column.setItemLabelGenerator(i -> {
			if (i.startsWith(IdentitiesGridColumnConstants.ATTR_COL_PREFIX))
				return i.substring(IdentitiesGridColumnConstants.ATTR_COL_PREFIX.length());
			else if (i.startsWith(IdentitiesGridColumnConstants.CRED_STATUS_COL_PREFIX))
				return i.substring(IdentitiesGridColumnConstants.CRED_STATUS_COL_PREFIX.length());
			else
				return msg.getMessage("Identities." + i);
		});

		operand = new NotEmptyComboBox<>();
		operand.setItems(Operand.values());
		operand.setItemLabelGenerator(item -> msg.getMessage("AddFilterDialog.operand." + item));
		operand.setValue(Operand.contain);
		argument = new TextField();
		
		HorizontalLayout filter = new HorizontalLayout();
		info.getStyle().set("margin-top", SMALL_MARGIN.value());
		filter.add(info, column, operand, argument);
		return filter;
	}

	private void onConfirm()
	{
		Operand op = operand.getValue();
		Operand opLabel = operand.getValue();
		String argumentV = argument.getValue();
		if (argumentV.isEmpty())
		{
			argument.setErrorMessage(msg.getMessage(
					"AddFilterDialog.argumentMustBePresent"));
			argument.setInvalid(true);
			return;
		}
		
		String colId = column.getValue();
		String colCaption = column.getItemLabelGenerator().apply(colId);

		EntityFilter filter = new ColumnFilterFactory(attributeSearchService, identitiesTable::resolveAttributeValue)
				.create(colId, op, argumentV, identitiesTable.getGroupPath());

		String description = colCaption + " " + msg.getMessage("AddFilterDialog.operand." + opLabel) + " '" + argumentV + "'";

		callback.onConfirm(filter, description);
		close();
	}

	interface Callback 
	{
		void onConfirm(EntityFilter filter, String description);
	}
}
