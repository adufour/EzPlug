package plugins.adufour.ezplug;

import icy.main.Icy;
import icy.sequence.Sequence;
import plugins.adufour.vars.gui.swing.SequenceChooser;
import plugins.adufour.vars.lang.VarSequence;

/**
 * Class defining a sequence type variable which displays as a combo box where the user may choose
 * among all open sequences
 * 
 * @author Alexandre Dufour
 */
public class EzVarSequence extends EzVar<Sequence>
{
    /**
     * Constructs a new variable that handles image data. <br>
     * 
     * @param varName
     *            the parameter name
     */
    public EzVarSequence(String varName)
    {
        super(new VarSequence(varName, null), null);
        Sequence s = Icy.getMainInterface().getActiveSequence();
        if (s != null) setValue(s);
    }
    
    /**
     * Force the sequence chooser to the "no sequence" selection
     */
    public void setNoSequenceSelection()
    {
        // set the variable to null
        // NB: this line is in principle not necessary as the variable listens to the combo box
        // but if the listener is not yet activated, the variable won't change
        getVariable().setValue(null);
        
        // set the active combo box item to "no sequence"
        ((SequenceChooser) getVarEditor()).getEditorComponent().setSelectedIndex(0);
    }
    
    public void setValue(final Sequence value) throws UnsupportedOperationException
    {
        // 1) adjust the graphical interface (will also change the variable value)
        ((SequenceChooser) getVarEditor()).getEditorComponent().setSelectedItem(value);
        // 2) if value is not in the list of opened sequences, the variable will be unchanged
        // => set it a second time here
        getVariable().setValue(value);
        // (note: listeners will not be fired twice if the first line worked)
    }
    
    @Override
    public VarSequence getVariable()
    {
        return (VarSequence) super.getVariable();
    }
}
