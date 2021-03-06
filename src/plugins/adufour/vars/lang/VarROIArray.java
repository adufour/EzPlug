package plugins.adufour.vars.lang;

import plugins.adufour.vars.gui.VarEditor;
import plugins.adufour.vars.util.VarListener;
import icy.roi.ROI;

public class VarROIArray extends VarArray<ROI>
{
    /**
     * Creates a new variable with an initial non-<code>null</code> array of ROI of size 0
     * 
     * @param name
     */
    public VarROIArray(String name)
    {
        this(name, null);
    }
    
    /**
     * Creates a new variable with an initial non-<code>null</code> array of ROI of size 0
     * 
     * @param name
     * @param defaultListener
     *            A listener to add to this variable immediately after creation
     */
    public VarROIArray(String name, VarListener<ROI[]> defaultListener)
    {
        super(name, ROI[].class, new ROI[0], defaultListener);
    }
    
    @Override
    public VarEditor<ROI[]> createVarEditor()
    {
        return super.createVarViewer();
    }
    
    /**
     * @return a pretty-printed text representation of the variable's local value (referenced
     *         variables are <b>not</b> followed). This text is used to display the value (e.g. in a
     *         graphical interface) or store the value into XML files. Overriding implementations
     *         should make sure that the result of this method is compatible with the
     *         {@link #parse(String)} method to ensure proper reloading from XML files.
     */
    public String getValueAsString()
    {
        ROI[] value = getValue();
        
        if (value == null || value.length == 0) return "No ROI";
        
        return value.length + " ROI";
    }
    
    @Override
    public ROI[] parse(String input)
    {
        return null;
    }
}
