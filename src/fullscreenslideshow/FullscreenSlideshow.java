/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fullscreenslideshow;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This is the application class where the UI will be created and managed.
 *
 * @author Christopher King
 */
public class FullscreenSlideshow {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
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

        }
    }

    /*
     * This method will check the folder to see if any new files have been
     * uploaded or any files have been changed. If there has been changes then
     * it will reload the directory, update "images", and return true. If not
     * then it will simply do nothing and return false;
     * 
     * Returning a boolean is really for future updates. It is not used at the moment.
     */
    private boolean checkUpdates() {
        //get images/files to check them
        File folder = new File(folderPath);
        File[] imageFiles = folder.listFiles(new OnlyImage());
        long tempTime = getLatestModified(imageFiles);
        //if stored modified timestamp is the same then we dont need to do anything
        if (newestModified >= tempTime) {
            return false;
        }
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
     * Allows app to force slideshow to repaint itself.
     */
    public void doRepaint() {
        this.repaint();
    }
    
    /*
     * This method advances the slideshow to the nextslide.
     * If the slideshow was on the last slide then it calls checkUpdates and
     * resets currentSlide to 0 (so that it loops).
     * 
     * 
     */
    public void nextSlide() {
        //check if we just displayed the last slide
        if(currentSlide >= images.length-1) {
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
            g.drawImage(images[currentSlide], 0, 0, null);
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
