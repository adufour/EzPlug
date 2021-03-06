package plugins.adufour.ezplug;

import java.awt.Component;
import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;

import javax.swing.JOptionPane;

import icy.file.FileUtil;
import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginImageAnalysis;
import icy.plugin.interface_.PluginLibrary;
import icy.system.IcyHandledException;
import icy.type.value.IntegerValue;
import plugins.adufour.vars.util.VarException;

/**
 * Main component of the EzPlug framework. EzPlug provides numerous additional features to the
 * {@link Plugin} class to simplify the development of plug-ins for ICY. In a nut shell, it allows
 * to: a) design intuitive and homogeneous graphical interfaces; b) save/load parameters to/from
 * disk in a standardized way (e.g. XML).<br>
 * To create an EzPlug and benefit from these features, simply create a class that extends EzPlug
 * instead of {@link Plugin}. EzPlug is abstract and requires to implement the following methods:
 * <ul>
 * <li>The {@link #initialize()} method should be used to declare the parameters, buttons, groups
 * etc.. This is done via the {@link #addEzComponent(EzComponent) addEzComponent} method. EzPlug
 * here uses the Vars library to provide create tailored graphical components for all major data
 * types (e.g. check box for a boolean, spinner for a numerical parameter, combo box for list of
 * values etc.). See the {@link EzVar} class hierarchy for an overview.<br>
 * Several helper methods offer additional possibilities such as: 1) group variables together into a
 * panel which can be folded to save space on the interface; 2) automatically show or hide
 * parameters or groups thereof, depending on the value of other parameters (so-called visibility
 * triggers); 3) automatically respond to parameter input events for enhanced user experience; 4)
 * add traditional or custom AWT or Swing components to the interface (for advanced developers).
 * <li>The {@link #execute()} method holds the main execution code of the plug-in, and is called
 * when the "Run" button is clicked on the interface.
 * <li>The {@link #clean()} method should be used to clean "sensitive" resources (if any) created by
 * the plug-in (e.g. sequence painters, I/O streams, etc.), in order to free memory properly and/or
 * avoid polluting the display. This method is called when the interface (or Icy) is closed.
 * </ul>
 * The final generated interface comes with a sleek title bar and an action panel in the bottom,
 * from where the user may start or {@link EzStoppable stop} the execution process, load and save
 * parameters from/to disk via XML files.<br>
 * <br>
 * An example showing most of the features in action is available online
 * <a href="http://icy.bioimageanalysis.org/index.php?display=detailPlugin&pluginId=77">here</a>.
 * 
 * @see plugins.adufour.ezplug.EzInternalFrame
 * @author Alexandre Dufour
 */
@SuppressWarnings("deprecation")
public abstract class EzPlug extends Plugin implements PluginImageAnalysis, PluginLibrary, Runnable
{
    public static final String EZPLUG_MAINTAINERS = "Alexandre Dufour (adufour@pasteur.fr)";
    
    /**
     * This piece of code removes the Vars plug-in from Icy, since it has been merged into EzPlug
     * v.2.0
     */
    static
    {
        String path = new File("").getAbsolutePath() + FileUtil.separator + "plugins.adufour.vars".replace('.', FileUtil.separatorChar);
        if (FileUtil.exists(path))
        {
            try
            {
                FileUtil.delete(new File(path), true);
            }
            catch (Throwable t)
            {
            }
        }
    }
    
    /**
     * Number of active instances of this plugin
     */
    private static IntegerValue nbInstances = new IntegerValue(0);
    
    private EzGUI ezgui;
    
    private final HashMap<String, EzVar<?>> ezVars;
    
    private boolean timeTrial = false;
    
    private long startTime;
    
    private Thread executionThread = null;
    
    private final EzStatus status = new EzStatus();
    
    protected EzPlug()
    {
        ezVars = new HashMap<String, EzVar<?>>();
        
        synchronized (nbInstances)
        {
            nbInstances.setValue(nbInstances.getValue() + 1);
        }
    }
    
    protected void addComponent(Component component)
    {
        if (component == null)
        {
            // the component was not initialized inside the plug-in code
            throw new EzException(this, "null graphical component", false);
        }
        
        EzGUI g = getUI();
        if (g != null) g.addComponent(component);
    }
    
    /**
     * Adds a graphical component to the interface. Components are graphically ordered on the
     * interface panel in top-down fashion in the same order as they are added.
     * 
     * @param component
     *            the component to add
     * @see plugins.adufour.ezplug.EzVar
     * @see plugins.adufour.ezplug.EzButton
     * @see plugins.adufour.ezplug.EzGroup
     */
    protected void addEzComponent(EzComponent component)
    {
        if (component == null)
        {
            // the component was not initialized inside the plug-in code
            throw new EzException(this, "A plug-in variable was not initialized properly", false);
        }
        
        // if the component is a variable, register it
        if (component instanceof EzVar<?>) registerVariable((EzVar<?>) component);

        if (component instanceof EzPanel) registerVariables((EzPanel) component);
        
        EzGUI g = getUI();
        if (g != null) g.addEzComponent(component, true);
    }
    
    /**
     * Cleans user-defined structures when the plug-in window is closed. This method should be used
     * if this EzPlug has references to some structures which should be cleaned properly before
     * closing the plug window (e.g. painters on a sequence, I/O streams, etc.).<br>
     * NOTE: this method is called after cleaning of the graphical user interface. Hence, any
     * attempt to access graphical components (or change the value of EzVar objects) will result in
     * a null pointer exception.
     */
    public abstract void clean();
    
    /**
     * Clean method called whenever the user interface (or ICY) is closed, in order to clean
     * internal and user-defined structures properly.
     */
    void cleanFromUI()
    {
        // clean user-specific items
        try
        {
            clean();
        }
        catch (EzException eze)
        {
            // do not display anything as the interface is being closed
            if (!eze.catchException) throw eze;
        }
        
        // clear all variables
        
        ezVars.clear();
        
        ezgui = null;
        
        synchronized (nbInstances)
        {
            nbInstances.setValue(nbInstances.getValue() - 1);
        }
    }
    
    /**
     * Entry point of this EzPlug, which creates the user interface and displays it on the main
     * desktop pane. This method can be called either via ICY's main menu or directly via code.
     */
    @Override
    @Deprecated
    public void compute()
    {
        if (Icy.getMainInterface().isHeadLess())
        {
            // Special scenario: The plugin is running headless (from the command line)
            // => call the execute() method right away and exit
            execute();
            return;
        }
        
        try
        {
            // generate the user interface
            createUI();
            
            // show the interface to the user
            showUI();
        }
        catch (EzException e)
        {
            if (e.catchException) JOptionPane.showMessageDialog(ezgui.getFrame(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            else throw e;
        }
    }
    
    /**
     * Generates the user interface of this EzPlug. Note that the window is not shown on screen
     * (this can be done by calling the {@link #showUI()} method.
     */
    public void createUI()
    {
        // generate the main interface
        ezgui = new EzGUI(this);
        
        // incorporate user-defined parameters
        initialize();
        
        // pack the frame
        ezgui.repack(true);
        
        // fire listeners declared in the initialize method
        for (EzVar<?> var : ezVars.values())
            var.fireVariableChangedInternal();
            
        addIcyFrame(ezgui);
    }
    
    /**
     * Main method containing the core execution code of this EzPlug. This method is launched in a
     * separate thread only if it was called from the user interface (via the run button)
     */
    protected abstract void execute();
    
    /**
     * Generates an EzPlug code fragment that is ready to use and compile
     * 
     * @param className
     *            the name of the new class
     */
    public static String generateEzPlugCodeFragment(String className)
    {
        StringWriter sw = new StringWriter();
        
        sw.write("import " + EzPlug.class.getPackage().getName() + ".*;\n\n");
        sw.write("public class " + className + " extends " + EzPlug.class.getName() + "\n{\n");
        
        for (java.lang.reflect.Method m : EzPlug.class.getDeclaredMethods())
        {
            if (java.lang.reflect.Modifier.isAbstract(m.getModifiers()))
            {
                Class<?> returnType = m.getReturnType();
                sw.write("  public " + (returnType == null ? "void" : returnType.getName()) + " ");
                
                sw.write(m.getName() + "(");
                
                Class<?>[] params = m.getParameterTypes();
                if (params.length > 0)
                {
                    int cpt = 1;
                    
                    sw.write(params[0].getName() + " arg" + cpt++);
                    
                    for (int i = 1; i < params.length; i++)
                        sw.write(", " + params[i].getName() + " arg" + cpt++);
                }
                
                sw.write(")\n  {\n");
                sw.write("  // TODO: write your code here\n");
                sw.write("  }\n\n");
            }
        }
        
        sw.write('}');
        
        return sw.toString();
    }
    
    /**
     * Gets the name of this EzPlug (defaults to the class name).
     * 
     * @return the name of this EzPlug
     */
    public String getName()
    {
        return getDescriptor().getName();
    }
    
    /**
     * @return the number of active (not-destroyed) instances of this plug-in
     */
    public static int getNbInstances()
    {
        return nbInstances.getValue();
    }
    
    /**
     * Gets the starting execution time of this EzPlug (in nanoseconds). This method can be used in
     * conjunction with the {@link System#nanoTime()} method to measure elapsed time during the
     * execution process
     * 
     * @return the starting execution time in nanoseconds (obtained from the
     *         {@link System#nanoTime()} method)
     */
    public long getStartTime()
    {
        return startTime;
    }
    
    /**
     * Gets the graphical interface attached to this EzPlug. This interface gives access to specific
     * methods related to user interaction (e.g. progress bars, highlighting, etc.).
     * 
     * @return the graphical interface of the EzPlug, or null if the EzPlug has been created via
     *         code without generating the interface
     */
    public EzGUI getUI()
    {
        return ezgui;
    }
    
    protected EzStatus getStatus()
    {
        return status;
    }
    
    /**
     * Hides the user interface (without destroying it)
     */
    public void hideUI()
    {
        if (ezgui == null) return;
        
        ezgui.setVisible(false);
    }
    
    /**
     * @return true if the graphical user interface has not been initialized (e.g. when running
     *         without screen or on a server). If this method returns true, then the
     *         {@link #getUI()} will return null
     */
    public boolean isHeadLess()
    {
        return getUI() == null;
    }
    
    /**
     * This method lets the developer initialize the user interface of this EzPlug by adding
     * variables and other EzComponent objects via the {@link #addEzComponent(EzComponent)} method
     * 
     * @see plugins.adufour.ezplug.EzVar
     * @see plugins.adufour.ezplug.EzComponent
     */
    protected abstract void initialize();
    
    /**
     * Attempts to interrupt the execution of this plug-in, by calling {@link Thread#interrupt()} on
     * the execution thread. Note that this does not guarantee that the execution will indeed stop,
     * as it is up to the implementation of {@link #execute()} to regularly monitor the thread's
     * interrupted flag
     */
    public void stopExecution()
    {
        if (executionThread != null) executionThread.interrupt();
    }
    
    /**
     * Saves the EzPlug user parameters into the specified XML file
     * 
     * @param file
     * @see EzVarIO
     */
    public void loadParameters(File file)
    {
        try
        {
            EzVarIO.load(this, file, ezVars);
        }
        catch (EzException e)
        {
            if (!Icy.getMainInterface().isHeadLess())
            {
                JOptionPane.showMessageDialog(ezgui.getFrame(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                System.err.println("EzVarIO: " + e.getMessage());
            }
            
            if (!e.catchException) throw e;
        }
    }
    
    <T> void registerVariable(EzVar<T> var)
    {
        String varID = var.getID();
        
        if (ezVars.containsKey(varID)) throw new IllegalArgumentException("Variable " + varID + " already exists");
        
        ezVars.put(varID, var);
    }
    
    void registerVariables(EzPanel ezPanel)
    {
        for (EzComponent panelComponent : ezPanel)
        {
            if (panelComponent instanceof EzVar<?>)
            {
                registerVariable((EzVar<?>) panelComponent);
            }
            else if (panelComponent instanceof EzPanel)
            {
                // add recursively
                registerVariables((EzPanel) panelComponent);
            }
        }
    }
    
    @Override
    public synchronized void run()
    {
        executionThread = Thread.currentThread();
        
        try
        {
            try
            {
                startTime = System.nanoTime();
                
                status.setCompletion(Double.NaN);
                status.setMessage("Running");
                
                execute();
                
                if (timeTrial) System.out.println(getName() + " executed in " + (System.nanoTime() - startTime) / 1000000 + " ms");
            }
            catch (final VarException e)
            {
                if (isHeadLess()) throw e;
                
                String message = "Parameter: " + (e.source == null ? "(unknown)" : e.source.getName()) + "\n";
                message += "Message: " + e.getMessage();
                
                throw new EzException(this, message, true);
            }
        }
        catch (final EzException e)
        {
            if (e.catchException)
            {
                String message = "Plugin: " + (e.source == null ? "(unknown)" : e.source.getName()) + "\n";
                message += e.getMessage();
                
                throw new IcyHandledException(message);
            }
            
            throw e;
        }
        finally
        {
            status.done();
        }
    }
    
    /**
     * Saves the EzPlug user parameters into the specified XML file
     * 
     * @param file
     * @see EzVarIO
     */
    public void saveParameters(File file)
    {
        EzVarIO.save(this, ezVars, file);
    }
    
    /**
     * Displays the user interface on screen. If it didn't exist, the interface is first created
     */
    public void showUI()
    {
        if (ezgui == null) createUI();
        
        ezgui.setVisible(true);
        ezgui.toFront();
    }
    
    /**
     * Sets whether the execution time of this EzPlug should be displayed on the console
     * 
     * @param displayRunningTime
     */
    public void setTimeDisplay(boolean displayRunningTime)
    {
        this.timeTrial = displayRunningTime;
    }
}