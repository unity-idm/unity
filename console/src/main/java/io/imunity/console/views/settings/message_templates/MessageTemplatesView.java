/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.message_templates;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.ActionMenu;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.FlagIcon;
import io.imunity.vaadin.elements.MenuButton;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.msg_template.MessageType;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.icon.VaadinIcon.*;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.settings.messageTemplates")
@Route(value = "/message-templates", layout = ConsoleMenu.class)
public class MessageTemplatesView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final MessageTemplateController controller;
	private final TextField search = new TextField();
	private Grid<MessageTemplateEntry> messageTemplateGrid;
	private FormLayout selectedMessageTemplateDetails;

	MessageTemplatesView(MessageSource msg, MessageTemplateController controller)
	{
		this.msg = msg;
		this.controller = controller;

		MainMenu globalHamburgerHandlers = createMainMenu();
		initGrid(globalHamburgerHandlers);
		Component bottomPanel = createScrollablePanel();
		SplitLayout splitLayout = new SplitLayout(messageTemplateGrid, bottomPanel, SplitLayout.Orientation.VERTICAL);
		splitLayout.setSizeFull();
		splitLayout.setSplitterPosition(60);
		VerticalLayout layout = new VerticalLayout(createHeaderLayout(globalHamburgerHandlers.menu), splitLayout);
		layout.setSpacing(false);
		layout.setSizeFull();
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

	private void initGrid(MainMenu globalHamburgerHandlers)
	{
		messageTemplateGrid = new Grid<>();
		messageTemplateGrid.setSelectionMode(Grid.SelectionMode.MULTI);
		messageTemplateGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		messageTemplateGrid.setHeight("20em");
		messageTemplateGrid.addItemClickListener(e ->
		{
			if(messageTemplateGrid.getSelectedItems().contains(e.getItem()))
				messageTemplateGrid.deselect(e.getItem());
			else
				messageTemplateGrid.select(e.getItem());
		});
		Grid.Column<MessageTemplateEntry> nameColumn = messageTemplateGrid
				.addComponentColumn(m -> new RouterLink(m.messageTemplate.getName(), MessageTemplateEditView.class, m.messageTemplate.getName()))
				.setHeader(msg.getMessage("MessageTemplatesView.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(m -> m.messageTemplate.getName()));

		messageTemplateGrid.addColumn(m -> m.messageTemplate.getNotificationChannel())
				.setHeader(msg.getMessage("MessageTemplatesView.channelCaption"))
				.setSortable(true)
				.setAutoWidth(true);
		messageTemplateGrid.addColumn(m -> m.messageTemplate.getType().toString())
				.setHeader(msg.getMessage("MessageTemplatesView.messageTypeCaption"))
				.setSortable(true)
				.setAutoWidth(true);
		messageTemplateGrid.addColumn(m -> m.messageTemplate.getConsumer())
				.setHeader(msg.getMessage("MessageTemplatesView.purposeCaption"))
				.setSortable(true)
				.setAutoWidth(true);
		messageTemplateGrid.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);
		messageTemplateGrid.sort(GridSortOrder.asc(nameColumn).build());

		selectedMessageTemplateDetails = new FormLayout();
		selectedMessageTemplateDetails.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		messageTemplateGrid.addSelectionListener(event ->
		{
			MessageTemplate messageTemplate = event.getAllSelectedItems().size() != 1 ? null
					: controller.getPreprocedMessageTemplate(event.getFirstSelectedItem().get().messageTemplate);
			setSelectedMessageTemplateDetails(messageTemplate);
			globalHamburgerHandlers.setEnabled(!event.getAllSelectedItems().isEmpty());
		});
	}

	private VerticalLayout createHeaderLayout(Component globalHamburgerHandlers)
	{
		VerticalLayout headerLayout = new VerticalLayout();
		headerLayout.setPadding(false);
		Button addButton = new Button(msg.getMessage("addNew"), e -> UI.getCurrent().navigate(MessageTemplateEditView.class));
		addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addButton.setIcon(PLUS_CIRCLE_O.create());
		search.setValueChangeMode(ValueChangeMode.EAGER);
		search.setPlaceholder(msg.getMessage("search"));
		headerLayout.setAlignItems(FlexComponent.Alignment.END);
		HorizontalLayout lowerHeaderLayout = new HorizontalLayout(globalHamburgerHandlers, search);
		lowerHeaderLayout.setWidthFull();
		lowerHeaderLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		lowerHeaderLayout.setAlignItems(FlexComponent.Alignment.END);
		headerLayout.add(addButton, lowerHeaderLayout);
		headerLayout.setSpacing(false);
		return headerLayout;
	}

	private void setSelectedMessageTemplateDetails(MessageTemplate messageTemplate)
	{
		selectedMessageTemplateDetails.removeAll();
		if(messageTemplate == null)
			return;
		selectedMessageTemplateDetails.addFormItem(new Label(messageTemplate.getDescription()), msg.getMessage("MessageTemplateViewer.description"));
		selectedMessageTemplateDetails.addFormItem(new Label(controller.getCompatibilityInformation(messageTemplate)), msg.getMessage("MessageTemplateViewer.consumer"));

		I18nString subjectContent = messageTemplate.getMessage().getSubject();
		I18nString bodyContent = messageTemplate.getMessage().getBody();

		if (!subjectContent.isEmpty())
			selectedMessageTemplateDetails.addFormItem(new Span(new FlagIcon(msg.getLocale().getLanguage()), new Span(" "),
					new Label(subjectContent.getDefaultLocaleValue(msg))), msg.getMessage("MessageTemplateViewer.subject"));

		if (!bodyContent.isEmpty())
			selectedMessageTemplateDetails.addFormItem(new Span(new FlagIcon(msg.getLocale().getLanguage()), new Span(" "),
					new Label(bodyContent.getDefaultLocaleValue(msg))), msg.getMessage("MessageTemplateViewer.body"));
	}

	private void refresh()
	{
		GridListDataView<MessageTemplateEntry> messageTemplateEntryGridListDataView = messageTemplateGrid.setItems(controller.getMessageTemplates());
		messageTemplateEntryGridListDataView.addFilter(entry -> entry.anyFieldContains(search.getValue()));
		search.addValueChangeListener(e -> messageTemplateEntryGridListDataView.refreshAll());
		setSelectedMessageTemplateDetails(null);
	}

	private Component createRowActionMenu(MessageTemplateEntry entry)
	{
		ActionMenu actionMenu = new ActionMenu();

		MenuButton previewButton = new MenuButton(msg.getMessage("MessageTemplatesView.preview"), SEARCH);
		actionMenu.addItem(previewButton, e ->
		{
			preview(entry);
			refresh();
		});

		MenuButton removeButton = new MenuButton(msg.getMessage("remove"), TRASH);
		actionMenu.addItem(removeButton, e ->
		{
			tryRemove(Set.of(entry));
			refresh();
		});

		MenuButton resetButton = new MenuButton(msg.getMessage("MessageTemplatesView.resetToDefault"), RETWEET);
		actionMenu.addItem(resetButton, e -> resetFromConfig(Set.of(entry)));

		Icon generalSettings = EDIT.create();
		generalSettings.setTooltipText(msg.getMessage("edit"));
		generalSettings.getStyle().set("cursor", "pointer");
		generalSettings.addClickListener(e -> UI.getCurrent().navigate(MessageTemplateEditView.class, entry.messageTemplate.getName()));

		HorizontalLayout horizontalLayout = new HorizontalLayout(generalSettings, actionMenu.getTarget());
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	private MainMenu createMainMenu()
	{
		ActionMenu actionMenu = new ActionMenu();

		MenuButton removeButton = new MenuButton(msg.getMessage("remove"), TRASH);
		removeButton.setEnabled(false);
		actionMenu.addItem(removeButton, e ->
		{
			tryRemove(messageTemplateGrid.getSelectedItems());
			refresh();
		});

		MenuButton resetButton = new MenuButton(msg.getMessage("MessageTemplatesView.resetToDefault"), RETWEET);
		resetButton.setEnabled(false);
		actionMenu.addItem(resetButton, e -> resetFromConfig(messageTemplateGrid.getSelectedItems()));

		Component target = actionMenu.getTarget();
		target.getElement().getStyle().set("margin-left", "1.3em");
		return new MainMenu(target, Set.of(removeButton, resetButton));
	}

	private void preview(MessageTemplateEntry toPreview)
	{
		MessageTemplate preprocessedTemplate = controller.getPreprocedMessageTemplate(toPreview.messageTemplate);
		PreviewDialog dialog = new PreviewDialog(
				preprocessedTemplate.getMessage().getBody().getValue(msg),
				toPreview.messageTemplate.getType().equals(MessageType.HTML)
		);
		dialog.open();
	}

	private void remove(Set<MessageTemplateEntry> msgTemplates)
	{
		controller.removeMessageTemplates(msgTemplates);
		refresh();
	}

	private void tryRemove(Set<MessageTemplateEntry> msgTemplates)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg,
				msgTemplates.stream().map(m -> m.messageTemplate.getName()).collect(Collectors.toList()));

		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("MessageTemplatesView.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> remove(msgTemplates),
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}

	private void resetFromConfig(Set<MessageTemplateEntry> msgTemplates)
	{
		controller.reloadFromConfiguration(msgTemplates.stream().map(m -> m.messageTemplate.getName()).collect(Collectors.toSet()));
		refresh();
	}

}
