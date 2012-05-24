/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fullscreenslideshow;

import java.awt.Component;
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
    

    /*
     * This is the constructor for the slideshow object. It should perform the
     * initial loading of the images and setting of the newestModified variable
     * to track changes to the images in the directory.
     *
     */
    public Slideshow(String folderPath) {
        //load folder
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
