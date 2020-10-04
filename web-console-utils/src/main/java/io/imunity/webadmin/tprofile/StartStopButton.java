/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile;

import java.io.Serializable;
import java.lang.reflect.Method;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.util.ReflectTools;

import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * 
 * @author Roman Krysinski
 *
 */
public class StartStopButton extends Button 
{
	private boolean checked = false;
	
	public StartStopButton() 
	{
		super();
		setIcon();
		addStyleName(Styles.vButtonLink.toString());
		addStyleName(Styles.toolbarButton.toString());
		addClickListener(new ClickListener() 
		{
			@Override
			public void buttonClick(ClickEvent event) 
			{
				if (checked)
				{
					fireEvent(new ClickStopEvent(StartStopButton.this));
				} else
				{
					fireEvent(new ClickStartEvent(StartStopButton.this));
				}
				checked = !checked;
				setIcon();
			}
		});
	}
	
    private void setIcon() 
    {
    	if (checked)
    	{
    		setIcon(Images.pause.getResource());
    	} else
    	{
    		setIcon(Images.play.getResource());
    	}
	}
    
    public void addClickListener(StartStopListener listener) 
    {
    	addListener(ClickStartEvent.class, listener,
    			StartStopListener.BUTTON_START_METHOD);
    	addListener(ClickStopEvent.class, listener,
    			StartStopListener.BUTTON_STOP_METHOD);
    }	

	public interface StartStopListener extends Serializable {

        public static final Method BUTTON_START_METHOD = ReflectTools
                .findMethod(StartStopListener.class, "onStart", ClickStartEvent.class);
        public static final Method BUTTON_STOP_METHOD = ReflectTools
        		.findMethod(StartStopListener.class, "onStop", ClickStopEvent.class);

        public void onStart(ClickStartEvent event);

        public void onStop(ClickStopEvent event);
    }
    
    public class ClickStartEvent extends Component.Event
    {
		public ClickStartEvent(Component source) 
		{
			super(source);
		}
    }
    
    public class ClickStopEvent extends Component.Event
    {
		public ClickStopEvent(Component source) 
		{
			super(source);
		}
    }
}
