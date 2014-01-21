package plugins.adufour.vars.lang;

import plugins.adufour.vars.gui.VarEditor;
import plugins.adufour.vars.gui.swing.TextArea;
import plugins.adufour.vars.gui.swing.TextField;

public class VarString extends Var<String>
{
    private final int nbLines;
    
    /**
     * Creates a new text variable with a default text field editor
     * 
     * @param name
     * @param defaultValue
     */
    public VarString(String name, String defaultValue)
    {
        this(name, defaultValue, 1);
    }
    
    /**
     * Creates a new text variable with a default text field editor
     * 
     * @param name
     *            the name of this variable (*not* the actual text to store and/or display)
     * @param defaultValue
     *            the initial text to store and/or display
     * @param nbLines
     *            The number of lines (used only to create the graphical interface)
     */
    public VarString(String name, String defaultValue, int nbLines)
    {
        super(name, String.class, defaultValue);
        this.nbLines = nbLines;
    }
    
    @Override
    public String parse(String s)
    {
        return s;
    }
    
    @Override
    public VarEditor<String> createVarEditor()
    {
        if (getDefaultEditorModel() == null) return nbLines > 1 ? new TextArea<String>(this, nbLines) : new TextField<String>(this);
        
        return super.createVarEditor();
    }
}
