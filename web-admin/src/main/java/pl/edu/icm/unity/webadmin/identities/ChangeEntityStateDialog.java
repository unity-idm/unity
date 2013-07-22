/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.EnumComboBox;

/**
 * Allows to select a new entity state
 * @author K. Benedyczak
 */
public class ChangeEntityStateDialog extends AbstractDialog
{
	private long entityId;
	protected Callback callback;
	
	private EnumComboBox<EntityState> entityState;
	private final EntityState initialState;
	
	public ChangeEntityStateDialog(UnityMessageSource msg, long entityId, EntityState currentState, Callback callback)
	{
		super(msg, msg.getMessage("ChangeEntityStateDialog.caption"));
		this.entityId = entityId;
		this.callback = callback;
		this.defaultSizeUndfined = true;
		this.initialState = currentState;
	}

	@Override
	protected FormLayout getContents()
	{
		Label info = new Label(msg.getMessage("ChangeEntityStateDialog.info", entityId));
		entityState = new EnumComboBox<EntityState>(msg.getMessage("ChangeEntityStateDialog.newState"), msg, 
				"EntityState.", EntityState.class, initialState);
		
		FormLayout main = new FormLayout();
		main.addComponents(info, entityState);
		main.setSizeFull();
		return main;
	}

	@Override
	protected void onConfirm()
	{
		if (callback.onChanged(entityState.getSelectedValue()))
			close();
	}
	
	public interface Callback 
	{
		public boolean onChanged(EntityState newState);
	}
}
