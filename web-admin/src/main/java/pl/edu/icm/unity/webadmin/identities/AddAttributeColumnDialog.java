/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * Allows to choose an attribute type to be added as identities table column.
 * @author K. Benedyczak
 */
public class AddAttributeColumnDialog extends AbstractDialog
{
	private AttributesManagement attrsMan;
	protected Callback callback;
	
	private ComboBox attributeType;
	private CheckBox useRootGroup;
	
	
	public AddAttributeColumnDialog(UnityMessageSource msg, AttributesManagement attrsMan, 
			Callback callback)
	{
		super(msg, msg.getMessage("AddAttributeColumnDialog.caption"));
		this.attrsMan = attrsMan;
		this.callback = callback;
		setSizeMode(SizeMode.SMALL);
	}

	@Override
	protected FormLayout getContents()
	{
		Label info = new Label(msg.getMessage("AddAttributeColumnDialog.info"));
		Label info2 = new Label(msg.getMessage("AddAttributeColumnDialog.info2"));
		Collection<AttributeType> attrTypes;
		try
		{
			attrTypes = attrsMan.getAttributeTypes();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("AddAttributeColumnDialog.cantGetAttrTypes"));
			throw new IllegalStateException();
		}
		List<AttributeType> filtered = new ArrayList<>(attrTypes.size());
		for (AttributeType at: attrTypes)
		{
			if (!at.isInstanceImmutable())
				filtered.add(at);
		}
		attributeType = new AttributeSelectionComboBox(msg.getMessage("AddAttributeColumnDialog.attribute"),
				filtered);
		attributeType.setNullSelectionAllowed(false);
		
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
		callback.onChosen((String)attributeType.getValue(), group);
		close();
	}
	
	public interface Callback 
	{
		public void onChosen(String attributeType, String group);
	}
}
