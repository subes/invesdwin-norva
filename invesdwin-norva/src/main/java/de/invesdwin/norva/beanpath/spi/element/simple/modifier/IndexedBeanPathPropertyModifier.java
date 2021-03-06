package de.invesdwin.norva.beanpath.spi.element.simple.modifier;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.norva.beanpath.impl.clazz.IBeanClassAccessor;
import de.invesdwin.norva.beanpath.impl.object.IBeanObjectAccessor;
import de.invesdwin.norva.beanpath.spi.IBeanPathAccessor;

@NotThreadSafe
public class IndexedBeanPathPropertyModifier implements IBeanPathPropertyModifier<Object> {

    private final IBeanPathPropertyModifier<List<?>> choiceModifier;
    private final int index;
    private final IBeanPathPropertyModifier<Object> delegate;

    public IndexedBeanPathPropertyModifier(final IBeanPathPropertyModifier<List<?>> choiceModifier, final int index,
            final IBeanPathPropertyModifier<Object> delegate) {
        this.choiceModifier = choiceModifier;
        this.index = index;
        this.delegate = delegate;
    }

    @Override
    public IBeanPathAccessor getAccessor() {
        return delegate.getAccessor();
    }

    @Override
    public IBeanClassAccessor getBeanClassAccessor() {
        return delegate.getBeanClassAccessor();
    }

    @Override
    public IBeanObjectAccessor getBeanObjectAccessor() {
        return delegate.getBeanObjectAccessor();
    }

    @Override
    public void setValue(final Object value) {
        final Object target = getIndexedTarget(choiceModifier.getValue());
        setValueFromTarget(target, value);
    }

    @Override
    public Object getValue() {
        final Object target = getIndexedTarget(choiceModifier.getValue());
        return getValueFromTarget(target);
    }

    @Override
    public void setValueFromRoot(final Object root, final Object value) {
        final Object target = getIndexedTarget(choiceModifier.getValueFromRoot(root));
        setValueFromTarget(target, value);
    }

    @Override
    public Object getValueFromRoot(final Object root) {
        final Object target = getIndexedTarget(choiceModifier.getValueFromRoot(root));
        return getValueFromTarget(target);
    }

    private Object getIndexedTarget(final List<?> value) {
        return value.get(index);
    }

    @Override
    public void setValueFromTarget(final Object target, final Object value) {
        delegate.setValueFromTarget(target, value);
    }

    @Override
    public Object getValueFromTarget(final Object target) {
        return delegate.getValueFromTarget(target);
    }

}
