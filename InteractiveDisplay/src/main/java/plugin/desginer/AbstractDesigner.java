package plugin.desginer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.Nullable;
import model.application.InteractiveDisplayApplicationModel;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;
import org.jhotdraw.app.Application;
import org.jhotdraw.app.View;
import org.jhotdraw.gui.JFileURIChooser;
import org.jhotdraw.gui.URIChooser;
import org.jhotdraw.gui.filechooser.ExtensionFileFilter;
import plugin.IPlugin;
import plugin.PluginRuntime;
import plugin.compile.CompilerUtils;

import javax.swing.*;

import static java.lang.System.out;

/**
 * AbstractDesigner provides runtime compilation.
 *
 * @author HongKee Moon
 * @version 0.1beta
 * @since 9/5/13
 */
public abstract class AbstractDesigner extends JFrame {
    protected InteractiveDisplayApplicationModel model;
    protected final String pluginType;
    protected final String processName;
    protected RSyntaxTextArea textArea;
    protected IPlugin plugin;

    protected AbstractDesigner(String pluginType, String processName) {
        this.pluginType = pluginType;
        this.processName = processName;
        initializeComponents();
    }

    public void setModel(InteractiveDisplayApplicationModel model)
    {
        this.model = model;
    }

    protected abstract void process();

    public void initializeComponents()
    {
        JPanel cp = new JPanel(new BorderLayout());

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton loadBtn = new JButton("Load");
        loadBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = getJavaFileChooser();

                int returnVal = chooser.showOpenDialog(getParent());
                if(returnVal == JFileChooser.APPROVE_OPTION)
                {
                    String filename = chooser.getSelectedFile().getAbsolutePath();

                    try {
                        FileInputStream fis = new FileInputStream(filename);
                        InputStreamReader in = new InputStreamReader(fis, "UTF-8");

                        textArea.read(in, null);

                        in.close();
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (UnsupportedEncodingException e1) {
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (IOException e1) {
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        });
        bp.add(loadBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = getJavaFileChooser();

                int returnVal = chooser.showSaveDialog(getParent());
                if(returnVal == JFileChooser.APPROVE_OPTION)
                {
                    String filename = chooser.getSelectedFile().getAbsolutePath();
                    if(!filename.endsWith(".java"))
                    {
                        filename += ".java";
                    }

                    try {
                        FileOutputStream fos = new FileOutputStream(filename);
                        OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");

                        textArea.write(out);

                        out.close();

                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (UnsupportedEncodingException e1) {
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (IOException e1) {
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        });
        bp.add(saveBtn);

        JButton compileBtn = new JButton("Compile");
        compileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                compile();
            }
        });
        bp.add(compileBtn);

        JButton processBtn = new JButton(processName);
        processBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                process();
            }
        });
        bp.add(processBtn);

        textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);
        sp.setFoldIndicatorEnabled(true);
        cp.add(bp, BorderLayout.NORTH);
        cp.add(sp, BorderLayout.CENTER);

        setContentPane(cp);
        setTitle(pluginType);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void compile() {
        StringWriter writer = new StringWriter();
        try {
            textArea.write(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        PluginRuntime runtime = new PluginRuntime();

        String code = writer.toString();

        String className = code.substring(code.indexOf("public class") + 13);
        className = className.substring(0, className.indexOf(" "));

        if(runtime.compile(className, writer.toString()))
        {
            try {
                plugin = runtime.instanciate(className, writer.toString());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            out.println("Compiled successfully.");
            out.println("Plugin name : " + plugin.getName());
            out.println("Plugin author : " + plugin.getAuthor());
            out.println("Plugin version : " + plugin.getVersion());
        }
    }

    private JFileChooser getJavaFileChooser() {
        JFileChooser c = new JFileChooser();
        ExtensionFileFilter defaultFilter = new ExtensionFileFilter("JavaFile","java");
        c.addChoosableFileFilter(defaultFilter);

        c.setFileFilter(defaultFilter);
        return c;
    }
}