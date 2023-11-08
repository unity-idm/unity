/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.console.views.directory_browser.ComboBoxNonEmptyValueSupport;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Collection;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.SMALL_MARGIN;


class AddFilterDialog extends ConfirmDialog
{
	enum Operand {equal, notEqual, contain, notContain}
	private final Callback callback;
	private final MessageSource msg;
	private final Collection<String> columns;

	private ComboBox<String> column;
	private ComboBox<Operand> operand;
	private TextField argument;
	
	AddFilterDialog(MessageSource msg, Collection<String> columns, Callback callback)
	{
		this.msg = msg;
		this.columns = columns;
		this.callback = callback;
		setHeader(msg.getMessage("AddFilterDialog.caption"));
		setCancelable(true);
		setConfirmButton(msg.getMessage("ok"), e -> onConfirm());
		setWidth("45em");
		setHeight("15em");
		add(getContents());
	}

	private Component getContents()
	{
		Span info = new Span(msg.getMessage("AddFilterDialog.column"));
		column = new ComboBox<>();
		ComboBoxNonEmptyValueSupport.install(column);
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

		operand = new ComboBox<>();
		ComboBoxNonEmptyValueSupport.install(operand);
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
			open();
			return;
		}
		
		String colId = column.getValue();
		String colCaption = column.getItemLabelGenerator().apply(colId);
		
		EntityFilter baseFilter = (op == Operand.notEqual || op == Operand.equal) ? 
			ie -> argumentV.equals(ie.getAnyValue(colId)) : 
			ie -> testForContain(ie, colId, argumentV.toLowerCase());
		EntityFilter filter = (op == Operand.notEqual || op == Operand.notContain) ?
			ie -> !baseFilter.test(ie) : baseFilter;
		
		String description = colCaption + " " + opLabel + " '" + argumentV + "'";
		
		callback.onConfirm(filter, description);
	}
	
	private boolean testForContain(IdentityEntry ie, String key, String searched)
	{
		String value = ie.getAnyValue(key);
		return value != null && value.toLowerCase().contains(searched);
	}
	
	interface Callback 
	{
		void onConfirm(EntityFilter filter, String description);
	}
}
