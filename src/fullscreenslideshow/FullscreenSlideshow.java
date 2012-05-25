/*
 * This program loads images from specified folders into fullscreen slideshows
 * on specfified monitors. It dynamically reloads the sets of images so that a
 * user can simply add or remove images from the folder to change the currently
 * playing slideshow.
 * 
 * The program accepts command line arguements in the following style:
 * <int for monitor number> "quotes enclosed string for folder path with double
 * slashes"
 * The monitors do not need to be in any particular order they simply need to
 * follow the pattern of "monitor number" "folder path"
 * 
 * Ex. FullscreenSlideshow.jar 1 "C:\\test"
 * This will load images from C:\test directory onto monitor 1.
 * Ex. FullscreenSlideshow.jar 1 "C:\\test" 0 "C:\\test2"
 * This will load images from C:\test onto monitor 1 and images from C:\test2
 * onto monitor 0.
 * 
 * 
 */
/*
 * TODO: 
 * Fix memory issue. Currently the program loads all images in the directory
 * into memory. This is fine until the directory has 400 full HD images. This
 * begins to take up a lot of unnecessary memory. Need to implement some kind of
 * buffer when the number of images gets too high.
 * package fullscreenslideshow;
 * 
 * Another issue is the reloading of the images after every full rotation of the
 * slideshow. If the slideshow is too long then it will take too long to update
 * and if it is too short then there is a lot of unnecessary file access. There
 * should be a way to manually trigger an update immedietally.
 */

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
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
    public Slideshow x;
    private GraphicsConfiguration gc;

    public FullscreenSlideshow(String path, GraphicsConfiguration gc) {
        super(gc);
        this.gc = gc;
        this.path = path;
    }

    public void init() {
        buildUI();
    }

    public void buildUI() {
        x = new Slideshow(path);
        x.setHW(gc.getDevice().getDisplayMode().getHeight(), gc.getDevice().getDisplayMode().getWidth());
        this.add(x);
        this.setExtendedState(FullscreenSlideshow.MAXIMIZED_BOTH);
        this.setUndecorated(true);
        this.setVisible(true);
        x.repaint();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        String path = "Z:\\test";
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
        } else {
            //count the true's so that we dont waste space on unused Slideshows
            int count = 0;
            for (int i = 0; i < use.length; i++) {
                if (use[i]) {
                    count++;
                }
            }
            paths = new String[count];
            int temp = 1;
            for (int i = 0; i < paths.length; i++) {
                paths[i] = args[temp];
                temp += 2;
            }
            shows = new FullscreenSlideshow[count];
        }

        int j = 0;
        for (int i = 0; i < gds.length; i++) {
            if (use[i]) {
                shows[j] = new FullscreenSlideshow(paths[j], gds[i].getDefaultConfiguration());
                shows[j].addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                });
                shows[j].buildUI();
                j++;
            }
        }
        //FullscreenSlideshow s = new FullscreenSlideshow(path);
        while (true) {
            Thread.sleep(5000);
            for (int i = 0; i < shows.length; i++) {
                shows[i].x.nextSlide();
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

    private long newestModified;
    private BufferedImage[] images;
    private boolean imagesLoaded;
    private int currentSlide;
    private String folderPath;
    private int h;
    private int w;

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
        File[] imageFiles = folder.listFiles(new OnlyImage()); //load array with all image files found in folder

        //if the folder is empty
        if (imageFiles == null) {
            System.out.println("The folder was empty or did not contain images.");
            System.exit(0);
        } //otherwise it has something in it
        else {
            //create an array to hold the images
            images = new BufferedImage[imageFiles.length];
            //populate image array with new images from file list in files array
            for (int i = 0; i < imageFiles.length; i++) {
                try {
                    images[i] = ImageIO.read(imageFiles[i]); //create BufferedImages
                } catch (IOException e) {
                    System.err.println("Caught IOException: " + e.getMessage());
                    System.exit(1);
                }
            }
            newestModified = getLatestModified(imageFiles); //set modified time to be checked on next update
            imagesLoaded = true;
            currentSlide = 0;
            h = 0;
            w = 0;

        }
    }

    /*
     * This method will check the folder to see if any new files have been
     * uploaded or any files have been changed. If there has been changes then
     * it will reload the directory, update "images", and return true. If not
     * then it will simply do nothing and return false;
     *
     * Returning a boolean is really for future updates. It is not used at the
     * moment.
     */
    private boolean checkUpdates() {
        //get images/files to check them
        File folder = new File(folderPath);
        File[] imageFiles = folder.listFiles(new OnlyImage());
        long tempTime = getLatestModified(imageFiles);
        //if stored modified timestamp is the same then we dont need to do anything
        /*
        if (newestModified >= tempTime) {
            return false;
        }*/
        //otherwise the folder has been updated so reload images and reset modified time
        newestModified = tempTime;
        imagesLoaded = false;
        //flush old bufferedImages
        for (int i = 0; i < images.length; i++) {
            images[i].flush();
        }

        images = new BufferedImage[imageFiles.length]; //new array
        //get new images
        for (int i = 0; i < imageFiles.length; i++) {
            try {
                images[i] = ImageIO.read(imageFiles[i]);
            } catch (IOException e) {
                System.err.println("Caught IOException: " + e.getMessage());
                System.exit(1);
            }
        }
        imagesLoaded = true;

        return true;
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
        if (currentSlide >= images.length - 1) {
            checkUpdates();
            currentSlide = 0;
            this.repaint();
            return;
        }
        currentSlide++;
        this.repaint();
    }

    @Override
    public void paint(Graphics g) {
        if (imagesLoaded) {
            g.drawImage(images[currentSlide].getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        }
    }
    /*
     * This method returns the latest modified timestamp of the files in the
     * given array of files.
     */

    private static long getLatestModified(File[] files) {
        long longTime = 0L;
        if (files == null) {
            return 0L;
        } else {
            //find the most recently modified file time
            for (int i = 0; i < files.length; i++) {
                if (files[i].lastModified() > longTime) {
                    longTime = files[i].lastModified();
                }
            }
        }

        return longTime;
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
