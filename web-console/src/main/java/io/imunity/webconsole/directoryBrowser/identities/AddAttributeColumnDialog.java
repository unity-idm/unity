/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.Collection;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;

/**
 * Allows to choose an attribute type to be added as identities table column.
 * @author K. Benedyczak
 */
class AddAttributeColumnDialog extends AbstractDialog
{
	private AttributeTypeManagement attrsMan;
	protected Callback callback;
	
	private ComboBox<AttributeType> attributeType;
	private CheckBox useRootGroup;
	
	
	AddAttributeColumnDialog(MessageSource msg, AttributeTypeManagement attrsMan, 
			Callback callback)
	{
		super(msg, msg.getMessage("AddAttributeColumnDialog.caption"));
		this.attrsMan = attrsMan;
		this.callback = callback;
		setSizeEm(38, 24);
	}

	@Override
	protected FormLayout getContents()
	{
		Label info = new Label(msg.getMessage("AddAttributeColumnDialog.info"));
		info.setWidth(100, Unit.PERCENTAGE);
		Label info2 = new Label(msg.getMessage("AddAttributeColumnDialog.info2"));
		info2.setWidth(100, Unit.PERCENTAGE);
		Collection<AttributeType> attrTypes;
		try
		{
			attrTypes = attrsMan.getAttributeTypes();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("AddAttributeColumnDialog.cantGetAttrTypes"));
			throw new IllegalStateException();
		}
		attributeType = new AttributeSelectionComboBox(msg.getMessage("AddAttributeColumnDialog.attribute"),
				attrTypes, false);
		
		useRootGroup = new CheckBox(msg.getMessage("AddAttributeColumnDialog.useRootGroup"), true);
		useRootGroup.setDescription(msg.getMessage("AddAttributeColumnDialog.useRootGroupTooltip"));
		
		FormLayout main = new CompactFormLayout();
		main.addComponents(info, info2, attributeType, useRootGroup);
		main.setSizeFull();
		return main;
	}

	@Override
	protected void onConfirm()
	{
		String group = useRootGroup.getValue() ? "/" : null;
		callback.onChosen(attributeType.getValue().getName(), group);
		close();
	}
	
	interface Callback 
	{
		public void onChosen(String attributeType, String group);
	}
}
