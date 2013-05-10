/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Set;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;

/**
 * Allows to choose an attribute type to be removed from identities table columns.
 * @author K. Benedyczak
 */
public class RemoveAttributeColumnDialog extends AbstractDialog
{
	protected Callback callback;
	private Set<String> alreadyUsed;
	
	private ComboBox attributeType;
	
	
	public RemoveAttributeColumnDialog(UnityMessageSource msg, Set<String> alreadyUsed, Callback callback)
	{
		super(msg, msg.getMessage("RemoveAttributeColumnDialog.caption"));
		this.alreadyUsed = alreadyUsed;
		this.callback = callback;
		this.defaultSizeUndfined = true;
	}

	@Override
	protected FormLayout getContents()
	{
		Label info = new Label(msg.getMessage("RemoveAttributeColumnDialog.info"));
		attributeType = new ComboBox(msg.getMessage("RemoveAttributeColumnDialog.attribute"));
		for (String at: alreadyUsed)
		{
			attributeType.addItem(at);
		}
		if (alreadyUsed.size()>0)
			attributeType.select(alreadyUsed.iterator().next());
		attributeType.setNullSelectionAllowed(false);
		FormLayout main = new FormLayout();
		main.addComponents(info, attributeType);
		main.setSizeFull();
		return main;
	}

	@Override
	protected void onConfirm()
	{
		String selected = (String)attributeType.getValue();
		callback.onChosen(selected);
		close();
	}
	
	public interface Callback 
	{
		public void onChosen(String attributeType);
	}
}
