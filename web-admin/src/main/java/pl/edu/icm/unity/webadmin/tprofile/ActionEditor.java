/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.TypesRegistryBase;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentFactory.Provider;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.LayoutEmbeddable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.RequiredComboBox;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

/**
 * Responsible for editing of a single {@link TranslationAction}
 * 
 */
public class ActionEditor<T extends TranslationAction> extends LayoutEmbeddable
{
	private UnityMessageSource msg;
	private TypesRegistryBase<? extends TranslationActionFactory<T>> tc;
	
	private ComboBox actions;
	private Label actionParams;
	private Provider actionComponentProvider;
	private List<ActionParameterComponent> paramComponents = new ArrayList<>();

	public ActionEditor(UnityMessageSource msg, TypesRegistryBase<? extends TranslationActionFactory<T>> tc,
			TranslationAction toEdit, ActionParameterComponentFactory.Provider actionComponentProvider)
	{
		this.msg = msg;
		this.tc = tc;
		this.actionComponentProvider = actionComponentProvider;
		initUI(toEdit);
	}

	private void initUI(TranslationAction toEdit)
	{
		actions = new RequiredComboBox(msg.getMessage("ActionEditor.ruleAction"), msg);
		for (TranslationActionFactory<T> a : tc.getAll())
			actions.addItem(a.getName());
		actions.setValidationVisible(false);
		actions.setNullSelectionAllowed(false);
		actions.addValueChangeListener(new ValueChangeListener()
		{

			@Override
			public void valueChange(ValueChangeEvent event)
			{
				String action = (String) actions.getValue();
				setParams(action, null);

			}
		});

		actionParams = new Label();
		actionParams.setCaption(msg.getMessage("ActionEditor.actionParameters"));

		addComponents(actions, actionParams);
		
		if (toEdit != null)
		{
			actions.setValue(toEdit.getActionDescription().getName());
			setParams(actions.getValue().toString(), toEdit.getParameters());
		} else
		{
			if (!actions.getItemIds().isEmpty())
			{
				Object firstItem = actions.getItemIds().iterator().next();
				actions.setValue(firstItem);
				setParams((String) actions.getValue(), null);
			}
		}
	}

	private void setParams(String action, String[] values)
	{
		removeComponents(paramComponents);
		paramComponents.clear();
		
		TranslationActionFactory<T> factory = getActionFactory(action);
		if (factory == null)
		{	
			return;
		}
		
		actions.setDescription(msg.getMessage(factory.getDescriptionKey()));
		ActionParameterDesc[] params = factory.getParameters();	
		for (int i = 0; i < params.length; i++)
		{
			ActionParameterComponent p = actionComponentProvider.getParameterComponent(params[i]);
			p.setValidationVisible(false);
			if (values != null && values.length > i)
			{
				p.setActionValue(values[i]);
			}		
			paramComponents.add(p);
			addComponent(p);
		}
		actionParams.setVisible(!paramComponents.isEmpty());
	}

	private String[] getActionParams()
	{
		ArrayList<String> params = new ArrayList<String>();
		for (ActionParameterComponent tc: paramComponents)
		{
			((AbstractComponent)tc).setComponentError(null);
			String val = tc.getActionValue();
			params.add(val);
		}
		String[] wrapper = new String[params.size()];
		return params.toArray(wrapper);
	}
	
	private TranslationActionFactory<T> getActionFactory(String action)
	{
		TranslationActionFactory<T> factory = null;
		try
		{
			factory = tc.getByName(action);

		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("ActionEditor.errorGetActions"), e);
		}
		return factory;
	}
	

	public T getAction() throws FormValidationException
	{
		String ac = (String) actions.getValue();
		TranslationActionFactory<T> factory = getActionFactory(ac);
		try
		{
			return factory.getInstance(getActionParams());
		} catch (Exception e)
		{
			String error = msg.getMessage("ActionEditor.parametersError", e.getMessage());
			UserError ue = new UserError(error);
			for (ActionParameterComponent tc: paramComponents)
				((AbstractComponent)tc).setComponentError(ue);
			throw new FormValidationException(error);
		}
	}
	
	public void setReadOnlyStyle(boolean readOnly)
	{
		actions.setReadOnly(readOnly);
		for (Component param: paramComponents)
			param.setReadOnly(readOnly);
	}
}
