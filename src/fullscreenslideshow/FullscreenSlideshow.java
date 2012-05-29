package fullscreenslideshow;

/*
 * This program loads images from specified folders into fullscreen slideshows
 * on specfified monitors. It dynamically reloads the sets of images so that a
 * user can simply add or remove images from the folder to change the currently
 * playing slideshow.
 *
 * The program accepts command line arguements in the following style: <int for
 * monitor number> "quotes enclosed string for folder path with double slashes"
 * The monitors do not need to be in any particular order they simply need to
 * follow the pattern of "monitor number" "folder path"
 *
 * Ex. FullscreenSlideshow.jar 1 "C:\\test" This will load images from C:\test
 * directory onto monitor 1. Ex. FullscreenSlideshow.jar 1 "C:\\test" 0
 * "C:\\test2" This will load images from C:\test onto monitor 1 and images from
 * C:\test2 onto monitor 0. TODO:
 *
 * What to do if the program detects no images needs to be handled. What if
 * someone deletes all the images so that they can load new ones. Program should
 * be able to handle this and simply wait for more images instead of dying.
 */
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

/**
 * This is the application class where the UI will be created and managed.
 *
 * @author Christopher King
 */
public class FullscreenSlideshow extends JFrame {

    private String path;
    private Slideshow x;
    private GraphicsConfiguration gc;
    private RunSlides thread;

    public FullscreenSlideshow(String path, GraphicsConfiguration gc) {
        super(gc);
        this.gc = gc;
        this.path = path;
    }

    public class RunSlides extends Thread {

        private volatile Thread flag;

        @Override
        public void start() {
            flag = new Thread(this);
            flag.start();
        }

        public void quit() {
            Thread tmpFlag = flag;
            flag = null;
            if (tmpFlag != null) {
                tmpFlag.interrupt();
            }
        }

        @Override
        public void run() {
            Thread thisThread = Thread.currentThread();
            while (flag == thisThread) {
                try {
                    RunSlides.sleep(5000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Interrupted", ex);
                }
                x.nextSlide();
            }

        }
    }

    /*
     * Starts the slideshow running
     */
    public void startSlides() throws InterruptedException {
        thread = new RunSlides();
        thread.start();
    }

    /*
     * This is a simple method that just gets rid of the graphics junk after the
     * program is finished so that the main thread will end. It is wrapped
     * because there is no other way to access it in the windowclose event
     * handler.
     */
    private void endProgram() {
        this.dispose();
    }

    public void buildUI() {
        x = new Slideshow(path);
        x.setHW(gc.getDevice().getDisplayMode().getHeight(), gc.getDevice().getDisplayMode().getWidth());
        this.add(x);

        this.setExtendedState(FullscreenSlideshow.MAXIMIZED_BOTH);
        this.setUndecorated(true);
        this.setVisible(true);
        x.repaint();
        try {
            startSlides();
        } catch (InterruptedException ex) {
            throw new RuntimeException("Interrupted", ex);
        }
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                endProgram();
                thread.quit();
            }
        });

    }

    public boolean isFinished() {

        if (thread != null && thread.isAlive()) {
            return false;
        }

        return true;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        //Get the graphics enironment and devices to allow windows to be created on multiple monitors
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        boolean[] use = new boolean[gds.length];
        FullscreenSlideshow[] shows;
        String[] paths;
        
        //if args has something in it and its less than or equal to the number of devices
        if (args.length > 0 && args.length <= gds.length * 2) {
            for (int i = 0; i < args.length; i += 2) {
                use[Integer.parseInt(args[i])] = true;
            }
        }

        //if no args then defult to use every device and default path
        if (args.length == 0) {
            paths = new String[use.length];
            for (int i = 0; i < use.length; i++) {
                use[i] = true;
                paths[i] = "Z:\\test";
            }
            shows = new FullscreenSlideshow[use.length];
        } //otherwise there were arguments so assume they are in correct format
        else {
            //count the true's so that we dont waste space on unused Slideshows
            int count = 0;
            for (int i = 0; i < use.length; i++) {
                if (use[i]) {
                    count++;
                }
            }
            //creating array to hold paths entered in as command line arguments
            paths = new String[count];
            /*
             * This for loops loads paths to correspond with the use array and
             * therefor the shows array. However the paths in the command line
             * args are everyother argument so temp counts from 1,3,5,...
             */
            int temp = 1;
            for (int i = 0; i < paths.length; i++) {
                paths[i] = args[temp];
                temp += 2;
            }
            shows = new FullscreenSlideshow[count];
        }

        //create the slideshows using the given data/paths/devices
        int j = 0;
        for (int i = 0; i < gds.length; i++) {
            if (use[i]) {
                shows[j] = new FullscreenSlideshow(paths[j], gds[i].getDefaultConfiguration());
                shows[j].buildUI();
                j++;
            }
        }

    }
}

/**
 * This is the background class where the actual slideshow will be implemented
 * This will contain the methods that draw and load the images.
 *
 * @author Christopher King
 */
class Slideshow extends Component {

    private boolean imagesLoaded;
    private int currentSlide;
    private String folderPath;
    private int h; //height of display
    private int w; //width of display
    private File[] files;
    private FontMetrics fm;
    private final Font F = new Font("Monospaced", Font.BOLD, 100);

    /*
     * This is the constructor for the slideshow object. It should perform the
     * initial loading of the images and setting of the newestModified variable
     * to track changes to the images in the directory.
     *
     */
    public Slideshow(String path) {
        //load folder
        folderPath = path;
        File folder = new File(folderPath);
        files = folder.listFiles(new OnlyImage()); //load array with all image files found in folder

        //if the folder is empty
        if (files.length == 0) {
            imagesLoaded = false;
        } else {
            imagesLoaded = true;
        }
        currentSlide = 0;
        h = 0;
        w = 0;
    }

    /*
     * This method reloads the directory containing the images. If no images are
     * found then it sets imagesLoaded to false (so we dont try to paint them)
     * and tells the program that there are 0 slides. Since loading a list of
     * files is easy (in terms of computing time) we can just do this everytime
     * without regard for how many slides there might be.
     *
     */
    private void checkUpdates() {
        //get images/files to check them
        File folder = new File(folderPath);
        files = folder.listFiles(new OnlyImage());

        if (files.length == 0) {
            imagesLoaded = false;
        } else {
            imagesLoaded = true;
        }
        this.repaint();
    }

    /*
     * Allows app to get current slide. Not really used at the moment.
     */
    public int getCurrentSlide() {
        return currentSlide;
    }

    /*
     * This method is here to allow the app class to tell us the size of its
     * window This allows the image to be scaled to the full size easily.
     */
    public void setHW(int height, int width) {
        this.h = height;
        this.w = width;
    }

    /*
     * This method advances the slideshow to the nextslide. If the slideshow was
     * on the last slide then it calls checkUpdates and resets currentSlide to 0
     * (so that it loops).
     *
     *
     */
    public void nextSlide() {
        //check if we just displayed the last slide
        if (currentSlide >= files.length - 1) {
            checkUpdates();
            currentSlide = 0;
            this.repaint();
        } else {
            currentSlide++;
            this.repaint();
        }

    }
    
    

    @Override
    public void paint(Graphics g) {
        if (fm == null) {
            fm = g.getFontMetrics(F);
            
        }

        if (imagesLoaded) {
            try {
                g.drawImage(ImageIO.read(files[currentSlide]).getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
                String temp = Integer.toString(currentSlide);
                g.setFont(F);
                g.drawString(temp, (w / 2 - fm.stringWidth(temp) / 2), h / 2);
            } catch (IOException ex) {
                //if an IOException is thrown it probably means the file has been deleted
                //so just update the slides and start over
                this.checkUpdates();
            }
        } else {
            g.setFont(F);
            g.drawString("Loading Images...", (w / 2 - fm.stringWidth("Loading Images...") / 2), h / 2);
        }
    }

    /*
     * Subclass that implements a filter to only get images from the folder.
     */
    private class OnlyImage implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return (name.endsWith(".jpg")
                    || name.endsWith(".jpeg")
                    || name.endsWith(".JPG")
                    || name.endsWith(".JPEG")
                    || name.endsWith(".gif")
                    || name.endsWith(".GIF")
                    || name.endsWith(".png")
                    || name.endsWith(".PNG"));
        }
    }
}
