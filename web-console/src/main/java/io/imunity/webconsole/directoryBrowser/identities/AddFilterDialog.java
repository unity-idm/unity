/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.Collection;
import java.util.stream.Collectors;

import com.vaadin.server.UserError;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.EnumComboBox;

/**
 * Allows to create a filter for the identities table.
 * @author K. Benedyczak
 */
class AddFilterDialog extends AbstractDialog
{
	enum Operand {equal, notEqual, contain, notContain};
	protected Callback callback;
	private Collection<String> columns;
	
	private ComboBox<String> column;
	private EnumComboBox<Operand> operand;
	private TextField argument;
	
	AddFilterDialog(MessageSource msg, Collection<String> columns, Callback callback)
	{
		super(msg, msg.getMessage("AddFilterDialog.caption"));
		this.columns = columns;
		this.callback = callback;
		setSizeEm(60, 12);
	}

	@Override
	protected Component getContents()
	{
		Label info = new Label(msg.getMessage("AddFilterDialog.column"));
		column = new ComboBox<>();
		column.setItems(columns.stream().filter(c -> !c.equals(IdentitiesGridColumnConstans.ACTION_COLUMN_ID)).collect(Collectors.toList()));
		column.setEmptySelectionAllowed(false);
		column.setSelectedItem(columns.iterator().next());
		column.setItemCaptionGenerator(i -> {
			if (i.startsWith(IdentitiesGridColumnConstans.ATTR_COL_PREFIX))
				return i.substring(IdentitiesGridColumnConstans.ATTR_COL_PREFIX.length());
			else if (i.startsWith(IdentitiesGridColumnConstans.CRED_STATUS_COL_PREFIX))
				return i.substring(IdentitiesGridColumnConstans.CRED_STATUS_COL_PREFIX.length());
			else
				return msg.getMessage("Identities." + i);
		});

		operand = new EnumComboBox<>(msg, "AddFilterDialog.operand.", 
				Operand.class, Operand.contain);
		
		argument = new TextField();
		
		HorizontalLayout filter = new HorizontalLayout();
		filter.setMargin(false);
		filter.addComponents(info, column, operand, argument);
		return filter;
	}

	@Override
	protected void onConfirm()
	{
		Operand op = operand.getValue();
		String opLabel = operand.getSelectedLabel();
		String argumentV = argument.getValue();
		if (argumentV.equals(""))
		{
			argument.setComponentError(new UserError(msg.getMessage(
					"AddFilterDialog.argumentMustBePresent")));
			return;
		} else
			argument.setComponentError(null);
		
		String colId = column.getValue();
		String colCaption = column.getItemCaptionGenerator().apply(colId);
		
		EntityFilter baseFilter = (op == Operand.notEqual || op == Operand.equal) ? 
			ie -> argumentV.equals(ie.getAnyValue(colId)) : 
			ie -> testForContain(ie, colId, argumentV.toLowerCase());
		EntityFilter filter = (op == Operand.notEqual || op == Operand.notContain) ?
			ie -> !baseFilter.test(ie) : baseFilter;
		
		String description = colCaption + " " + opLabel + " '" + argumentV + "'";
		
		callback.onConfirm(filter, description);
		close();
	}
	
	private boolean testForContain(IdentityEntry ie, String key, String searched)
	{
		String value = ie.getAnyValue(key);
		return value != null && value.toLowerCase().contains(searched);
	}
	
	interface Callback 
	{
		public void onConfirm(EntityFilter filter, String description);
	}
}
