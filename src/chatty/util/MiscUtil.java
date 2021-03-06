
package chatty.util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * General purpose static methods.
 * 
 * @author tduva
 */
public class MiscUtil {
    
    private static final Logger LOGGER = Logger.getLogger(MiscUtil.class.getName());

    /**
     * Copy the given text to the clipboard.
     * 
     * @param text 
     */
    public static void copyToClipboard(String text) {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents(new StringSelection(text), null);
    }
    
    public static boolean openFolder(File folder, Component parent) {
        try {
            Desktop.getDesktop().open(folder);
        } catch (Exception ex) {
            if (parent != null) {
                JOptionPane.showMessageDialog(parent, "Opening folder failed.\n"+ex.getLocalizedMessage());
            }
            return false;
        }
        return true;
    }
    
    /**
     * Parses the command line arguments from the main method into a Map.
     * Arguments that start with a dash "-" are interpreted as key, everything
     * after as value (until the next key or end of the arguments). This means
     * that argument values can contain spaces, but they can not contain an
     * argument starting with "-" (which would be interpreted as the next key).
     * If a key occurs more than once, the value of the last one is used.
     * 
     * Example:
     * -cd -channel test -channel zmaskm, sirstendec -connect
     * 
     * Returns the Map:
     * {cd="",
     *  channel="zmaskm, sirstendec",
     *  connect=""
     * }
     * 
     * @param args The commandline arguments from the main method
     * @return The map with argument keys and values
     */
    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> result = new HashMap<>();
        String key = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                // Put key in result, but also remember for next iteration
                key = arg.substring(1);
                // Overwrites possibly existing key, so only last one with this
                // name is saved
                result.put(key, "");
            } else if (key != null) {
                // Append current value (not a key) to last found key
                // Trim in case previous value was empty
                String newValue = (result.get(key)+" "+arg).trim();
                result.put(key, newValue);
            }
        }
        return result;
    }
    
    /**
     * Attempt to move the file atomically, and if that fails try regular file
     * replacing.
     * 
     * @param from The file to move
     * @param to The target filename, which will be overwritten if it already
     * exists
     * @throws java.io.IOException
     */
    public static void moveFile(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, ATOMIC_MOVE);
        } catch (IOException ex) {
            // Based on the Files.move() docs it may throw an IOException when
            // the target file already exists (implementation specific), so try
            // alternate move on that instead of AtomicMoveNotSupportedException
            LOGGER.info("ATOMIC_MOVE failed: "+ex);
            Files.move(from, to, REPLACE_EXISTING);
        }
    }
    
    /**
     * Delete all files and directories in the given directory.
     * 
     * The prefix can be used if all the files to delete share a common prefix,
     * to ensure that only the correct files are deleted.
     * 
     * If the deleteDir parameter is true, then the given directory itself is
     * also deleted, otherwise only subdirectories are deleted.
     * 
     * If a file or directory fails to be deleted (for directories this often
     * occurs because it is not empty), then the rest of the process will still
     * continue and no error is thrown.
     * 
     * @param dir The directory to act upon
     * @param prefix Files need to have this prefix to be deleted
     * @param deleteDir If true, delete the given directory itself as well
     * @return The number of files successfully deleted
     */
    public static int deleteInDir(File dir, String prefix, boolean deleteDir) {
        int count = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.isDirectory()) {
                        count += deleteInDir(file, prefix, true);
                    } else if (file.getName().startsWith(prefix)) {
                        if (files[i].delete()) {
                            count++;
                        }
                    }
                }
            }
        }
        if (deleteDir) {
            dir.delete();
        }
        return count;
    }
    
    /**
     * Returns the StackTrace of the given Throwable as a String.
     * 
     * @param e
     * @return 
     */
    public static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    public static final boolean OS_WINDOWS = checkOS("Windows");
    public static final boolean OS_LINUX = checkOS("Linux");
    public static final boolean OS_MAC = checkOS("Mac");
    
    private static boolean checkOS(String check) {
        String os = System.getProperty("os.name");
        return os.startsWith(check);
    }
    
    /**
     * Returns System.nanoTime() as milliseconds and can thus only be used to
     * compare two values to eachother to get elapsed time that is not dependent
     * on system clock time.
     * 
     * @return Some elapsed time in milliseconds
     */
    public static long ems() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
    
    public static boolean biton(int value, int i) {
        return (value & (1 << i)) != 0;
    }

    public static Image rotateImage(Image image) {
        BufferedImage bi;
        if (image instanceof BufferedImage) {
            bi = (BufferedImage)image;
        } else {
            bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
        AffineTransform tx;
        AffineTransformOp op;
        tx = AffineTransform.getScaleInstance(-1, -1);
        tx.translate(-image.getWidth(null), -image.getHeight(null));
        op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(bi, null);
    }
    
}
