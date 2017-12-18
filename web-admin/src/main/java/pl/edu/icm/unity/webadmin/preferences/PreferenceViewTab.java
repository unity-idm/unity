/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.preferences;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog.Callback;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor.ModificationListener;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandler;

/**
 * Management of a single preference.
 * @author K. Benedyczak
 */
public class PreferenceViewTab extends VerticalLayout
{
	private PreferencesHandler preferenceHandler;
	private PreferencesManagement prefMan;
	private UnityMessageSource msg;
	private EntityParam entityParam;
	
	private Label saveInfo;
	private Button save;
	private VerticalLayout viewerPanel;
	private PreferencesEditor editor;
	private String currentValue;
	
	public PreferenceViewTab(UnityMessageSource msg, PreferencesHandler preferenceHandler, 
			PreferencesManagement prefMan)
	{
		this.msg = msg;
		this.preferenceHandler = preferenceHandler;
		this.prefMan = prefMan;
		
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		entityParam = new EntityParam(entity.getEntityId());
		init();
	}
	
	private void reset()
	{
		try
		{
			prefMan.removePreference(entityParam, preferenceHandler.getPreferenceId());
			setSaveEnabled(false);
			refresh();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Preferences.errorReset"), e);
		}
	}
	
	private void update()
	{
		String value;
		try
		{
			value = editor.getValue();
		} catch (FormValidationException e)
		{
			NotificationPopup.showFormError(msg);
			return;
		}
		try
		{
			prefMan.setPreference(entityParam, preferenceHandler.getPreferenceId(), value);
			refresh();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Preferences.errorUpdate"), e);
		}
	}
	
	private void refresh() throws EngineException
	{
		currentValue = prefMan.getPreference(entityParam, preferenceHandler.getPreferenceId());
		editor = preferenceHandler.getPreferencesEditor(currentValue);
		viewerPanel.removeAllComponents();
		viewerPanel.addComponent(editor.getComponent());
		setSaveEnabled(false);
		editor.setChangeListener(new ModificationListener()
		{
			
			@Override
			public void preferencesModified()
			{
				setSaveEnabled(true);
			}
		});
	}
	
	private void setSaveEnabled(boolean how)
	{
		save.setEnabled(how);
		saveInfo.setVisible(how);
	}
	
	private void init()
	{
		setMargin(false);
		setSpacing(false);
		VerticalLayout topBar = new VerticalLayout();
		saveInfo = new Label(msg.getMessage("Preferences.saveNeeded"));
		saveInfo.setVisible(false);
		saveInfo.addStyleName(Styles.error.toString());
		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.setMargin(false);
		save = new Button(msg.getMessage("save"));
		save.setIcon(Images.save.getResource());
		save.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				update();
			}
		});
		Button reset = new Button(msg.getMessage("Preferences.reset"));
		reset.setIcon(Images.trashBin.getResource());

		reset.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				new ConfirmDialog(msg, msg.getMessage("Preferences.confirmReset", 
								preferenceHandler.getPreferenceLabel()),
						new Callback()
						{
							@Override
							public void onConfirm()
							{
								reset();
							}
						}).show();
			}
		});
		Button refresh = new Button(msg.getMessage("Preferences.refresh"));
		refresh.setIcon(Images.refresh.getResource());
		refresh.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				try
				{
					refresh();
				} catch (EngineException e)
				{
					NotificationPopup.showError(msg, msg.getMessage("Preferences.errorRefresh"), e);
				}
			}
		});
		
		
		toolbar.addComponents(save, reset, refresh);
		topBar.addComponents(saveInfo, toolbar);
		
		viewerPanel = new VerticalLayout();
		viewerPanel.setSpacing(false);
		viewerPanel.setMargin(new MarginInfo(false, false, true, true));
		setCaption(preferenceHandler.getPreferenceLabel());
		
		try
		{
			refresh();
			addComponents(topBar, viewerPanel);
		} catch (EngineException e)
		{
			ErrorComponent ec = new ErrorComponent();
			ec.setError(msg.getMessage("Preferences.errorGet"), e);
			addComponent(ec);
		}
	}
}
