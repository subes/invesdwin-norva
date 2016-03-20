package de.invesdwin.norva.beanpath.spi.element.utility;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.norva.beanpath.BeanPathStrings;
import de.invesdwin.norva.beanpath.annotation.BeanPathRedirect;
import de.invesdwin.norva.beanpath.spi.PathUtil;
import de.invesdwin.norva.beanpath.spi.element.AActionBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.IBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.simple.SimpleActionBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.simple.modifier.IBeanPathPropertyModifier;
import de.invesdwin.norva.beanpath.spi.visitor.IBeanPathVisitor;

@NotThreadSafe
public class ColumnOrderBeanPathElement extends AActionBeanPathElement implements IUtilityBeanPathElement {

    public static final String COLUMN_ORDER_BEAN_PATH_FRAGMENT = "columnOrder";
    private IBeanPathElement attachedToElement;
    private IBeanPathPropertyModifier<List<?>> columnOrderModifier;

    public ColumnOrderBeanPathElement(final SimpleActionBeanPathElement simpleActionElement) {
        super(simpleActionElement);
        org.assertj.core.api.Assertions.assertThat(getAccessor().getBeanPathFragment())
                .isEqualTo(COLUMN_ORDER_BEAN_PATH_FRAGMENT);
    }

    @Override
    protected BeanPathRedirect postProcessRedirect(final BeanPathRedirect annotation) {
        final BeanPathRedirect parent = super.postProcessRedirect(annotation);
        return new BeanPathRedirect() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return BeanPathRedirect.class;
            }

            @Override
            public String value() {
                return PathUtil.maybeAddUtilityFragment(parent.value(), COLUMN_ORDER_BEAN_PATH_FRAGMENT);
            }
        };
    }

    @Override
    public void setAttachedToElement(final IBeanPathElement attachedToElement) {
        org.assertj.core.api.Assertions.assertThat(this.attachedToElement).isNull();
        this.attachedToElement = attachedToElement;
    }

    @Override
    public boolean isAttachedToElement() {
        return attachedToElement != null;
    }

    @Override
    protected void innerAccept(final IBeanPathVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    private IBeanPathPropertyModifier<List<?>> getColumnOrderModifier() {
        if (columnOrderModifier == null) {
            columnOrderModifier = new de.invesdwin.norva.beanpath.spi.element.simple.modifier.internal.ChoiceBeanPathPropertyModifier(
                    new de.invesdwin.norva.beanpath.spi.element.simple.modifier.internal.ActionInvokerBeanObjectAccessor(
                            getInvoker()),
                    null);
        }
        return columnOrderModifier;
    }

    public List<String> getColumnOrder() {
        final List<String> columnOrder = new ArrayList<String>();
        for (final Object column : getColumnOrderModifier().getValue()) {
            columnOrder.add(BeanPathStrings.asString(column));
        }
        return columnOrder;
    }

}
