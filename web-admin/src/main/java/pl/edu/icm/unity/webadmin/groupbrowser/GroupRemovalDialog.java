/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;

/**
 * Group removal dialog allowing to choose recursive delete.
 * @author K. Benedyczak
 */
public class GroupRemovalDialog extends AbstractDialog
{
	private static final long serialVersionUID = 1L;
	private String groupPath;
	private Callback callback;
	private CheckBox recursive;
	
	public GroupRemovalDialog(UnityMessageSource msg, String groupPath, Callback callback)
	{
		super(msg, msg.getMessage("GroupRemovalDialog.caption"));
		this.groupPath = groupPath;
		this.callback = callback;
	}

	@Override
	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		vl.addComponent(new Label(msg.getMessage("GroupRemovalDialog.confirmDelete", groupPath)));
		recursive = new CheckBox(msg.getMessage("GroupRemovalDialog.recursive"));
		vl.addComponent(recursive);
		return vl;
	}

	@Override
	protected void onConfirm()
	{
		close();
		callback.onGroupRemove(recursive.getValue());
	}
	
	public interface Callback 
	{
		public void onGroupRemove(boolean recursive);
	}
}
