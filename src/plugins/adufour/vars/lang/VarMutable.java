package plugins.adufour.vars.lang;

import java.lang.reflect.Array;
import java.util.ArrayList;

import plugins.adufour.vars.util.MutableType;
import plugins.adufour.vars.util.TypeChangeListener;

/**
 * Variable holding a transient value (i.e. its type may during during runtime)
 * 
 * @author Alexandre Dufour
 * 
 */
@SuppressWarnings("rawtypes")
public class VarMutable extends Var implements MutableType
{
    private final ArrayList<TypeChangeListener> listeners = new ArrayList<TypeChangeListener>();
    
    /**
     * Constructs a new mutable variable with specified name and type. The default value for mutable
     * variables is always null, in order to be reset properly
     * 
     * @param name
     * @param initialType
     *            the type of variable to handle (this can be changed via the
     *            {@link #setType(Class)} method
     */
    @SuppressWarnings("unchecked")
    public VarMutable(String name, Class<?> initialType)
    {
        super(name, initialType, null);
    }
    
    public void addTypeChangeListener(TypeChangeListener listener)
    {
        listeners.add(listener);
    }
    
    public void removeTypeChangeListener(TypeChangeListener listener)
    {
        listeners.remove(listener);
    }
    
    @Override
    public String getValueAsString()
    {
        Object value = getValue();
        
        if (value == null) return "null";
        
        if (value.getClass().isArray())
        {
            int length = Array.getLength(value);
            
            if (length == 0) return "";
            
            StringBuilder sb = new StringBuilder();
            
            sb.append(Array.get(value, 0).toString());
            for (int i = 1; i < length; i++)
                sb.append(" " + Array.get(value, i).toString());
            
            return sb.toString();
        }
        
        return value.toString();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean isAssignableFrom(Var source)
    {
        Class<?> sourceType = source.getType();
        
        return sourceType != null && (this.getType() == null || this.getType().isAssignableFrom(sourceType));
    }
    
    @SuppressWarnings("unchecked")
    public void setType(Class<?> newType)
    {
        Class<?> oldType = this.getType();
        
        if (oldType == newType) return;
        
        setValue(null);
        
        this.type = newType;
        
        for (TypeChangeListener listener : listeners)
            listener.typeChanged(this, oldType, newType);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void setValue(Object newValue)
    {
        // TODO in case of type mismatch: change type or throw exception ?
        super.setValue(newValue);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void setReference(Var variable) throws ClassCastException
    {
        if (variable != null) setType(variable.getType());
        super.setReference(variable);
    }
}