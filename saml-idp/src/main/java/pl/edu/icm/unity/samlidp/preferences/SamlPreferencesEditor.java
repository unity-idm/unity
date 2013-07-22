/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.preferences;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.samlidp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor;

/**
 * Viewing and editing UI of {@link SamlPreferences}.
 * @author K. Benedyczak
 */
public class SamlPreferencesEditor implements PreferencesEditor
{
	private UnityMessageSource msg;
	private SamlPreferences preferences;
	
	private HorizontalLayout main;
	private GenericElementsTable<String> table;
	private SamlSPSettingsViewer viewer;

	public SamlPreferencesEditor(UnityMessageSource msg, SamlPreferences preferences)
	{
		this.msg = msg;
		this.preferences = preferences;
		init();
	}
	
	private void init()
	{
		main = new HorizontalLayout();
		
		table = new GenericElementsTable<String>(msg.getMessage("SAMLPreferences.spSettings"), 
				String.class, new GenericElementsTable.NameProvider<String>()
				{
					@Override
					public String toString(String element)
					{
						return element;
					}
				});
		table.setWidth(90, Unit.PERCENTAGE);
		main.addComponent(table);
		viewer = new SamlSPSettingsViewer(msg);
		main.addComponent(viewer);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				@SuppressWarnings("unchecked")
				GenericItem<String> item = (GenericItem<String>)table.getValue();
				if (item != null)
				{
					SPSettings sp = preferences.getSPSettings(item.getElement());
					viewer.setInput(sp);
				} else
					viewer.setInput(null);
			}
		});
//		table.addActionHandler(new AddActionHandler());
//		table.addActionHandler(new EditActionHandler());
//		table.addActionHandler(new DeleteActionHandler());
		main.setSizeFull();
		main.setSpacing(true);

		table.setInput(preferences.getKeys());
	}

	@Override
	public Component getComponent()
	{
		return main;
	}

	@Override
	public String getValue() throws FormValidationException
	{
		// TODO Auto-generated method stub
		return preferences.getSerializedConfiguration();
	}
}
