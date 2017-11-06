/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.Map;
import java.util.function.Consumer;

import com.vaadin.addon.contextmenu.ContextMenu;
import com.vaadin.addon.contextmenu.MenuItem;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.DragStartMode;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.engine.translation.TranslationCondition;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationRule;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Responsible for editing of a single TranslationRule
 * 
 * @author P. Piernik
 * @contributor Roman Krysinski
 * 
 */
public class RuleComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private TypesRegistryBase<? extends TranslationActionFactory<?>> tc;
	private ActionEditor actionEditor;
	private AbstractTextField condition;
	private MappingResultComponent mappingResultComponent;
	private Callback callback;
	private MenuItem top;
	private MenuItem bottom;
	private boolean editMode;
	private ActionParameterComponentProvider actionComponentProvider;
	private FormLayout content;
	private Label info;
	private Button showHide;
	
	public RuleComponent(UnityMessageSource msg, TypesRegistryBase<? extends TranslationActionFactory<?>> tc,
			TranslationRule toEdit, ActionParameterComponentProvider actionComponentProvider, 
			Callback callback)
	{
		this.callback = callback;
		this.msg = msg;
		this.tc = tc;
		this.actionComponentProvider = actionComponentProvider;
		editMode = toEdit != null;
		initUI(toEdit);
	}

	private void initUI(TranslationRule toEdit)
	{
		
		
		VerticalLayout headerWrapper = new VerticalLayout();
		
		HorizontalLayout header = new HorizontalLayout();
		header.setSizeFull();
		header.setMargin(false);
		
		
		showHide = new Button(Images.vaadinDownArrow.getResource());
		showHide.addStyleName(Styles.vButtonLink.toString());
		showHide.addStyleName(Styles.toolbarButton.toString());
		showHide.addClickListener(new ClickListener()
		{
			
			@Override
			public void buttonClick(ClickEvent event)
			{
				showHideContent(!content.isVisible());		
			}
		});
		header.addComponent(showHide);
		header.setExpandRatio(showHide, 0);
		header.setComponentAlignment(showHide, Alignment.MIDDLE_LEFT);
		
		info = new Label("");
		info.setSizeUndefined();;
				
		DragHtmlLabel img = new DragHtmlLabel(this, "&nbsp" + Images.vaadinResize.getHtml() + "&nbsp");
		img.addStyleName(Styles.link.toString());
		img.setSizeFull();
		
		DragAndDropWrapper dragWrapper = new DragAndDropWrapper(img);
		dragWrapper.setDragStartMode(DragStartMode.WRAPPER);
		dragWrapper.setSizeUndefined();
		
		header.addComponent(dragWrapper);	
		header.setComponentAlignment(dragWrapper, Alignment.MIDDLE_LEFT);
		header.setExpandRatio(dragWrapper, 0);
			
		header.addComponent(info);	
		header.setComponentAlignment(info, Alignment.MIDDLE_LEFT);
		header.setExpandRatio(info, 10);
	
		Button menu = new Button();
		menu.setIcon(Images.vaadinMenu.getResource());
		menu.addStyleName(Styles.vButtonLink.toString());
		menu.addStyleName(Styles.toolbarButton.toString());
		
		ContextMenu contextMenu = new ContextMenu(menu, false);
		contextMenu.setAsContextMenuOf(menu);
		MenuItem remove = contextMenu.addItem(msg.getMessage("TranslationProfileEditor.remove"), e -> {
				callback.remove(RuleComponent.this);
		        });
		remove.setCheckable(false);
		remove.setIcon(Images.vaadinRemove.getResource());	
		
		top = contextMenu.addItem(msg.getMessage("TranslationProfileEditor.moveTop"), e -> {
			callback.moveTop(RuleComponent.this);
	        });
	
		top.setCheckable(false);
		top.setIcon(Images.vaadinTopArrow.getResource());	
		
		bottom = contextMenu.addItem(msg.getMessage("TranslationProfileEditor.moveBottom"), e -> {
			callback.moveBottom(RuleComponent.this);
	        });
		bottom.setCheckable(false);
		bottom.setIcon(Images.vaadinBottomArrow.getResource());
		
		menu.addClickListener(new ClickListener()
		{
			
			@Override
			public void buttonClick(ClickEvent event)
			{
				contextMenu.open(event.getClientX(), event.getClientY());
				
			}
		});
		
	
		
		header.addComponent(menu);
		header.setComponentAlignment(menu, Alignment.MIDDLE_RIGHT);
		header.setExpandRatio(menu, 0);
				
		header.addLayoutClickListener(new LayoutClickListener()
		{	
			@Override
			public void layoutClick(LayoutClickEvent event)
			{
				if (!event.isDoubleClick())
					return;
				showHideContent(!content.isVisible());			
			}
		});
		
		Label separator = new Label();
		separator.addStyleName(Styles.horizontalLine.toString());
		headerWrapper.addComponent(header);
		headerWrapper.addComponent(separator);
			
		condition = new MVELExpressionField(msg, msg.getMessage("TranslationProfileEditor.ruleCondition"), 
				msg.getMessage("MVELExpressionField.conditionDesc"));
		condition.setStyleName(Styles.vTiny.toString());
		
		
		Consumer<String> editorCallback = (s) -> {
			info.setValue(s);
		};
		actionEditor = new ActionEditor(msg, tc, toEdit == null ? null : toEdit.getAction(),
				actionComponentProvider, editorCallback);
		
		mappingResultComponent = new MappingResultComponent(msg);	
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		content = new FormLayout();	
		content.addComponent(condition);
		actionEditor.addToLayout(content);
		content.addComponents(mappingResultComponent);
		content.setMargin(false);
		content.setSpacing(true);
		showHideContent(false);
		
		main.addComponent(headerWrapper);
		main.addComponent(content);
	
		setCompositionRoot(main);
		
		info.setValue(actionEditor.getStringRepresentation());
		if (editMode)
		{	
			condition.setValue(toEdit.getCondition());
		} else
		{
			condition.setValue("true");
		}	
	}

	public TranslationRule getRule() throws FormValidationException
	{
		TranslationAction action = actionEditor.getAction();
		TranslationCondition cnd = new TranslationCondition();
		cnd.setCondition(condition.getValue());			
		
		return new TranslationRule(condition.getValue(), action);
	}
	
	public void setTopVisible(boolean v)
	{
		top.setVisible(v);
	}

	public void setBottomVisible(boolean v)
	{
		bottom.setVisible(v);
	}
	
	public void setFocus()
	{
		showHideContent(true);
		condition.focus();
	}

	public boolean validateRule()
	{
		boolean ok = true;
		try
		{
			actionEditor.getAction();
		} catch (Exception e)
		{
			ok = false;
		}
		
		condition.setValidationVisible(true);
		if (!condition.isValid())
			ok = false;
		
		if (!ok)
			showHideContent(true);
		return ok;
	}

	public void test(RemotelyAuthenticatedInput remoteAuthnInput) 
	{
		setReadOnlyStyle(true);
		TranslationRule rule = null;
		try 
		{
			rule = getRule();
		} catch (Exception e)
		{
			actionEditor.indicateExpressionError(e);
			return;
		}
		
		Map<String, Object> mvelCtx = InputTranslationProfile.createMvelContext(remoteAuthnInput);
		TranslationCondition conditionRule = new TranslationCondition(rule.getCondition());
		try 
		{
			boolean result = conditionRule.evaluate(mvelCtx);
			setLayoutForEvaludatedCondition(result);
		} catch (EngineException e) 
		{
			indicateConditionError(e);
		}
		
		InputTranslationAction action = (InputTranslationAction) rule.getAction();
		try 
		{
			MappingResult mappingResult = action.invoke(remoteAuthnInput, mvelCtx, null);
			displayMappingResult(mappingResult);
		} catch (EngineException e) 
		{
			actionEditor.indicateExpressionError(e);
		}
	}
	
	private void setLayoutForEvaludatedCondition(boolean conditionResult) 
	{
		removeRuleComponentEvaluationStyle();
		if (conditionResult)
		{
			setColorForInputComponents(Styles.trueConditionBackground.toString());
		} else
		{
			setColorForInputComponents(Styles.falseConditionBackground.toString());
		}
	}
	
	private void displayMappingResult(MappingResult mappingResult) 
	{
		mappingResultComponent.displayMappingResult(mappingResult);
		mappingResultComponent.setVisible(true);
	}
	
	private void indicateConditionError(Exception e) 
	{
		condition.setStyleName(Styles.errorBackground.toString());
		condition.setComponentError(new UserError(NotificationPopup.getHumanMessage(e)));
		condition.setValidationVisible(true);
	}


	public void clearTestResult() 
	{
		removeRuleComponentEvaluationStyle();	
		hideMappingResultComponent();
		setReadOnlyStyle(false);
	}
	
	private void setColorForInputComponents(String style)
	{
		condition.setStyleName(style);
		actionEditor.setStyle(style);
	}
	
	private void removeRuleComponentEvaluationStyle()
	{
		condition.removeStyleName(Styles.trueConditionBackground.toString());
		condition.removeStyleName(Styles.errorBackground.toString());
		condition.removeStyleName(Styles.falseConditionBackground.toString());
		condition.setComponentError(null);
		condition.setValidationVisible(false);
		
		actionEditor.removeComponentEvaluationStyle();
	}
	
	private void hideMappingResultComponent() 
	{
		mappingResultComponent.setVisible(false);
	}
	
	private void setReadOnlyStyle(boolean readOnly)
	{
		condition.setReadOnly(readOnly);
		actionEditor.setReadOnlyStyle(readOnly);
	}
	
	private void showHideContent(boolean show)
	{
		showHide.setIcon(show ? Images.vaadinUpArrow.getResource()
				: Images.vaadinDownArrow.getResource());
		content.setVisible(show);
	}

	public static class DragHtmlLabel extends Label
	{
		RuleComponent parentRule;
		public DragHtmlLabel(RuleComponent parent, String value)
		{
			super(value, ContentMode.HTML);
			this.parentRule = parent;
		}
		
		public RuleComponent getParentRule()
		{
			return parentRule;
		}
	}	
	
	public interface Callback
	{
		public boolean remove(RuleComponent rule);
		public boolean moveTop(RuleComponent rule);
		public boolean moveBottom(RuleComponent rule);
	}
}
