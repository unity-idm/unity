/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Base layout for all {@link ColumnComponent}s
 * 
 * @author P.Piernik
 *
 */
public abstract class ColumnComponentBase extends VerticalLayout implements ColumnComponent
{
	protected final MessageSource msg;
	private final String title;
	private final VaadinIcon icon;
	private final Runnable dragStart;
	private final Runnable dragStop;
	private VerticalLayout contentLayout;
	private Icon removeButton;
	private Icon expand;
	private Icon collapse;

	private HorizontalLayout header;
	private Consumer<ColumnComponent> removeElementListener;

	public ColumnComponentBase(MessageSource msg, String title, VaadinIcon icon, Runnable dragStart,
			Runnable dragStop, Consumer<ColumnComponent> removeElementListener)
	{
		this.msg = msg;
		this.title = title;
		this.icon = icon;
		this.dragStart = dragStart;
		this.dragStop = dragStop;
		this.removeElementListener = removeElementListener;
		initUI();
	}

	public void initUI()
	{
		setWidthFull();
		setSpacing(false);
		setMargin(false);
		setPadding(false);
		addClassName("u-border");

		header = new HorizontalLayout();
		header.setSpacing(false);
		header.setPadding(true);
		header.setWidthFull();
		header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
		header.setJustifyContentMode(JustifyContentMode.BETWEEN);
		header.setHeight(3, Unit.EM);
		
		HorizontalLayout leftButtons = new HorizontalLayout();
		leftButtons.setMargin(false);
		leftButtons.setSpacing(false);
		leftButtons.setDefaultVerticalComponentAlignment(Alignment.CENTER);

		expand = new Icon(VaadinIcon.CARET_RIGHT);
		collapse = new Icon(VaadinIcon.CARET_DOWN);
		expand.addClickListener(e -> changeMode());
		expand.setVisible(false);
		collapse.addClickListener(e -> changeMode());
		collapse.setVisible(false);
		leftButtons.add(expand, collapse);

		Icon ic = new Icon(icon);
		leftButtons.add(ic);
		header.add(leftButtons);
		NativeLabel captionL = new NativeLabel();
		captionL.setText(title);
		header.add(captionL);

		HorizontalLayout rightButtons = new HorizontalLayout();
		rightButtons.setMargin(false);

		Icon remove =  new Icon(VaadinIcon.CLOSE_SMALL);
		remove.setTooltipText(msg.getMessage("ColumnElementBase.remove"));
		remove.addClickListener(e -> {
			if (removeElementListener != null)
				removeElementListener.accept(this);
		});
		remove.setVisible(removeElementListener != null);
		rightButtons.add(remove);

		header.add(rightButtons);
		header.addClassName("u-columnHeader");
		add(header);

		contentLayout = new VerticalLayout();
		contentLayout.setMargin(false);
		contentLayout.setPadding(false);
		contentLayout.setSpacing(false);
		contentLayout.setVisible(false);
		contentLayout.setWidthFull();
		add(contentLayout);


		DragSource<ColumnComponentBase> dragSource = DragSource.create(this);
		dragSource.setEffectAllowed(EffectAllowed.MOVE);
		dragSource.setDragData(this);

		dragSource.addDragStartListener(e -> dragStart.run());
		dragSource.addDragEndListener(e -> dragStop.run());
	}

	private void changeMode()
	{
		if (contentLayout.isVisible())
		{
			collapse();
		} else
		{
			expand();
		}
	}

	public void collapse()
	{
		contentLayout.setVisible(false);
		expand.setVisible(false);
		collapse.setVisible(true);

	}

	public void expand()
	{
		contentLayout.setVisible(true);
		expand.setVisible(true);
		collapse.setVisible(false);
	}

	protected void addContent(Component content)
	{
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(true);
		wrapper.setPadding(false);
		wrapper.setSpacing(false);
		wrapper.setWidthFull();
		wrapper.add(content);
		wrapper.setJustifyContentMode(JustifyContentMode.CENTER);
		contentLayout.add(wrapper);
		expand();
	}

	public void setRemoveListener(Consumer<ColumnComponent> removeListener)
	{
		this.removeElementListener = removeListener;
		removeButton.setVisible(removeElementListener != null);
	}

	@Override
	public void refresh()
	{

	}

	@Override
	public void validate() throws FormValidationException
	{

	}

	@Override
	public void addValueChangeListener(Runnable valueChange)
	{

	}
	
	@Override
	public Component getComponent()
	{
		return this;
	}
}
