/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import java.util.Collection;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.attrstmnt.EverybodyStatement;
import pl.edu.icm.unity.webadmin.attrstmt.AttributeStatementWebHandlerFactory.AttributeStatementComponent;
import pl.edu.icm.unity.webadmin.attrstmt.StatementHandlersRegistry;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.MapComboBox;
import pl.edu.icm.unity.webui.common.SafePanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

/**
 * Allows to create/edit/view an attribute statement of a group.
 * @author K. Benedyczak
 */
public class AttributeStatementEditDialog extends AbstractDialog
{
	private AttributeStatement statement;
	private AttributesManagement attrsMan;
	private StatementHandlersRegistry statementHandlersRegistry;
	private final Callback callback;
	private final String group;
	
	private MapComboBox<AttributeStatementComponent> statementComponents;
	private SafePanel statementPanel;
	private EnumComboBox<AttributeStatement.ConflictResolution> conflictResolution;
	
	/**
	 * @param msg
	 * @param attributeStatement attribute statement to be displayed initially or 
	 * null when a new statement should be created
	 */
	public AttributeStatementEditDialog(UnityMessageSource msg, AttributeStatement attributeStatement,
			AttributesManagement attrsMan, StatementHandlersRegistry statementHandlersRegistry,
			String group, Callback callback)
	{
		super(msg, msg.getMessage("AttributeStatementEditDialog.caption"));
		this.statement = attributeStatement;
		this.statementHandlersRegistry = statementHandlersRegistry;
		this.attrsMan = attrsMan;
		this.group = group;
		this.callback = callback;
		setWidth(50, Unit.PERCENTAGE);
	}

	@Override
	protected Component getContents() throws Exception
	{
		Collection<AttributeType> attributeTypes;
		try
		{
			attributeTypes = attrsMan.getAttributeTypes();
		} catch(Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("AttributeStatementEditDialog.cantReadAttributeTypes"), e);
			throw e;
		}

		statementComponents = new MapComboBox<AttributeStatementComponent>(
				msg.getMessage("AttributeStatementEditDialog.statement"),
				statementHandlersRegistry.getAvailableComponents(attributeTypes, group), 
				msg.getMessage("AttributeStatements.stmt."+EverybodyStatement.NAME));
		statementComponents.setImmediate(true);
		statementComponents.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				statementChanged();
			}
		});
		
		statementPanel = new SafePanel();
		conflictResolution = new EnumComboBox<AttributeStatement.ConflictResolution>(
				msg.getMessage("AttributeStatementEditDialog.conflictResolution"), msg, 
				"AttributeStatement.conflictResolution.", AttributeStatement.ConflictResolution.class, 
				AttributeStatement.ConflictResolution.skip);
				
		FormLayout ret = new FormLayout();
		ret.addComponents(statementComponents, statementPanel, conflictResolution);
		
		statementChanged();
		if (statement != null)
			setInitialData(statement);
		ret.setWidth(100, Unit.PERCENTAGE);
		return ret;
	}

	private void setInitialData(AttributeStatement attributeStatement)
	{
		conflictResolution.setEnumValue(statement.getConflictResolution());
		statementComponents.setValue(msg.getMessage("AttributeStatements.stmt."+attributeStatement.getName()));
		statementComponents.getSelectedValue().setInitialData(attributeStatement);
	}

	
	private void statementChanged()
	{
		statementPanel.setContent(statementComponents.getSelectedValue().getComponent());
	}
	
	@Override
	protected void onConfirm()
	{
		AttributeStatementComponent component = statementComponents.getSelectedValue();
		AttributeStatement ret;
		try
		{
			ret = component.getStatementFromComponent();
		} catch (FormValidationException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("AttributeStatementEditDialog.invalidFormSettings"), 
					e.getMessage());
			return;
		}
		
		ret.setConflictResolution(conflictResolution.getSelectedValue());
		callback.onConfirm(ret);
		close();
	}

	
	public interface Callback
	{
		public void onConfirm(AttributeStatement newStatement);
	}

}
