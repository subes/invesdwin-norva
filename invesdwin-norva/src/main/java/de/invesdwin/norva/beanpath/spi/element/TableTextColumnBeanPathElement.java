package de.invesdwin.norva.beanpath.spi.element;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.norva.beanpath.spi.element.simple.SimplePropertyBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.simple.modifier.IBeanPathPropertyModifier;
import de.invesdwin.norva.beanpath.spi.element.simple.modifier.internal.IndexedBeanPathPropertyModifier;
import de.invesdwin.norva.beanpath.spi.element.simple.modifier.internal.NoObjectBeanPathPropertyModifier;
import de.invesdwin.norva.beanpath.spi.visitor.IBeanPathVisitor;

@NotThreadSafe
public class TableTextColumnBeanPathElement extends APropertyBeanPathElement implements ITableColumnBeanPathElement {

    private TableBeanPathElement tableElement;
    private NoObjectBeanPathPropertyModifier modifier;

    public TableTextColumnBeanPathElement(final SimplePropertyBeanPathElement simplePropertyElement) {
        super(simplePropertyElement);
    }

    @Override
    public TableBeanPathElement getTableElement() {
        return tableElement;
    }

    void setTableElement(final TableBeanPathElement tableElement) {
        org.assertj.core.api.Assertions.assertThat(this.tableElement).isNull();
        this.tableElement = tableElement;
    }

    @Override
    public IBeanPathPropertyModifier<Object> getModifier() {
        if (modifier == null) {
            modifier = new NoObjectBeanPathPropertyModifier(getAccessor());
        }
        return modifier;
    }

    public IBeanPathPropertyModifier<Object> getModifier(final int index) {
        return new IndexedBeanPathPropertyModifier(getTableElement().getChoiceModifier(), index, modifier);
    }

    @Override
    protected final void innerAccept(final IBeanPathVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean shouldAttachToContainerTitleElement() {
        return false;
    }

    /**
     * A column is visible if it is in the filtered columnOrder.
     */
    @Override
    public boolean isVisible() {
        return getTableElement().getColumns().contains(this);
    }

}