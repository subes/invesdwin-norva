package de.invesdwin.norva.beanpath.spi;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.norva.beanpath.BeanPathStrings;
import de.invesdwin.norva.beanpath.annotation.BeanPathEndPoint;
import de.invesdwin.norva.beanpath.annotation.Tabbed;
import de.invesdwin.norva.beanpath.spi.context.ABeanPathContext;
import de.invesdwin.norva.beanpath.spi.element.ABeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.ButtonBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.CheckBoxBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.ComboBoxBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.ContainerOpenBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.HiddenBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.IBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.RootBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.TabbedBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.TabbedColumnBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.TableBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.TableButtonColumnBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.TableTextColumnBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.TextFieldBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.UploadButtonBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.simple.SimpleActionBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.simple.SimplePropertyBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.utility.ChoiceBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.utility.ColumnOrderBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.utility.ContainerTitleBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.utility.DisableBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.utility.HideBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.utility.IUtilityBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.utility.RemoveFromBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.utility.TitleBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.utility.TooltipBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.utility.ValidateBeanPathElement;
import de.invesdwin.norva.beanpath.spi.visitor.IBeanPathVisitor;

// CS:OFF ClassDataAbstraction
@NotThreadSafe
public abstract class ABeanPathProcessor<X extends ABeanPathContext, C extends IBeanPathContainer> {
    //CS:ON ClassDataAbstraction

    private static final String[] ELEMENT_NAME_BLACKLIST = { "getClass", "hashCode", "clone", "equals", "toString",
            "compareTo", "wait", "notify", "notifyAll", "readResolve", "afterPropertiesSet" };

    private final X context;
    private final IBeanPathVisitor[] visitors;
    private final Set<String> duplicateBeanPathsFilter = new HashSet<String>();

    private boolean shallowOnly;
    private boolean ignoreBeanPathEndPointAnnotation;

    @SafeVarargs
    public ABeanPathProcessor(final X context, final IBeanPathVisitor... visitors) {
        this.context = context;
        this.visitors = visitors;
    }

    public ABeanPathProcessor<X, C> withShallowOnly() {
        this.shallowOnly = true;
        return this;
    }

    public boolean isShallowOnly() {
        return shallowOnly;
    }

    public boolean isIgnoreBeanPathEndPointAnnotation() {
        return ignoreBeanPathEndPointAnnotation;
    }

    public ABeanPathProcessor<X, C> withIgnoreBeanPathEndPointAnnotation(
            final boolean ignoreBeanPathEndPointAnnotation) {
        this.ignoreBeanPathEndPointAnnotation = ignoreBeanPathEndPointAnnotation;
        return this;
    }

    public final X getContext() {
        return context;
    }

    @SuppressWarnings("unchecked")
    public final void process() {
        final boolean scanned = scanContainer(new RootBeanPathElement(context), (C) context.getRootContainer());
        if (scanned) {
            // hide any leftover utilities
            for (final IBeanPathElement element : getContext().getElementRegistry().getElements()) {
                if (element instanceof IUtilityBeanPathElement) {
                    final IUtilityBeanPathElement cElement = (IUtilityBeanPathElement) element;
                    if (!cElement.isAttachedToElement()) {
                        cElement.setAttachedToElement(element);
                        new HiddenBeanPathElement(element).accept(visitors);
                    }
                }
            }
            for (final IBeanPathVisitor visitor : visitors) {
                visitor.finish();
            }
        }
    }

    private boolean scanContainer(final IBeanPathElement parentElement, final C container) {
        if (container.getType().isAbstract()) {
            return false;
        }

        final ScanResult result = scanContainerShallow(container);

        final List<ContainerOpenBeanPathElement> containerOpenElements = new ArrayList<ContainerOpenBeanPathElement>();
        final List<TableBeanPathElement> tableElements = new ArrayList<TableBeanPathElement>();
        final List<TabbedBeanPathElement> tabbedElements = new ArrayList<TabbedBeanPathElement>();
        // 1. identify utility actions
        final List<SimpleActionBeanPathElement> nonUtilityActionElements = new ArrayList<SimpleActionBeanPathElement>();
        processUtilityActionElements(result.getActions(), nonUtilityActionElements);
        // 2. accept root if not done so right after scanning his utility actions
        final boolean parentAccepted = parentElement.accept(visitors);
        org.assertj.core.api.Assertions
                .assertThat(!parentElement.getBeanPath().equals(RootBeanPathElement.ROOT_BEAN_PATH) || parentAccepted)
                .isTrue();
        if (!parentAccepted) {
            return false;
        }
        // 3. identify utility properties, then process Checkbox, ComboBox, TextField
        processPropertyElements(result.getProperties(), containerOpenElements, tableElements, tabbedElements);
        // 4. ContainerOpen
        for (final ContainerOpenBeanPathElement containerOpenElement : containerOpenElements) {
            processContainerOpenElement(containerOpenElement);
        }
        // 5. Table (with Column and Button)
        processTableElements(tableElements);
        // 6. Tabbed
        for (final TabbedBeanPathElement tabbedElement : tabbedElements) {
            new ContainerOpenBeanPathElement(tabbedElement.getSimplePropertyElement(), false).accept(visitors);
            tabbedElement.accept(visitors);
            for (final IBeanPathVisitor visitor : visitors) {
                visitor.visitContainerClose();
            }
        }
        // 7. Button
        for (final SimpleActionBeanPathElement actionElement : nonUtilityActionElements) {
            boolean buttonAlreadyProcessed = false;
            if (actionElement.getAccessor().getType().isInstanceOf(File.class)
                    && actionElement.getAccessor().getPublicActionParameterCount() == 1) {
                new UploadButtonBeanPathElement(actionElement).accept(visitors);
                buttonAlreadyProcessed = true;
            }
            if (!buttonAlreadyProcessed) {
                new ButtonBeanPathElement(actionElement).accept(visitors);
            }
        }
        return true;
    }

    private void processTableElements(final List<TableBeanPathElement> tableElements) {
        for (final TableBeanPathElement tableElement : tableElements) {
            final boolean containerOpenAccepted = new ContainerOpenBeanPathElement(
                    tableElement.getSimplePropertyElement(), false).accept(visitors);
            tableElement.accept(visitors);
            if (containerOpenAccepted) {
                for (final IBeanPathVisitor visitor : visitors) {
                    visitor.visitContainerClose();
                }
            }
        }
    }

    private void processUtilityActionElements(final List<SimpleActionBeanPathElement> actionElements,
            final List<SimpleActionBeanPathElement> nonUtilityActionElements) {
        for (final SimpleActionBeanPathElement actionElement : actionElements) {
            if (shouldProcessElement(actionElement)) {
                if (duplicateBeanPathsFilter.add(actionElement.getBeanPath())) {
                    if (actionElement.getAccessor().getRawName().startsWith(DisableBeanPathElement.DISABLE_PREFIX)) {
                        new DisableBeanPathElement(actionElement).accept();
                    } else if (actionElement.getAccessor()
                            .getRawName()
                            .startsWith(ValidateBeanPathElement.VALIDATE_PREFIX)) {
                        new ValidateBeanPathElement(actionElement).accept();
                    } else if (actionElement.getAccessor().getRawName().startsWith(HideBeanPathElement.HIDE_PREFIX)) {
                        new HideBeanPathElement(actionElement).accept();
                    } else if (actionElement.getAccessor()
                            .getRawName()
                            .startsWith(RemoveFromBeanPathElement.REMOVE_FROM_PREFIX)) {
                        new RemoveFromBeanPathElement(actionElement).accept();
                    } else if (actionElement.getAccessor()
                            .getRawName()
                            .equals(ContainerTitleBeanPathElement.CONTAINER_TITLE_BEAN_PATH_FRAGMENT)) {
                        new ContainerTitleBeanPathElement(actionElement).accept();
                    } else if (actionElement.getAccessor()
                            .getRawName()
                            .equals(ColumnOrderBeanPathElement.COLUMN_ORDER_BEAN_PATH_FRAGMENT)) {
                        new ColumnOrderBeanPathElement(actionElement).accept();
                    } else if (actionElement.getAccessor().getRawName().endsWith(TitleBeanPathElement.TITLE_SUFFIX)) {
                        new TitleBeanPathElement(actionElement).accept();
                    } else if (actionElement.getAccessor()
                            .getRawName()
                            .endsWith(TooltipBeanPathElement.TOOLTIP_SUFFIX)) {
                        new TooltipBeanPathElement(actionElement).accept();
                    } else {
                        nonUtilityActionElements.add(actionElement);
                    }
                }
            }
        }
    }

    private void processContainerOpenElement(final ContainerOpenBeanPathElement containerOpenElement) {
        IBeanPathContainer parent = containerOpenElement.getContainer();
        while (parent != null) {
            if (parent.getType()
                    .getQualifiedName()
                    .equals(containerOpenElement.getAccessor().getType().getQualifiedName())) {
                //skip recursive loops
                return;
            } else {
                parent = parent.getParent();
            }
        }
        final boolean containerOpen;
        if (!isShallowOnly()) {
            final C subContainer = newSubContainer(containerOpenElement);
            containerOpen = scanContainer(containerOpenElement, subContainer);
        } else {
            containerOpen = containerOpenElement.accept(visitors);
        }
        if (containerOpen) {
            for (final IBeanPathVisitor visitor : visitors) {
                visitor.visitContainerClose();
            }
        } else if (context.getElementRegistry().getElement(containerOpenElement.getBeanPath()) == null) {
            new HiddenBeanPathElement(containerOpenElement).accept(visitors);
        }
    }

    private void processPropertyElements(final List<SimplePropertyBeanPathElement> propertyElements,
            final List<ContainerOpenBeanPathElement> containerOpenElements,
            final List<TableBeanPathElement> tableElements, final List<TabbedBeanPathElement> tabbedElements) {

        final List<SimplePropertyBeanPathElement> nonUtilityPropertyElements = new ArrayList<SimplePropertyBeanPathElement>();
        processUtilityPropertyElements(propertyElements, nonUtilityPropertyElements);

        //process normal properties and attach selection properties to them if possible
        for (final SimplePropertyBeanPathElement propertyElement : nonUtilityPropertyElements) {
            if (isBeanPathEndpoint(propertyElement)) {
                if (propertyElement.getAccessor().getRawType().isBoolean()) {
                    new CheckBoxBeanPathElement(propertyElement).accept(visitors);
                    //explicitly checking type instead of rawType here on enums
                } else if (propertyElement.getAccessor().getType().isEnum()) {
                    new ComboBoxBeanPathElement(propertyElement).accept(visitors);
                } else if (propertyElement.getAccessor().getRawType().isIterable()
                        || propertyElement.getAccessor().getRawType().isArray() || hasChoice(propertyElement)) {
                    processPotentialTableElement(propertyElement, tableElements);
                } else {
                    new TextFieldBeanPathElement(propertyElement).accept(visitors);
                }
            } else {
                if (propertyElement.getAccessor().getAnnotation(Tabbed.class) != null
                        || propertyElement.getAccessor().getRawType().getAnnotation(Tabbed.class) != null) {
                    processTabbedElement(propertyElement, tabbedElements);
                } else {
                    containerOpenElements.add(new ContainerOpenBeanPathElement(propertyElement, true));
                }
            }
        }
    }

    private boolean hasChoice(final SimplePropertyBeanPathElement propertyElement) {
        return propertyElement.getContext().getElementRegistry().getElement(
                propertyElement.getBeanPath() + ChoiceBeanPathElement.CHOICE_SUFFIX) != null;
    }

    private void processUtilityPropertyElements(final List<SimplePropertyBeanPathElement> propertyElements,
            final List<SimplePropertyBeanPathElement> nonUtilityPropertyElements) {

        for (final SimplePropertyBeanPathElement propertyElement : propertyElements) {
            if (shouldProcessElement(propertyElement)) {
                if (duplicateBeanPathsFilter.add(propertyElement.getBeanPath())) {
                    if (propertyElement.getAccessor().getRawName().endsWith(ChoiceBeanPathElement.CHOICE_SUFFIX)) {
                        new ChoiceBeanPathElement(propertyElement, true).accept();
                    } else if (propertyElement.getAccessor().getRawName().endsWith(TitleBeanPathElement.TITLE_SUFFIX)) {
                        new TitleBeanPathElement(propertyElement).accept();
                    } else if (propertyElement.getAccessor()
                            .getRawName()
                            .endsWith(TooltipBeanPathElement.TOOLTIP_SUFFIX)) {
                        new TooltipBeanPathElement(propertyElement).accept();
                    } else {
                        nonUtilityPropertyElements.add(propertyElement);
                    }
                }
            }
        }
    }

    private void processTabbedElement(final SimplePropertyBeanPathElement propertyElement,
            final List<TabbedBeanPathElement> tabbedElements) {

        final C subContainer = newSubContainer(new ContainerOpenBeanPathElement(propertyElement, false));
        final ScanResult result = scanContainerShallow(subContainer);

        final List<SimpleActionBeanPathElement> nonUtilityActionElements = new ArrayList<SimpleActionBeanPathElement>();
        processUtilityActionElements(result.getActions(), nonUtilityActionElements);

        final List<SimplePropertyBeanPathElement> nonUtilityPropertyElements = new ArrayList<SimplePropertyBeanPathElement>();
        processUtilityPropertyElements(result.getProperties(), nonUtilityPropertyElements);

        final List<HiddenBeanPathElement> invalidColumns = new ArrayList<HiddenBeanPathElement>();
        final List<TabbedColumnBeanPathElement> rawColumns = new ArrayList<TabbedColumnBeanPathElement>();
        for (final SimplePropertyBeanPathElement columnElement : nonUtilityPropertyElements) {
            if (isBeanPathEndpoint(propertyElement)) {
                //invalidate endpoint properties
                invalidColumns.add(new HiddenBeanPathElement(columnElement));
            } else {
                rawColumns.add(new TabbedColumnBeanPathElement(columnElement));
            }
        }

        //invalidate non utility actions
        for (final SimpleActionBeanPathElement actionElement : nonUtilityActionElements) {
            invalidColumns.add(new HiddenBeanPathElement(actionElement));
        }

        tabbedElements.add(new TabbedBeanPathElement(propertyElement, rawColumns, invalidColumns));
    }

    private void processPotentialTableElement(final SimplePropertyBeanPathElement propertyElement,
            final List<TableBeanPathElement> tableElements) {
        boolean isComboBox = propertyElement.getAccessor().getType().isJavaType()
                || propertyElement.getAccessor().getType().isNumber();
        if (!isComboBox) {
            final C subContainer = newSubContainer(new ContainerOpenBeanPathElement(propertyElement, false));
            final ScanResult result = scanContainerShallow(subContainer);

            final List<SimpleActionBeanPathElement> nonUtilityActionElements = new ArrayList<SimpleActionBeanPathElement>();
            processUtilityActionElements(result.getActions(), nonUtilityActionElements);

            final List<SimplePropertyBeanPathElement> nonUtilityPropertyElements = new ArrayList<SimplePropertyBeanPathElement>();
            processUtilityPropertyElements(result.getProperties(), nonUtilityPropertyElements);

            final List<TableTextColumnBeanPathElement> textColumns = new ArrayList<TableTextColumnBeanPathElement>();
            for (final SimplePropertyBeanPathElement property : nonUtilityPropertyElements) {
                textColumns.add(new TableTextColumnBeanPathElement(property));
            }
            final List<TableButtonColumnBeanPathElement> buttonColumns = new ArrayList<TableButtonColumnBeanPathElement>();
            for (final SimpleActionBeanPathElement actionMethod : nonUtilityActionElements) {
                buttonColumns.add(new TableButtonColumnBeanPathElement(actionMethod));
            }
            if (textColumns.size() > 0 || buttonColumns.size() > 0) {
                final TableBeanPathElement tableElement = new TableBeanPathElement(propertyElement, textColumns,
                        buttonColumns);
                tableElements.add(tableElement);
                isComboBox = false;
            }
        }
        if (isComboBox) {
            //no table afterall, thus using combobox instead
            new ComboBoxBeanPathElement(propertyElement).accept(visitors);
        }
    }

    private boolean shouldProcessElement(final ABeanPathElement element) {
        if (!element.getAccessor().isPublic() || element.getAccessor().isStatic()) {
            return false;
        }
        if (!element.getAccessor().hasPublicSetter() && !element.getAccessor().hasPublicGetter()
                && !element.getAccessor().hasPublicAction()) {
            return false;
        }
        if (hasUnexpectedParameters(element)) {
            return false;
        }

        for (final String blacklistedElementName : ELEMENT_NAME_BLACKLIST) {
            if (blacklistedElementName.equals(element.getAccessor().getRawName())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasUnexpectedParameters(final ABeanPathElement element) {
        if (element.getAccessor().hasPublicSetter() && element.getAccessor().getPublicSetterParameterCount() != 1) {
            return true;
        }
        if (element.getAccessor().hasPublicGetter() && element.getAccessor().getPublicGetterParameterCount() != 0) {
            return true;
        }
        if (element.getAccessor().hasPublicAction()) {
            if (BeanPathStrings.startsWithAny(element.getAccessor().getBeanPathFragment(),
                    ValidateBeanPathElement.VALIDATE_PREFIX, RemoveFromBeanPathElement.REMOVE_FROM_PREFIX)) {
                return element.getAccessor().getPublicActionParameterCount() != 1;
            }
            if (element.getAccessor().getType().isInstanceOf(File.class)) {
                return element.getAccessor().getPublicActionParameterCount() > 1;
            }
            return element.getAccessor().getPublicActionParameterCount() != 0;
        }
        return false;
    }

    private boolean isBeanPathEndpoint(final SimplePropertyBeanPathElement propertyElement) {
        final boolean isIterableOrJavaType = propertyElement.getAccessor().getRawType().isArray()
                || hasChoice(propertyElement) || propertyElement.getAccessor().getRawType().isJavaType();
        if (isIterableOrJavaType) {
            return true;
        }
        if (!ignoreBeanPathEndPointAnnotation) {
            final boolean hasBeanPathEndPointAnnotation = propertyElement.getAccessor()
                    .getRawType()
                    .getAnnotation(BeanPathEndPoint.class) != null
                    || propertyElement.getAccessor().getAnnotation(BeanPathEndPoint.class) != null;
            if (hasBeanPathEndPointAnnotation) {
                return true;
            }
        }
        return propertyElement.getAccessor().getRawType().isVoid()
                || propertyElement.getAccessor().getRawType().isEnum()
                || propertyElement.getAccessor().getRawType().isPrimitive()
                || propertyElement.getAccessor().getRawType().isNumber()
                || propertyElement.getAccessor().getRawType().isDate();
    }

    protected abstract C newSubContainer(ContainerOpenBeanPathElement containerOpenElement);

    private ScanResult scanContainerShallow(final C container) {
        final ScanResult result = new ScanResult();
        innerScanContainerShallow(container, result);
        result.sort();
        return result;
    }

    protected abstract void innerScanContainerShallow(final C container, final ScanResult result);

    public static class ScanResult {
        private final List<SimplePropertyBeanPathElement> propertyMethods = new ArrayList<SimplePropertyBeanPathElement>();
        private final List<SimplePropertyBeanPathElement> propertyFields = new ArrayList<SimplePropertyBeanPathElement>();
        private final List<SimpleActionBeanPathElement> actionsMethods = new ArrayList<SimpleActionBeanPathElement>();

        public void addPropertyMethod(final SimplePropertyBeanPathElement propertyMethod) {
            propertyMethods.add(propertyMethod);
        }

        public void addPropertyField(final SimplePropertyBeanPathElement propertyField) {
            propertyFields.add(propertyField);
        }

        public void addActionMethod(final SimpleActionBeanPathElement actionMethod) {
            actionsMethods.add(actionMethod);
        }

        /**
         * Ensures that the generated output does not differ too much each time.
         */
        public void sort() {
            Collections.sort(propertyMethods, ABeanPathElement.COMPARATOR);
            Collections.sort(propertyFields, ABeanPathElement.COMPARATOR);
            Collections.sort(actionsMethods, ABeanPathElement.COMPARATOR);
        }

        public List<SimplePropertyBeanPathElement> getProperties() {
            final List<SimplePropertyBeanPathElement> properties = new ArrayList<SimplePropertyBeanPathElement>();
            properties.addAll(propertyMethods);
            //fields have a lower priority than methods
            properties.addAll(propertyFields);
            return Collections.unmodifiableList(properties);
        }

        public List<SimpleActionBeanPathElement> getActions() {
            return Collections.unmodifiableList(actionsMethods);
        }
    }
}
