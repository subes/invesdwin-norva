package de.invesdwin.norva.beanpath.spi.element.simple;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.norva.beanpath.annotation.ModalCloser;
import de.invesdwin.norva.beanpath.annotation.ModalOpener;
import de.invesdwin.norva.beanpath.impl.clazz.BeanClassAccessor;
import de.invesdwin.norva.beanpath.impl.object.BeanObjectAccessor;
import de.invesdwin.norva.beanpath.spi.IBeanPathAccessor;
import de.invesdwin.norva.beanpath.spi.IBeanPathContainer;
import de.invesdwin.norva.beanpath.spi.context.ABeanPathContext;
import de.invesdwin.norva.beanpath.spi.element.ABeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.IActionBeanPathElement;
import de.invesdwin.norva.beanpath.spi.element.simple.invoker.BeanPathActionInvoker;
import de.invesdwin.norva.beanpath.spi.element.simple.invoker.IBeanPathActionInvoker;
import de.invesdwin.norva.beanpath.spi.visitor.IBeanPathVisitor;

@NotThreadSafe
public class SimpleActionBeanPathElement extends ABeanPathElement implements IActionBeanPathElement {

    private IBeanPathActionInvoker invoker;

    public SimpleActionBeanPathElement(final ABeanPathContext context, final IBeanPathContainer container,
            final IBeanPathAccessor accessor) {
        super(context, container, accessor);
    }

    @Override
    public IBeanPathActionInvoker getInvoker() {
        if (invoker == null) {
            invoker = new BeanPathActionInvoker(getAccessor());
        }
        return invoker;
    }

    @Override
    public boolean isInvokerAvailable() {
        return getAccessor() instanceof BeanObjectAccessor || getAccessor() instanceof BeanClassAccessor;
    }

    @Override
    public boolean isProperty() {
        return false;
    }

    @Override
    public boolean isAction() {
        return true;
    }

    @Override
    protected final void innerAccept(final IBeanPathVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean shouldBeAddedToElementRegistry() {
        return false;
    }

    @Override
    public boolean isModalCloser() {
        if (getAccessor().getAnnotation(ModalCloser.class) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isModalOpener() {
        if (getAccessor().getAnnotation(ModalOpener.class) != null) {
            return true;
        }
        return false;
    }
}
