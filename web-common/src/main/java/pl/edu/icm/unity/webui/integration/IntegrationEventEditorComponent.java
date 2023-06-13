/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Binder;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.integration.IntegrationEvent;
import pl.edu.icm.unity.engine.api.integration.IntegrationEvent.EventType;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventConfiguration;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventDefinition;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventGroup;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventRegistry;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventVariable;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Single integration event editor
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class IntegrationEventEditorComponent extends CustomComponent
{
	private MessageSource msg;
	private IntegrationEventRegistry intEventDefRegistry;
	private List<IntegrationEventGroup> trimmedIntegrationEventGroups;
	private Label title;
	private Button showHide;
	private VerticalLayout content;
	private Callback callback;
	private ComboBox<String> trigger;
	private Binder<IntegrationEventVaadinBean> binder;
	private TextField name;
	private IntegrationEventConfigurationEditorRegistry editorsRegistry;
	private IntegrationEventConfigurationEditor editor;
	private EnumComboBox<EventType> type;

	@Autowired
	public IntegrationEventEditorComponent(MessageSource msg, IntegrationEventRegistry intEventDefRegistry,
			IntegrationEventConfigurationEditorRegistry editorsRegistry)
	{
		this.msg = msg;
		this.intEventDefRegistry = intEventDefRegistry;
		this.editorsRegistry = editorsRegistry;
		this.trimmedIntegrationEventGroups = new ArrayList<>();
		this.binder = new Binder<>(IntegrationEventVaadinBean.class);
		initUI();
	}

	public IntegrationEventEditorComponent withEvent(IntegrationEvent event)
	{
		binder.setBean(new IntegrationEventVaadinBean(event));
		refreshEditor();
		refreshTitle();
		return this;
	}

	public IntegrationEventEditorComponent withCallback(Callback callback)
	{
		this.callback = callback;
		return this;
	}

	public IntegrationEventEditorComponent forWebhookGroup(List<IntegrationEventGroup> groups)
	{
		trimmedIntegrationEventGroups.addAll(groups);
		return this;
	}

	public IntegrationEvent getIntegrationEvent() throws FormValidationException
	{
		if (hasErrors())
			throw new FormValidationException();
		IntegrationEventVaadinBean bean = binder.getBean();
		return new IntegrationEvent(bean.getName(), bean.getTrigger(), bean.getType(), bean.getConfiguration());

	}

	public boolean hasErrors()
	{
		return binder.validate().hasErrors();
	}

	private void initUI()
	{
		VerticalLayout headerWrapper = new VerticalLayout();
		headerWrapper.setMargin(false);
		headerWrapper.setSpacing(false);

		HorizontalLayout header = new HorizontalLayout();
		header.setSizeFull();
		header.setMargin(false);

		showHide = new Button(Images.downArrow.getResource());
		showHide.addStyleName(Styles.vButtonLink.toString());
		showHide.addStyleName(Styles.toolbarButton.toString());
		showHide.addStyleName(Styles.vButtonBorderless.toString());
		showHide.addClickListener(event -> showContent(!content.isVisible()));
		header.addComponent(showHide);
		header.setComponentAlignment(showHide, Alignment.MIDDLE_LEFT);

		title = new Label("");
		title.setSizeFull();
		header.addComponent(title);
		header.setComponentAlignment(title, Alignment.MIDDLE_LEFT);
		header.setExpandRatio(title, 1);

		HamburgerMenu<String> menuBar = new HamburgerMenu<String>();
		menuBar.addItem(msg.getMessage("IntegrationEventComponent.remove"), Images.remove.getResource(),
				s -> callback.remove(IntegrationEventEditorComponent.this));
		menuBar.addItem(msg.getMessage("IntegrationEventComponent.test"), Images.dryrun.getResource(),
				s -> showTestResult());
		header.addComponent(menuBar);
		header.setComponentAlignment(menuBar, Alignment.MIDDLE_RIGHT);
		header.setExpandRatio(menuBar, 0);

		header.addLayoutClickListener(event -> {
			if (!event.isDoubleClick())
				return;
			showContent(!content.isVisible());
		});

		headerWrapper.addComponent(header);
		headerWrapper.addComponent(HtmlTag.horizontalLine());

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		content = new VerticalLayout();
		content.setMargin(false);
		content.setSpacing(true);
		showContent(false);
		FormLayout commonFieldLayout = new FormLayoutWithFixedCaptionWidth();
		commonFieldLayout.setMargin(false);
		content.addComponent(commonFieldLayout);

		name = new TextField();
		name.setCaption(msg.getMessage("IntegrationEventComponent.name"));
		binder.forField(name).asRequired().bind("name");
		commonFieldLayout.addComponent(name);

		Label typeInfo = new Label();
		typeInfo.setContentMode(ContentMode.HTML);
		typeInfo.setWidth(100, Unit.PERCENTAGE);

		trigger = new ComboBox<>();
		trigger.setCaption(msg.getMessage("IntegrationEventComponent.trigger"));
		commonFieldLayout.addComponent(trigger);
		trigger.setItems(intEventDefRegistry.getAll().stream()
				.filter(w -> trimmedIntegrationEventGroups.isEmpty() ? true
						: trimmedIntegrationEventGroups.contains(w.getGroup()))
				.map(w -> w.getName()).collect(Collectors.toList()));
		binder.forField(trigger).asRequired().bind("trigger");

		trigger.addValueChangeListener(e -> {
			if (e.getValue() == null || e.getValue().isEmpty())
				return;
			IntegrationEventDefinition def = intEventDefRegistry.getByName(trigger.getValue());
			typeInfo.setValue(msg.getMessage(def.getDescriptionKey()) + "<br>"
					+ msg.getMessage("IntegrationEventComponent.variables",
							String.join(", ", def.getVariables().keySet())));
			if (e.isUserOriginated())
			{
				binder.getBean().setConfiguration(null);
				editor.setTrigger(e.getValue());
			}
		});

		commonFieldLayout.addComponent(typeInfo);
		type = new EnumComboBox<IntegrationEvent.EventType>(msg, "IntegrationEventType.", EventType.class,
				EventType.WEBHOOK);
		type.setCaption(msg.getMessage("IntegrationEventComponent.type"));
		binder.forField(type).bind("type");
		type.addValueChangeListener(e -> {
			if (e.isUserOriginated())
				binder.getBean().setConfiguration(null);
			refreshEditor();
		});
		commonFieldLayout.addComponent(type);
		binder.addValueChangeListener(e -> refreshTitle());
		main.addComponent(headerWrapper);
		main.addComponent(content);
		refreshTitle();
		setCompositionRoot(main);
		showContent(false);
	}

	private void showTestResult()
	{
		if (editor.getValue() == null)
		{
			NotificationPopup.showError(msg, "", new FormValidationException());
			return;
		}

		new TestParamsDialog(msg, intEventDefRegistry.getByName(trigger.getValue()), p -> {

			Component c;
			try
			{
				c = editor.test(p);
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("IntegrationEventComponent.testError"),
						e);
				return;
			}
			new AbstractDialog(msg, msg.getMessage("IntegrationEventComponent.testResult"),
					msg.getMessage("close"))
			{
				@Override
				protected void onConfirm()
				{
					close();
				}

				@Override
				protected Component getContents() throws Exception
				{
					return c;
				}
			}.show();
		}).show();
	}

	private void refreshTitle()
	{
		title.setValue(String.join("|",
				Arrays.asList(name.getValue(), trigger.getValue(), type.getValue().toString()).stream()
						.filter(v -> v != null).collect(Collectors.toList())));
	}

	private void showContent(boolean show)
	{
		showHide.setIcon(show ? Images.upArrow.getResource() : Images.downArrow.getResource());
		content.setVisible(show);
	}

	public void refreshEditor()
	{
		if (editor != null)
		{
			content.removeComponent(editor);
			binder.removeBinding(editor);
		}

		editor = editorsRegistry.getByName(binder.getBean().getType().toString())
				.getEditor(binder.getBean().getTrigger());
		binder.forField(editor).asRequired().bind("configuration");
		content.addComponent(editor);
	}

	public void expand()
	{
		showContent(true);
	}

	public void focus()
	{
		name.focus();
	}

	public interface Callback
	{
		void remove(IntegrationEventEditorComponent eventComponent);
	}

	public static class IntegrationEventVaadinBean
	{
		private String name;
		private String trigger;
		private EventType type;
		private IntegrationEventConfiguration configuration;

		public IntegrationEventVaadinBean()
		{

		}

		public IntegrationEventVaadinBean(EventType type)
		{
			this.type = type;
		}

		public IntegrationEventVaadinBean(IntegrationEvent event)
		{
			this.name = event.name;
			this.trigger = event.trigger;
			this.setType(event.type);
			this.configuration = event.configuration;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getTrigger()
		{
			return trigger;
		}

		public void setTrigger(String trigger)
		{
			this.trigger = trigger;
		}

		public EventType getType()
		{
			return type;
		}

		public void setType(EventType type)
		{
			this.type = type;
		}

		public IntegrationEventConfiguration getConfiguration()
		{
			return configuration;
		}

		public void setConfiguration(IntegrationEventConfiguration configuration)
		{
			this.configuration = configuration;
		}
	}

	private static class TestParamsDialog extends AbstractDialog
	{
		private IntegrationEventDefinition event;
		private Consumer<Map<String, String>> onConfirm;
		private Map<String, TextField> params;

		public TestParamsDialog(MessageSource msg, IntegrationEventDefinition event,
				Consumer<Map<String, String>> onConfirm)
		{
			super(msg, msg.getMessage("IntegrationEventComponent.specifyParameters"),
					msg.getMessage("IntegrationEventComponent.test"), msg.getMessage("cancel"));
			this.event = event;
			this.onConfirm = onConfirm;
			this.params = new HashMap<>();
		}

		@Override
		protected Component getContents() throws Exception
		{
			FormLayoutWithFixedCaptionWidth main = FormLayoutWithFixedCaptionWidth.withShortCaptions();

			for (IntegrationEventVariable var : event.getVariables().values())
			{
				TextField value = new TextField(var.name + ":");
				value.setDescription(msg.getMessage(var.descriptionKey));
				params.put(var.name, value);
				main.addComponent(value);
			}
			return main;
		}

		@Override
		protected void onConfirm()
		{
			close();
			onConfirm.accept(params.entrySet().stream().filter(f -> !f.getValue().getValue().isEmpty())
					.collect(Collectors.toMap(f -> f.getKey(), f -> f.getValue().getValue())));

		}

	}

}
