/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Slider;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;

/**
 * Allows for editing ETD settings
 * @author K. Benedyczak
 */
public class ETDSettingsEditor
{
	public static final long MS_PER_DAY = 1000*3600*24;
	private UnityMessageSource msg;
	
	private CheckBox generateETD;
	private Slider validityDays;
	
	public ETDSettingsEditor(UnityMessageSource msg, Layout parentLayout)
	{
		this.msg = msg;
		addToLayout(parentLayout);
	}

	private void addToLayout(Layout parentLayout)
	{
		final Label infoVal = new Label();
		generateETD = new CheckBox(msg.getMessage("SamlUnicoreIdPWebUI.generateETD"));
		generateETD.addValueChangeListener(event ->
		{
			boolean how = generateETD.getValue();
			validityDays.setEnabled(how);
			infoVal.setEnabled(how);
		});
		validityDays = new Slider(1, 90);
		validityDays.setSizeFull();
		validityDays.addValueChangeListener(event ->
		{
			int days = validityDays.getValue().intValue();
			infoVal.setValue(msg.getMessage("SamlUnicoreIdPWebUI.etdValidity", days));
		});
		infoVal.setValue(msg.getMessage("SamlUnicoreIdPWebUI.etdValidity", 
				String.valueOf(validityDays.getValue().intValue())));
		
		parentLayout.addComponents(generateETD, infoVal, validityDays);
		
		setDefaults();
	}
	
	private void setDefaults()
	{
		generateETD.setValue(true);
		validityDays.setValue(14d);
	}
	
	public SPETDSettings getSPETDSettings()
	{
		SPETDSettings ret = new SPETDSettings();
		ret.setGenerateETD(generateETD.getValue());
		ret.setEtdValidity((long)(double)validityDays.getValue()*MS_PER_DAY);
		return ret;
	}
	
	public void setValues(SPETDSettings initial)
	{
		if (initial.isGenerateETD())
		{
			generateETD.setValue(true);
			validityDays.setValue(initial.getEtdValidity()/(double)MS_PER_DAY);
		} else
		{
			generateETD.setValue(false);
			validityDays.setValue(14d);
		}
	}
}
