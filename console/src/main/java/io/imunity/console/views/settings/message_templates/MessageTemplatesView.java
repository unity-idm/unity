/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.message_templates;

import static com.vaadin.flow.component.icon.VaadinIcon.RETWEET;
import static com.vaadin.flow.component.icon.VaadinIcon.SEARCH;
import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;
import static io.imunity.vaadin.elements.CssClassNames.AVOID_MAIN_LAYOUT_Y_SCROLLER;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.FlagIcon;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.elements.grid.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.elements.grid.GridSearchFieldFactory;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.msg_template.MessageType;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.settings.messageTemplates", parent = "WebConsoleMenu.settings")
@Route(value = "/message-templates", layout = ConsoleMenu.class)
public class MessageTemplatesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final MessageTemplateController controller;
	private final Set<String> templatesFromConfig;
	private GridWithActionColumn<MessageTemplateEntry> messageTemplateGrid;
	private FormLayout selectedMessageTemplateDetails;
	private ComponentWithToolbar gridWithToolbar;

	MessageTemplatesView(MessageSource msg, MessageTemplateController controller)
	{
		this.msg = msg;
		this.controller = controller;
		this.templatesFromConfig = controller.getMessagesTemplatesFromConfiguration();
		initGrid();
		Component bottomPanel = createScrollablePanel();
		SplitLayout splitLayout = new SplitLayout(gridWithToolbar, bottomPanel, SplitLayout.Orientation.VERTICAL);
		splitLayout.setSizeFull();
		splitLayout.setSplitterPosition(60);
		VerticalLayout layout = new VerticalLayout(createHeaderActionLayout(msg, MessageTemplateEditView.class),
				splitLayout);
		layout.setSpacing(false);
		layout.setWidthFull();
		layout.addClassName(AVOID_MAIN_LAYOUT_Y_SCROLLER.getName());
		getContent().setHeightFull();
		getContent().add(layout);
		refresh();
	}

	private Scroller createScrollablePanel()
	{
		Scroller scroller = new Scroller(selectedMessageTemplateDetails);
		scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
		return scroller;
	}

	private void initGrid()
	{
		messageTemplateGrid = new GridWithActionColumn<MessageTemplateEntry>(msg::getMessage, getRowActionsHandlers());
		messageTemplateGrid.setSelectionMode(Grid.SelectionMode.MULTI);
		messageTemplateGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		messageTemplateGrid.setHeight("20em");
		messageTemplateGrid.addItemClickListener(e ->
		{
			if (messageTemplateGrid.getSelectedItems()
					.contains(e.getItem()))
				messageTemplateGrid.deselect(e.getItem());
			else
				messageTemplateGrid.select(e.getItem());
		});
		Grid.Column<MessageTemplateEntry> nameColumn = messageTemplateGrid
				.addComponentColumn(m -> new RouterLink(m.messageTemplate.getName(), MessageTemplateEditView.class,
						m.messageTemplate.getName()))
				.setHeader(msg.getMessage("MessageTemplatesView.nameCaption"))
				.setAutoWidth(true)
				.setResizable(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(m -> m.messageTemplate.getName()));

		messageTemplateGrid.addColumn(m -> m.messageTemplate.getNotificationChannel())
				.setHeader(msg.getMessage("MessageTemplatesView.channelCaption"))
				.setSortable(true)
				.setResizable(true)
				.setAutoWidth(true);
		messageTemplateGrid.addColumn(m -> m.messageTemplate.getType()
				.toString())
				.setHeader(msg.getMessage("MessageTemplatesView.messageTypeCaption"))
				.setSortable(true)
				.setResizable(true)
				.setAutoWidth(true);
		messageTemplateGrid.addColumn(m -> m.messageTemplate.getConsumer())
				.setHeader(msg.getMessage("MessageTemplatesView.purposeCaption"))
				.setSortable(true)
				.setResizable(true)
				.setAutoWidth(true);

		messageTemplateGrid.addHamburgerActions(getRowHamburgerHandlers());
		messageTemplateGrid.sort(GridSortOrder.asc(nameColumn)
				.build());

		selectedMessageTemplateDetails = new FormLayout();
		selectedMessageTemplateDetails.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		messageTemplateGrid.addSelectionListener(event ->
		{
			MessageTemplate messageTemplate = event.getAllSelectedItems()
					.size() != 1 ? null
							: controller.getPreprocedMessageTemplate(event.getFirstSelectedItem()
									.get().messageTemplate);
			setSelectedMessageTemplateDetails(messageTemplate);
		});

		ActionMenuWithHandlerSupport<MessageTemplateEntry> hamburgerMenu = new ActionMenuWithHandlerSupport<>();
		hamburgerMenu.addActionHandlers(getGlobalHamburgerHandlers());
		messageTemplateGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		SearchField search = GridSearchFieldFactory.generateSearchField(messageTemplateGrid, msg::getMessage);

		Toolbar<MessageTemplateEntry> toolbar = new Toolbar<>();
		toolbar.addHamburger(hamburgerMenu);
		toolbar.addSearch(search);
		gridWithToolbar = new ComponentWithToolbar(messageTemplateGrid, toolbar);
		gridWithToolbar.setSpacing(false);
		gridWithToolbar.setSizeFull();

	}

	private List<SingleActionHandler<MessageTemplateEntry>> getRowActionsHandlers()
	{
		SingleActionHandler<MessageTemplateEntry> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, MessageTemplateEntry.class)
				.withHandler(r -> gotoEdit(r.iterator()
						.next()))
				.build();
		return Arrays.asList(edit);
	}

	private void gotoEdit(MessageTemplateEntry next)
	{
		UI.getCurrent()
				.navigate(MessageTemplateEditView.class, next.messageTemplate.getName());
	}

	private List<SingleActionHandler<MessageTemplateEntry>> getRowHamburgerHandlers()
	{
		SingleActionHandler<MessageTemplateEntry> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, MessageTemplateEntry.class)
				.withHandler(items -> tryRemove(items))
				.build();

		SingleActionHandler<MessageTemplateEntry> preview = SingleActionHandler.builder(MessageTemplateEntry.class)
				.withCaption(msg.getMessage("MessageTemplatesView.preview"))
				.withIcon(SEARCH)
				.withHandler(items -> preview(items.iterator()
						.next()))
				.build();

		SingleActionHandler<MessageTemplateEntry> reset = SingleActionHandler.builder(MessageTemplateEntry.class)
				.withCaption(msg.getMessage("MessageTemplatesView.resetToDefault"))
				.withDisabledPredicate(e -> !templatesFromConfig.contains(e.messageTemplate.getName()))
				.withIcon(RETWEET)
				.withHandler(items -> resetFromConfig(items))
				.multiTarget()
				.build();

		return Arrays.asList(preview, reset, remove);
	}

	private List<SingleActionHandler<MessageTemplateEntry>> getGlobalHamburgerHandlers()
	{
		SingleActionHandler<MessageTemplateEntry> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, MessageTemplateEntry.class)
				.withHandler(items -> tryRemove(items))
				.multiTarget()
				.build();

		SingleActionHandler<MessageTemplateEntry> reset = SingleActionHandler.builder(MessageTemplateEntry.class)
				.withCaption(msg.getMessage("MessageTemplatesView.resetToDefault"))
				.withDisabledPredicate(e -> !templatesFromConfig.contains(e.messageTemplate.getName()))
				.withIcon(RETWEET)
				.withHandler(items -> resetFromConfig(items))
				.multiTarget()
				.build();

		return Arrays.asList(remove, reset);
	}

	private void setSelectedMessageTemplateDetails(MessageTemplate messageTemplate)
	{
		selectedMessageTemplateDetails.removeAll();
		if (messageTemplate == null)
			return;
		selectedMessageTemplateDetails.addFormItem(new Span(messageTemplate.getDescription()),
				msg.getMessage("MessageTemplateViewer.description"));
		selectedMessageTemplateDetails.addFormItem(new Span(controller.getCompatibilityInformation(messageTemplate)),
				msg.getMessage("MessageTemplateViewer.consumer"));

		I18nString subjectContent = messageTemplate.getMessage()
				.getSubject();
		I18nString bodyContent = messageTemplate.getMessage()
				.getBody();

		addLocalizedContent(subjectContent, msg.getMessage("MessageTemplateViewer.subject"));
		addLocalizedContent(bodyContent, msg.getMessage("MessageTemplateViewer.body"));
	}

	private void addLocalizedContent(I18nString subjectContent, String label)
	{
		msg.getEnabledLocales()
				.values()
				.stream()
				.filter(locale -> subjectContent.getValue(locale.getLanguage()) != null)
				.filter(locale -> !subjectContent.getValue(locale.getLanguage())
						.isBlank())
				.forEach(locale -> selectedMessageTemplateDetails
						.addFormItem(new Span(new FlagIcon(locale.getLanguage()), new Span(" "),
								new Span(subjectContent.getValue(locale.getLanguage()))), label));
	}

	private void refresh()
	{
		messageTemplateGrid.setItems(controller.getMessageTemplates());
		setSelectedMessageTemplateDetails(null);
	}

	private void preview(MessageTemplateEntry toPreview)
	{
		MessageTemplate preprocessedTemplate = controller.getPreprocedMessageTemplate(toPreview.messageTemplate);
		PreviewDialog dialog = new PreviewDialog(preprocessedTemplate.getMessage()
				.getBody()
				.getValue(msg),
				toPreview.messageTemplate.getType()
						.equals(MessageType.HTML));
		dialog.open();
	}

	private void remove(Set<MessageTemplateEntry> msgTemplates)
	{
		controller.removeMessageTemplates(msgTemplates);
		refresh();
	}

	private void tryRemove(Set<MessageTemplateEntry> msgTemplates)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg, msgTemplates.stream()
				.map(m -> m.messageTemplate.getName())
				.collect(Collectors.toList()));

		new ConfirmDialog(msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("MessageTemplatesView.confirmDelete", confirmText), msg.getMessage("ok"),
				e -> remove(msgTemplates), msg.getMessage("cancel"), e ->
				{
				}).open();
	}

	private void resetFromConfig(Set<MessageTemplateEntry> msgTemplates)
	{
		controller.reloadFromConfiguration(msgTemplates.stream()
				.map(m -> m.messageTemplate.getName())
				.collect(Collectors.toSet()));
		refresh();
	}

}
