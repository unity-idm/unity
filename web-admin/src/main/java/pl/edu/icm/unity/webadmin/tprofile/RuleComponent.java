package pl.edu.icm.unity.webadmin.tprofile;

import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationCondition;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationRule;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.RequiredTextField;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.AbstractStringValidator;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.Reindeer;

public class RuleComponent extends FormLayout
{
	private UnityMessageSource msg;
	private TranslationActionsRegistry tc;
	private ComboBox actions;
	private AbstractTextField condition;
	private FormLayout paramsL;
	private Callback callback;
	private Button up;
	private Button down;
	private boolean editMode;
	
	
	public RuleComponent(UnityMessageSource msg, TranslationActionsRegistry tc,  TranslationRule toEdit, Callback callback)
	{
		this.callback = callback;
		this.msg = msg;
		this.tc = tc;
		editMode = toEdit != null;
		
		initUI(toEdit);
	}

	private void initUI(TranslationRule toEdit)
	{
		
		up = new Button();
		up.setDescription(msg.getMessage("TranslationProfileEditor.moveUp"));
		up.setIcon(Images.upArrow.getResource());
		up.addStyleName(Reindeer.BUTTON_SMALL);
		up.addClickListener(new Button.ClickListener()	
		{
			
			@Override
			public void buttonClick(ClickEvent event)
			{
				callback.moveUp(RuleComponent.this);
				
			}
		});
		
		down = new Button();
		down.setDescription(msg.getMessage("TranslationProfileEditor.moveDown"));
		down.setIcon(Images.downArrow.getResource());
		down.addStyleName(Reindeer.BUTTON_SMALL);
		down.addClickListener(new Button.ClickListener()
		{
			
			@Override
			public void buttonClick(ClickEvent event)
			{
				callback.moveDown(RuleComponent.this);
				
			}
		});
		
		Button remove = new Button();
		remove.setDescription(msg.getMessage("TranslationProfileEditor.remove"));
		remove.setIcon(Images.delete.getResource());
		remove.addStyleName(Reindeer.BUTTON_SMALL);
		remove.addClickListener(new Button.ClickListener()
		{
			
			@Override
			public void buttonClick(ClickEvent event)
			{
				callback.remove(RuleComponent.this);
				
			}
		});
		
		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.setSpacing(false);
		toolbar.setMargin(false);
		toolbar.addComponents(up, down, remove);
		
		paramsL = new FormLayout();
		paramsL.setMargin(false);
		paramsL.setSpacing(false);
		condition = new RequiredTextField(msg);
		condition.setCaption(msg.getMessage("TranslationProfileEditor.ruleCondition") + ":");
		condition.addValidator(new AbstractStringValidator(msg.getMessage("TranslationProfileEditor.conditionValidationFalse"))
		{
			
			@Override
			protected boolean isValidValue(String value)
			{
				try
				{
					MVEL.compileExpression(value);
				} catch (Exception e)
				{
					return false;
				}

				return true;
				
			}
		});
	
		condition.addValueChangeListener(new ValueChangeListener()
		{
			
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				try
		        	{
		        		condition.validate();
		        	}catch (Exception e)
		        	{
		        		
		        	}
				
			}
		});
		condition.setImmediate(true);
		
		actions = new ComboBox(msg.getMessage("TranslationProfileEditor.ruleAction") + ":");
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
				Object action = actions.getValue();
				if (action != null)
				{
					setParams(actions.getValue().toString(), null);
				}else
				{
					setParams(null, null);
				}
				
			}
		});
		
		if (editMode)
		{	
			condition.setValue(toEdit.getCondition().getCondition());
			actions.setValue(toEdit.getAction().getName());		
			setParams(actions.getValue().toString(), toEdit.getAction().getParameters());
		}
		
		
		
		
		
		
		addComponents(toolbar, condition, actions, paramsL);	
	}
	
	private void setParams(String action,String[] values)
	{
		paramsL.removeAllComponents();
		
		if (action == null)
		{
			return;
		}
		try
		{
			ActionParameterDesc[] params = tc.getByName(action).getParameters();
			for (int i = 0; i < params.length; i++)
			{
				AbstractTextField p = new TextField(params[i].getName() + ":");
				p.setDescription(params[i].getDescription());
				p.setId(params[i].getName());
				if (values != null && values[i] != null)
				{
					p.setValue(values[i]);
				}

				paramsL.addComponent(p);

			}
		} catch (IllegalTypeException e)
		{
			ErrorPopup.showError(msg,
					msg.getMessage("TranslationProfileEditor.errorGetActions"),
					e);
		}
	}
	
	
	public TranslationRule getRule()
	{
		TranslationActionFactory f = null;
		TranslationAction action = null;
		try
		{

			f = tc.getByName(actions.getValue().toString());
			String[] params = new String[paramsL.getComponentCount()];
			for (int i = 0; i < paramsL.getComponentCount(); i++)
			{
				AbstractTextField tc = (AbstractTextField) paramsL.getComponent(i);
				params[i] = tc.getValue().toString();
			}
			action = f.getInstance(params);

		} catch (EngineException e)
		{
			ErrorPopup.showError(msg,
					msg.getMessage("TranslationProfileEditor.errorGetActions"),
					e);
		}
		TranslationCondition cnd = new TranslationCondition();
		cnd.setCondition(condition.getValue());
		TranslationRule rule = new TranslationRule(action, cnd);

		return rule;

	}
        
        
        public void setUpVisible(boolean v)
        {
        	up.setVisible(v);
        }
        
        public void setDownVisible(boolean v)
        {
        	down.setVisible(v);
        }
        
        public boolean validateCondition()
        {
        	try
        	{
        		condition.validate();
        	}catch (Exception e)
        	{
        		return false;
        	}
		return true;
        }
	
	public interface Callback
	{
		public boolean moveUp(RuleComponent rule);
		public boolean moveDown(RuleComponent rule);
		public boolean remove(RuleComponent rule);
	}
		
	
	
	
}
