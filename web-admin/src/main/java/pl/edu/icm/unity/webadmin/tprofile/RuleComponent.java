package pl.edu.icm.unity.webadmin.tprofile;

import java.util.HashMap;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.RequiredTextField;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

public class RuleComponent extends FormLayout
{
	private UnityMessageSource msg;
	private TranslationActionsRegistry tc;
	private ComboBox actions;
	private AbstractTextField condition;
	private HashMap<String, AbstractTextField> params;
	private FormLayout paramsL;
	private Callback callback;
	
	
	public RuleComponent(UnityMessageSource msg, TranslationActionsRegistry tc, Callback callback)
	{
		this.callback = callback;
		this.msg = msg;
		this.tc = tc;
		params = new HashMap<String, AbstractTextField>();
		initUI();
	}

	private void initUI()
	{
		
		Button up = new Button("UP");
		up.addClickListener(new Button.ClickListener()
		{
			
			@Override
			public void buttonClick(ClickEvent event)
			{
				callback.moveUP(RuleComponent.this);
				
			}
		});
		
		paramsL = new FormLayout();
		condition = new RequiredTextField(msg);
		condition.setCaption("Condition");
		
		actions = new ComboBox("Action");
		for (TranslationActionFactory a:tc.getAll())
		{
			actions.addItem(a.getName());
		}
		actions.setImmediate(true);
		
		actions.addValueChangeListener(new ValueChangeListener()
		{
			
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				
				paramsL.removeAllComponents();
				params.clear();
				try
				{
					for(ActionParameterDesc ad:tc.getByName(actions.getValue().toString()).getParameters())
					{
						AbstractTextField p = new TextField(ad.getName());
						p.setDescription(ad.getDescription());				
						paramsL.addComponent(p);
						
					}
				} catch (IllegalTypeException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		addComponents(up, condition, actions, paramsL);	
	}
	
	
	
	public interface Callback
	{
		public boolean moveUP(RuleComponent rule);
	}
		
	
	
	
}
