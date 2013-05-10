/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.filter.Not;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.MapComboBox;

/**
 * Allows to create a filter for the identities table.
 * @author K. Benedyczak
 */
public class AddFilterDialog extends AbstractDialog
{
	enum Operand {equal, notEqual, contain, notContain};
	protected Callback callback;
	private Collection<?> columns;
	
	private MapComboBox<String> column;
	private EnumComboBox<Operand> operand;
	private TextField argument;
	
	public AddFilterDialog(UnityMessageSource msg, Collection<?> columns, Callback callback)
	{
		super(msg, msg.getMessage("AddFilterDialog.caption"));
		this.columns = columns;
		this.callback = callback;
		this.defaultSizeUndfined = true;
	}

	@Override
	protected Component getContents()
	{
		Label info = new Label(msg.getMessage("AddFilterDialog.column"));
		Map<String, String> colsMap = new HashMap<String, String>();
		for (Object colIdRaw: columns)
		{
			String colId = (String) colIdRaw;
			if (colId.startsWith(IdentitiesTable.ATTR_COL_PREFIX))
				colsMap.put(colId.substring(IdentitiesTable.ATTR_COL_PREFIX.length()), colId);
			else
				colsMap.put(msg.getMessage("Identities."+colId), colId);
		}
		column = new MapComboBox<String>(colsMap, colsMap.keySet().iterator().next());
		
		operand = new EnumComboBox<AddFilterDialog.Operand>(msg, "AddFilterDialog.operand.", 
				Operand.class, Operand.contain);
		
		argument = new TextField();
		
		HorizontalLayout filter = new HorizontalLayout();
		filter.setSpacing(true);
		filter.addComponents(info, column, operand, argument);
		return filter;
	}

	@Override
	protected void onConfirm()
	{
		Operand op = operand.getSelectedValue();
		String opLabel = operand.getSelectedLabel();
		String argumentV = argument.getValue();
		if (argumentV.equals(""))
		{
			argument.setComponentError(new UserError(msg.getMessage(
					"AddFilterDialog.argumentMustBePresent")));
			return;
		} else
			argument.setComponentError(null);
		
		String propertyId = column.getSelectedValue();
		String propertyLabel = column.getSelectedLabel();
		
		Filter filter;
		if (op == Operand.notEqual || op == Operand.equal)
			filter = new Compare.Equal(propertyId, argumentV); 
		else
			filter = new SimpleStringFilter(propertyId, argumentV, true, false);
		if (op == Operand.notEqual || op == Operand.notContain)
			filter = new Not(filter);
		
		String description = propertyLabel + " " + opLabel + " '" + argumentV + "'";
		
		callback.onConfirm(filter, description);
		close();
	}
	
	public interface Callback 
	{
		public void onConfirm(Filter filter, String description);
	}
}
