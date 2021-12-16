/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.dnd.DragSourceExtension;
import com.vaadin.ui.dnd.DropTargetExtension;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Component showing a list of collapsable elements and add new element button.
 * Each element consists of a header and content - element editor. The order in
 * the list can be changed by DnD. In addition, each element has standard
 * actions placed in the hamburger menu: remove, move bottom, move top.
 * 
 * @author P.Piernik
 *
 */
public abstract class ListOfDnDCollapsableElements<T> extends CustomField<List<T>>
{
	private MessageSource msg;
	private VerticalLayout elementsLayout;
	private List<ElementComponent> elements;
	private VerticalLayout main;
	private Supplier<Editor<T>> editorProvider;
	private List<SingleActionHandler<T>> additionalActionHandlers;
	private Label captionLabel;

	public ListOfDnDCollapsableElements(MessageSource msg, Supplier<Editor<T>> editorProvider, String caption)
	{
		this(msg, editorProvider, caption, Collections.emptyList(), msg.getMessage("addNew"));
	}
	
	public ListOfDnDCollapsableElements(MessageSource msg, Supplier<Editor<T>> editorProvider, String caption, String addButtonCaption)
	{
		this(msg, editorProvider, caption, Collections.emptyList(), addButtonCaption);
	}

	public ListOfDnDCollapsableElements(MessageSource msg, Supplier<Editor<T>> editorProvider, String caption,
			List<SingleActionHandler<T>> additionalActionHandlers)
	{
		this.msg = msg;
		this.elements = new ArrayList<>();
		this.editorProvider = editorProvider;
		this.additionalActionHandlers = additionalActionHandlers;
		initUI(caption, msg.getMessage("addNew"));
	}
	
	public ListOfDnDCollapsableElements(MessageSource msg, Supplier<Editor<T>> editorProvider, String caption,
			List<SingleActionHandler<T>> additionalActionHandlers, String addButtonCaption)
	{
		this.msg = msg;
		this.elements = new ArrayList<>();
		this.editorProvider = editorProvider;
		this.additionalActionHandlers = additionalActionHandlers;
		initUI(caption, addButtonCaption);
	}

	private void initUI(String caption,String addButtonCaption)
	{
		main = new VerticalLayout();
		main.setMargin(false);

		HorizontalLayout elementsHeader = new HorizontalLayout();
		elementsHeader.setMargin(false);
		elementsHeader.setWidth(100, Unit.PERCENTAGE);
		Button addElement = new Button();
		addElement.setCaption(addButtonCaption);
		addElement.addStyleName("u-button-action");
		addElement.setIcon(Images.add.getResource());
		addElement.addClickListener(event -> {
			ElementComponent ec = addElementComponent(makeNewInstance());
			ec.showHideContent(true);
		});
		captionLabel = new Label(caption);
		elementsHeader.addComponents(captionLabel, addElement);
		elementsHeader.setComponentAlignment(captionLabel, Alignment.MIDDLE_LEFT);
		elementsHeader.setComponentAlignment(addElement, Alignment.MIDDLE_RIGHT);

		elementsLayout = new VerticalLayout();
		elementsLayout.setMargin(false);
		elementsLayout.setSpacing(false);
		elementsLayout.setStyleName(Styles.vDropLayout.toString());
		main.addComponent(elementsHeader);
		main.addComponent(elementsLayout);
	}
	
	public void setTitleVisible(boolean visible)
	{
		captionLabel.setVisible(visible);
	}

	protected abstract T makeNewInstance();

	@Override
	protected Component initContent()
	{
		return main;
	}

	private ElementComponent addElementComponent(T element)
	{
		ElementComponent ec = addElementComponentAt(element, elements.size());
		if (element == null)
			ec.setFocus();
		refreshElements();
		return ec;
	}

	private void remove(ElementComponent el)
	{
		elements.remove(el);
		refreshElements();
	}

	private void moveTop(ElementComponent el)
	{
		elements.remove(el);
		elements.add(0, el);
		refreshElements();
	}

	private void moveBottom(ElementComponent el)
	{
		elements.remove(el);
		elements.add(el);
		refreshElements();
	}

	private ElementComponent addElementComponentAt(T element, int index)
	{
		ElementComponent r = new ElementComponent(element);
		elements.add(index, r);
		return r;
	}

	protected void refreshElements()
	{
		fireChange();
		elementsLayout.removeAllComponents();
		if (elements.size() == 0)
			return;
		elementsLayout.addComponent(getDropElement(0));
		for (ElementComponent r : elements)
		{
			if (elements.size() > 1)
			{
				r.setTopVisible(true);
				r.setBottomVisible(true);
			} else
			{
				r.setTopVisible(false);
				r.setBottomVisible(false);
			}
		}

		elements.get(0).setTopVisible(false);
		elements.get(elements.size() - 1).setBottomVisible(false);
		for (ElementComponent r : elements)
		{
			elementsLayout.addComponent(r);
			elementsLayout.addComponent(getDropElement(elements.indexOf(r)));
		}

	}

	@SuppressWarnings("unchecked")
	private HorizontalLayout getDropElement(int pos)
	{
		HorizontalLayout drop = new HorizontalLayout();
		drop.setWidth(100, Unit.PERCENTAGE);
		drop.setHeight(1, Unit.EM);
		DropTargetExtension<HorizontalLayout> dropTarget = new DropTargetExtension<>(drop);
		dropTarget.setDropEffect(DropEffect.MOVE);
		dropTarget.addDropListener(event -> {
			Optional<AbstractComponent> dragSource = event.getDragSourceComponent();
			if (dragSource.isPresent() && dragSource.get() instanceof Button)
			{
				event.getDragData().ifPresent(data -> {
					ElementComponent sourceRule = (ElementComponent) data;
					elements.remove(sourceRule);
					elements.add(pos, sourceRule);
					refreshElements();
				});
			}
		});

		return drop;
	}

	private void fireChange()
	{
		fireEvent(new ValueChangeEvent<>(this, getValue(), true));
	}

	@Override
	public List<T> getValue()
	{
		return elements.stream().map(e -> e.getValue()).collect(Collectors.toList());
	}
	
	public void validate() throws FormValidationException
	{
		for (ElementComponent c: elements)
		{
			c.validate();
		}
	}

	@Override
	protected void doSetValue(List<T> value)
	{
		if (value == null)
			return;
		for (T v : value)
		{
			addElementComponent(v);
		}
	}

	public class ElementComponent extends CustomComponent
	{
		private Button showHide;
		private Label info;
		private Button dragImg;
		private HamburgerMenu<T> menuBar;
		private MenuItem top;
		private MenuItem bottom;
		private VerticalLayout content;
		private Editor<T> editor;

		public ElementComponent(T element)
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
			showHide.addClickListener(event -> showHideContent(!content.isVisible()));
			header.addComponent(showHide);
			header.setComponentAlignment(showHide, Alignment.MIDDLE_LEFT);

			info = new Label("");
			info.setSizeFull();
			header.addComponent(info);
			header.setComponentAlignment(info, Alignment.MIDDLE_LEFT);
			header.setExpandRatio(info, 1);

			dragImg = new Button(Images.resize.getResource());
			dragImg.setSizeFull();
			dragImg.setWidth(1, Unit.EM);
			dragImg.setStyleName(Styles.vButtonLink.toString());
			dragImg.addStyleName(Styles.vButtonBorderless.toString());
			dragImg.addStyleName(Styles.link.toString());

			DragSourceExtension<Button> dragSource = new DragSourceExtension<>(dragImg);
			dragSource.setEffectAllowed(EffectAllowed.MOVE);
			dragSource.setDragData(this);

			header.addComponent(dragImg);
			header.setComponentAlignment(dragImg, Alignment.MIDDLE_RIGHT);

			menuBar = new HamburgerMenu<>();
			menuBar.addActionHandlers(additionalActionHandlers);
			menuBar.addItem(msg.getMessage("remove"), Images.remove.getResource(),
					s -> remove(ElementComponent.this));
			top = menuBar.addItem(msg.getMessage("ListOfCollapsableElements.moveTop"),
					Images.topArrow.getResource(), s -> moveTop(ElementComponent.this));
			bottom = menuBar.addItem(msg.getMessage("ListOfCollapsableElements.moveBottom"),
					Images.bottomArrow.getResource(), s -> moveBottom(ElementComponent.this));

			header.addComponent(menuBar);
			header.setComponentAlignment(menuBar, Alignment.MIDDLE_RIGHT);
			header.setExpandRatio(menuBar, 0);

			header.addLayoutClickListener(event -> {
				if (!event.isDoubleClick())
					return;
				showHideContent(!content.isVisible());
			});

			headerWrapper.addComponent(header);
			headerWrapper.addComponent(HtmlTag.horizontalLine());

			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);
			main.setSpacing(false);
			content = new VerticalLayout();
			content.setMargin(false);
			content.setSpacing(true);

			editor = editorProvider.get();
			editor.addValueChangeListener(e -> info.setValue(editor.getHeaderText()));
			editor.addValueChangeListener(e -> fireChange());
			editor.setValue(element);
			content.addComponent(editor);

			showHideContent(false);

			main.addComponent(headerWrapper);
			main.addComponent(content);

			setCompositionRoot(main);
		}

		private void showHideContent(boolean show)
		{
			showHide.setIcon(show ? Images.upArrow.getResource() : Images.downArrow.getResource());
			content.setVisible(show);
		}

		public void setTopVisible(boolean v)
		{
			top.setVisible(v);
		}

		public void setBottomVisible(boolean v)
		{
			bottom.setVisible(v);
		}

		public T getValue()
		{
			return editor.getValue();
		}
		
		public void validate() throws FormValidationException
		{
			editor.validate();
		}

		public void setFocus()
		{
			editor.focus();

		}
	}

	public static abstract class Editor<V> extends CustomField<V>
	{
		protected abstract String getHeaderText();
		protected abstract void validate() throws FormValidationException;
	}

}
