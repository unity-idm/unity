/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.msgTemplates;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.settings.SettingsNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.base.msgtemplates.MessageType;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SearchField;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all message templates
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class MessageTemplatesView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "MessageTemplates";

	private MessageSource msg;
	private MessageTemplateController controller;
	private SimpleMessageTemplateViewer viewer;
	private GridWithActionColumn<MessageTemplateEntry> messageTemplateGrid;

	@Autowired
	MessageTemplatesView(MessageSource msg, MessageTemplateController controller)
	{
		this.msg = msg;
		this.controller = controller;

	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		HorizontalLayout buttonsBar = StandardButtonsHelper
				.buildTopButtonsBar(StandardButtonsHelper.build4AddAction(msg,
						e -> NavigationHelper.goToView(NewMessageTemplateView.VIEW_NAME)));

		messageTemplateGrid = new GridWithActionColumn<>(msg, getRowActionsHandlers(), false, false);
		messageTemplateGrid
				.addComponentColumn(
						m -> StandardButtonsHelper.buildLinkButton(m.messageTemplate.getName(),
								e -> gotoEdit(m)),
						msg.getMessage("MessageTemplatesView.nameCaption"), 10)
				.setSortable(true).setComparator((m1, m2) -> {
					return m1.messageTemplate.getName().compareTo(m2.messageTemplate.getName());
				}).setId("name");

		messageTemplateGrid.addSortableColumn(m -> m.messageTemplate.getNotificationChannel(),
				msg.getMessage("MessageTemplatesView.channelCaption"), 10);
		messageTemplateGrid.addSortableColumn(m -> m.messageTemplate.getType().toString(),
				msg.getMessage("MessageTemplatesView.messageTypeCaption"), 10);
		messageTemplateGrid.addSortableColumn(m -> m.messageTemplate.getConsumer(),
				msg.getMessage("MessageTemplatesView.purposeCaption"), 10);

		messageTemplateGrid.addHamburgerActions(getRowHamburgerHandlers());
		messageTemplateGrid.setMultiSelect(true);
		messageTemplateGrid.setSizeFull();

		viewer = controller.getViewer();

		messageTemplateGrid.addSelectionListener(e -> {
			try
			{
				viewer.setInput(e.getAllSelectedItems().size() != 1 ? null
						: controller.getPreprocedMessageTemplate(
								e.getFirstSelectedItem().get().messageTemplate));
			} catch (ControllerException ex)
			{
				NotificationPopup.showError(msg, ex);
			}
		});

		HamburgerMenu<MessageTemplateEntry> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addActionHandlers(getGlobalHamburgerHandlers());
		messageTemplateGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		SearchField search = FilterableGridHelper.generateSearchField(messageTemplateGrid, msg);
		
		Toolbar<MessageTemplate> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addHamburger(hamburgerMenu);
		toolbar.addSearch(search, Alignment.BOTTOM_RIGHT);
		ComponentWithToolbar msgGridWithToolbar = new ComponentWithToolbar(messageTemplateGrid, toolbar, Alignment.BOTTOM_LEFT);
		msgGridWithToolbar.setSpacing(false);
		msgGridWithToolbar.setSizeFull();
		
		VerticalLayout gridWrapper = new VerticalLayout();
		gridWrapper.setMargin(false);
		gridWrapper.setSpacing(true);
		gridWrapper.addComponent(buttonsBar);
		gridWrapper.setExpandRatio(buttonsBar, 0);
		gridWrapper.addComponent(msgGridWithToolbar);
		gridWrapper.setExpandRatio(msgGridWithToolbar, 2);
		gridWrapper.setSizeFull();
		
		Panel viewerPanel = new Panel();
		viewerPanel.setContent(viewer);
		viewerPanel.setSizeFull();
		viewerPanel.setStyleName(Styles.vPanelBorderless.toString());

		CompositeSplitPanel splitPanel = new CompositeSplitPanel(true, false, gridWrapper, viewerPanel, 50);
		splitPanel.setSizeFull();

		VerticalLayout main = new VerticalLayout();
		main.addComponent(splitPanel);
		main.setSizeFull();
		main.setMargin(false);
		setCompositionRoot(main);
		setSizeFull();
		refresh();
	}

	private void refresh()
	{
		messageTemplateGrid.setItems(getMessageTemplates());
		messageTemplateGrid.sort("name");
		viewer.setInput(null);
	}

	private List<SingleActionHandler<MessageTemplateEntry>> getRowActionsHandlers()
	{
		SingleActionHandler<MessageTemplateEntry> edit = SingleActionHandler.builder4Edit(msg, MessageTemplateEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();
		return Arrays.asList(edit);
	}

	private List<SingleActionHandler<MessageTemplateEntry>> getRowHamburgerHandlers()
	{
		SingleActionHandler<MessageTemplateEntry> remove = SingleActionHandler
				.builder4Delete(msg, MessageTemplateEntry.class).withHandler(items -> tryRemove(items))
				.build();

		SingleActionHandler<MessageTemplateEntry> preview = SingleActionHandler.builder(MessageTemplateEntry.class)
				.withCaption(msg.getMessage("MessageTemplatesView.preview"))
				.withIcon(Images.userMagnifier.getResource())
				.withHandler(items -> preview(items.iterator().next())).build();

		return Arrays.asList(preview, remove);
	}

	private List<SingleActionHandler<MessageTemplateEntry>> getGlobalHamburgerHandlers()
	{
		SingleActionHandler<MessageTemplateEntry> remove = SingleActionHandler
				.builder4Delete(msg, MessageTemplateEntry.class).withHandler(items -> tryRemove(items))
				.multiTarget().build();
		SingleActionHandler<MessageTemplateEntry> reset = SingleActionHandler.builder(MessageTemplateEntry.class)
				.withCaption(msg.getMessage("MessageTemplatesView.resetToDefault"))
				.withIcon(Images.reload.getResource()).withHandler(items -> resetFromConfig(items))
				.multiTarget().build();

		return Arrays.asList(remove, reset);
	}

	private void preview(MessageTemplateEntry toPreview)
	{
		MessageTemplate preprocessedTemplate;
		try
		{
			preprocessedTemplate = controller.getPreprocedMessageTemplate(toPreview.messageTemplate);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}
		getUI().addWindow(new PreviewWindow(preprocessedTemplate.getMessage().getBody().getValue(msg),
				preprocessedTemplate.getType().equals(MessageType.HTML) ? ContentMode.HTML
						: ContentMode.TEXT));
	}

	private void gotoEdit(MessageTemplateEntry e)
	{
		NavigationHelper.goToView(EditMessageTemplateView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + e.messageTemplate
						.getName());
	}

	private Collection<MessageTemplateEntry> getMessageTemplates()
	{
		try
		{
			return controller.getMessageTemplates();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private void remove(Set<MessageTemplateEntry> msgTemplates)
	{
		try
		{
			controller.removeMessageTemplates(msgTemplates);
			msgTemplates.forEach(m -> messageTemplateGrid.removeElement(m));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			refresh();
		}
	}

	private void tryRemove(Set<MessageTemplateEntry> msgTemplates)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg,
				msgTemplates.stream().map(m -> m.messageTemplate.getName()).collect(Collectors.toList()));
		new ConfirmDialog(msg, msg.getMessage("MessageTemplatesView.confirmDelete", confirmText),
				() -> remove(msgTemplates)).show();
	}

	private void resetFromConfig(Set<MessageTemplateEntry> msgTemplates)
	{
		try
		{
			controller.reloadFromConfiguration(
					msgTemplates.stream().map(m -> m.messageTemplate
							.getName()).collect(Collectors.toSet()));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		refresh();

	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.settings.messageTemplates");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	static class PreviewWindow extends Window
	{
		public PreviewWindow(String contentToPreview, ContentMode contentMode)
		{
			Label htmlPreview = new Label();
			htmlPreview.setContentMode(contentMode);
			htmlPreview.setValue(contentToPreview);
			setContent(htmlPreview);
			setModal(true);
		}
	}

	@Component
	public static class MessageTemplatesNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = VIEW_NAME;
		
		@Autowired
		public MessageTemplatesNavigationInfoProvider(MessageSource msg,
				ObjectFactory<MessageTemplatesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(SettingsNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.settings.messageTemplates"))
					.withIcon(Images.envelopes_open.getResource())
					.withPosition(10).build());

		}
	}
}
