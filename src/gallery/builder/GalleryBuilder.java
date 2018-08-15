/** \file
 * 
 * Aug 14, 2018
 *
 * Copyright Ian Kaplan 2018
 *
 * @author Ian Kaplan, www.bearcave.com, iank@bearcave.com
 */
package gallery.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * <h4>
 * GalleryBuilder
 * </h4>
 * <p>
 * Build Galleria (galleria.io) HTML from an old Gallery (http://galleryproject.org/) directory.  This code only works on the
 * old PHP Gallery format. This code will not work on newer galleries that use a database, like mySQL.
 * </p>
 * <p>
 * This code allows static web pages to be built from existing Gallery photo albums. The code extracts captions from the 
 * Gallery meta data (the photo.dat files). The captions are matched with the associated image files and entries for
 * Galleria static photo galleries are created.
 * </p>
 * <p>
 * I wrote this code when my web host stopped supporting PHP, at least in my directories, breaking Gallery and destroying the
 * ability of my web site to display my photo galleries.
 * </p>
 * <p>
 * Aug 14, 2018
 * </p>
 * 
 * @author Ian Kaplan, iank@bearcave.com
 */
public class GalleryBuilder {
    
    private static final class PhotosDotDatFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            boolean isPhotosDotDatFile = name.startsWith("photos.dat");
            return isPhotosDotDatFile;
        }
        
    } // class PhotosDotDatFilter
    
    
    private static final class ImgFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            boolean isImgFile = name.endsWith(".jpg") ||
                                name.endsWith("jpeg") ||
                                name.endsWith("png");
            return isImgFile;
        }
        
    }
    
    protected void usage() {
        System.out.println("usage: " + this.getClass().getName() + " [path to gallery directory]" );
    }
    
    private final static int PHOTOS_PER_PAGE = 25;
    private final static String GALLERY_ROOT_NAME = "gallery"; 
    
    String getSectionValue(final String metaData, final String sectionName ) {
        String quotedSectionName = '"' + sectionName + '"';
        int nameIx = metaData.indexOf( quotedSectionName);
        int openQuoteIx = metaData.indexOf('"', nameIx+quotedSectionName.length());
        int closeQuoteIx = metaData.indexOf('"', openQuoteIx+1);
        String value = metaData.substring(openQuoteIx, closeQuoteIx + 1);
        if (value.length() > 0) {
            value = value.substring(1, value.length()-1);
        }
        return value;
    }

    /**
     * <p>
     * Parse the metadata to rebuild the image file name. The sections that are parsed are:
     * </p>
     * <pre>
     * "name";s:8:"IMG_0020"
     * </pre>
     * <pre>
     * type";s:3:"jpg"
     * </pre>
     * @param metaDataSec
     * @return
     */
    protected String getImageFileName(final String metaDataSec) {
        String rootName = getSectionValue(metaDataSec, "name");
        String suffix = getSectionValue(metaDataSec, "type");
        String fileName = rootName + '.' + suffix;
        return fileName;
    }
    
    
    /**
     * <pre>
     * "caption";s:82:"The studio apartment I rented on Emili Vendrell (Joaquim Costa and Peu de la Creu)"
     * </pre>
     * @param metaData
     * @return
     */
    protected String getCaption(final String metaData ) {
        String caption = "";
        String captionTag = "\"caption\"";
        int capIx = metaData.indexOf(captionTag);
        if (capIx > 0) {
            String substr = metaData.substring(capIx + captionTag.length());
            String[] parts = substr.split(":");
            if (parts.length >= 3) {
                // Split into pieces separated by ":".  Note that there will be parts that go beyond the caption.
                // The second part is the length, the third part contains the caption, in quotes
                int capLen = Integer.parseInt( parts[1]);
                if (parts[2].length() > capLen) {
                    caption = parts[2].substring(1, capLen+1);
                }
            }

        }
        return caption;
    }
    
    /**
     * <p>
     * Split a string on the basis of a substring.
     * </P> 
     * <p>
     * I've been unable to find this function in the Java ecosystem, which is sort of odd. So
     * I wrote my own.
     * </p>
     * @param str
     * @param seperator
     * @return
     */
    protected String[] stringSplit(final String str, final String seperator ) {
        ArrayList<String> splitRslt = new ArrayList<String>();
        int sepSize = seperator.length();
        int startIx = str.indexOf(seperator);
        int endIx = 0;
        while (startIx > 0 && endIx < str.length()) {
            endIx = str.indexOf(seperator, startIx + sepSize);
            if (endIx < 0) {
                endIx = str.length();
            }
            String substr = str.substring(startIx, endIx);
            splitRslt.add(substr);
            startIx = endIx;
        }
        String[] splitVec = splitRslt.toArray( new String[1] );
        return splitVec;
    }

    /**
     * Build a map of name/value pairs consisting of the file name and the caption associated with the file.
     * @param photoDatFile
     * @param captionMap
     */
    protected void getCaptionData(File photoDatFile, HashMap<String, String> captionMap) {
        final String ENTRY_START_CAPITALIZED = "AlbumItem";
        final String ENTRY_START = "albumitem";
        FileInputStream istream = null;
        try {
            istream = new FileInputStream( photoDatFile );
            String metaData = IOUtils.toString(istream, Charset.defaultCharset().name());
            if (metaData.length() > 0) {
                // Convert the start marker to lower case. We don't want to convert the entire string to lower case since
                // it will mess up the caption.
                metaData = metaData.replaceAll(ENTRY_START_CAPITALIZED, ENTRY_START);
                String[] metaDataStrs = stringSplit( metaData, ENTRY_START);
                for (String metaDataSec : metaDataStrs) {
                    if (metaDataSec != null && metaDataSec.length() > 0) {
                        String imageFileName = getImageFileName( metaDataSec );
                        String caption = getCaption( metaDataSec );
                        if (caption.length() > 0) {
                            captionMap.put(imageFileName, caption);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error creating input stream for " + photoDatFile.getPath() + ": " + e.getLocalizedMessage() );
        } catch (IOException e) {
            System.out.println("Error reading file " + photoDatFile.getPath() + ": " + e.getLocalizedMessage() );
        } finally {
            if (istream != null) {
                try { istream.close(); } catch (IOException e) {}
            }
        }
        
    }

    

    /**
     * <p>
     * In the old PHP (pre-mySQL) Gallery the captions are stored in photo.dat files. These are
     * photos.dat, photos.dat.0, photos.dat.1 ... photos.dat.n
     * </p>
     * <p>
     * The photos.dat information is encoded in a run-lenght format. An example is shown below. The important part,
     * for this code, is that the section starts with "AlbumItem". This is followed by "name" which has the root
     * file name and "type" which has the image type. Following this is "caption" which has the caption string.
     * </p>
     * <p>
     * This code returns a hash map of the file name and the caption string from all of the photos.dat files.
     * </p>
     * <pre>
     * "AlbumItem":19:{s:5:"image";O:5:
     * "Image":12:{s:4:"name";s:8:"IMG_0020";s:4:
     * "type";s:3:"jpg";s:5:
     * "width";i:640;s:6:"height";i:480;s:11:
     * "resizedName";s:14:"IMG_0020.sized";s:7:
     * "thumb_x";N;s:7:"thumb_y";N;s:11:"thumb_width";N;s:12:"thumb_height";N;s:9:
     * "raw_width";i:2272;s:10:"raw_height";i:1704;s:7:"version";i:33;}s:9:
     * "thumbnail";O:5:"Image":12:{s:4:"name";s:14:"IMG_0020.thumb";s:4:"type";s:3:"jpg";s:5:
     * "width";i:150;s:6:"height";i:113;s:11:"resizedName";N;s:7:"thumb_x";N;s:7:"thumb_y";N;s:11:
     * "thumb_width";N;s:12:"thumb_height";N;s:9:"raw_width";i:150;s:10:"raw_height";i:113;s:7:"version";i:33;}s:7:
     * "preview";N;s:7:"caption";s:82:"The studio apartment I rented on Emili Vendrell (Joaquim Costa and Peu de la Creu)";s:6:
     * "hidden";N;s:9:"highlight";b:0;s:14:
     * "highlightImage";N;s:11:"isAlbumName";N;s:6:"clicks";i:15654;s:8:"keywords";N;s:8:
     * "comments";N;s:10:"uploadDate";i:1113756514;s:15:"itemCaptureDate";i:1113756512;s:8:
     * "exifData";N;s:5:"owner";s:21:"1109138339_1755202938";s:11:"extraFields";a:0:{}s:4:"rank";N;s:7:"version";i:33;s:7:"emailMe";N;}
     * </pre>
     * 
     * @param filePath
     * @return
     */
    protected HashMap<String, String> getCaptions(File dir) {
        HashMap<String, String> captionMap = new HashMap<String, String>();
        File[] metaDataVec = dir.listFiles( new PhotosDotDatFilter() );
        if (metaDataVec.length > 0) {
            for (File photoDatFile : metaDataVec) {
                getCaptionData(photoDatFile, captionMap );
            }
        } else {
            System.out.println("No photos.dat files found in path " + dir.getPath() );
        }
        return captionMap;
    }
    
    /**
     * <p>
     * Return a set of pairs containing the image file (e.g., IMG_0123.jpg) and the thumbnail, as a pair
     * with the image on the left branch and the thumnnail on the right branch.
     * </p>
     * @param dir
     * @return
     */
    protected ArrayList<Pair<String, String>> getImageList( File dir ) {
        ArrayList<Pair<String, String>> imageList = new ArrayList<Pair<String, String>>();
        String[] fileNames = dir.list( new ImgFilter() );
        Arrays.sort(fileNames);
        ArrayList<String> temp = new ArrayList<String>();
        // weed out image files with "sized" or "highlight" in their name
        for (String fileName : fileNames) {
           if ((fileName.toLowerCase().indexOf("sized") < 0) && (fileName.toLowerCase().indexOf("highlight") < 0)) {
               temp.add(fileName);
           }
        }
        // build the list of image, thumbnail pairs
        int ix = 0;
        while (ix < temp.size()) {
            String left = temp.get(ix);
            ix++;
            if (ix < temp.size()) {
                String right = temp.get(ix);
                ix++;
                if (right.toLowerCase().indexOf("thumb") > 0) {
                    Pair<String, String> pair = new ImmutablePair<String, String>(left, right);
                    imageList.add(pair);
                } else {
                    System.out.println("Could not find thumbnail file for " + left );
                }
            }
        }
        return imageList;
    }  // getImageList
    
    
    protected PrintStream getNewFile(File directory, int pageCnt ) throws IOException {
        PrintStream pstream = null;
        String fileName = String.format("%s_%02d", GALLERY_ROOT_NAME, pageCnt );
        String newPath = directory.getAbsolutePath() + File.separator + fileName;
        File newFile = new File( newPath );
        FileOutputStream ostream = new FileOutputStream( newFile );
        pstream = new PrintStream( ostream );
        return pstream;
    }

    /**
     * <p>
     * Build a set of Gallery files containing HTML for Galleria (galleria.io) folio pages
     * </p>
     * 
     * <pre>
     * &lt;img data-big="IMG_0438.jpg" src="IMG_0438.thumb.jpg" data-description="Alps 1"&gt;
     * </pre>
     * @param imageList
     * @param captionMap
     * @param directory
     * @param photosPerPage
     */
    protected void buildGalleryHTML(ArrayList<Pair<String, String>> imageList, 
                                    HashMap<String, String> captionMap,
                                    File directory, 
                                    int photosPerPage ) {
        int pageCnt = 1;
        int imageCnt = 0;
        PrintStream pstream = null;
        try {
            for (Pair<String, String> imagePair : imageList) {
                if (imageCnt == 0) {
                    pstream = getNewFile(directory, pageCnt );
                    pageCnt++;
                }
                if (pstream != null) {
                    String caption = captionMap.get(imagePair.getLeft());
                    if (caption != null && caption.length() > 0) {
                        pstream.printf("<img src=\"%s\" data-big=\"%s\" data-description=\"%s\">\n", imagePair.getRight(), imagePair.getLeft(), caption);
                    } else {
                        pstream.printf("<img src=\"%s\" data-big=\"%s\">\n", imagePair.getRight(), imagePair.getLeft() );
                    }
                }
                imageCnt++;
                if (imageCnt == photosPerPage) {
                    imageCnt = 0;
                    if (pstream != null) {
                        pstream.close();
                    }
                    pstream = null;
                }
            }
        } catch (IOException e) {
            System.out.println("Error writing to gallery file: " + e.getLocalizedMessage() );
        } finally {
            if (pstream != null) {
                pstream.close();
            }
        }
    }
    
    
    protected void application( String[] args ) {
        if (args.length == 1) {
            String filePath = args[0];
            File directory = new File( filePath );
            if (directory.exists()) {
                if (directory.canRead()) {
                    if (directory.isDirectory()) {
                        HashMap<String, String> captionMap = getCaptions( directory );
                        ArrayList<Pair<String, String>> imageList = getImageList( directory );
                        buildGalleryHTML(imageList, captionMap, directory, PHOTOS_PER_PAGE );
                    } else {
                        System.out.println(filePath + " should be a directory");
                    }
                } else {
                    System.out.println("Cannot read " + filePath );
                }
            } else {
                System.out.println("The path " + filePath + " does not exist");
            }
        } else {
            usage();
        }
    }

    

    /**
     * <p>
     * Read an old Gallery photograph directory and build files containing HTML that can be used with the
     * Galleria (galleria.io) JavaScript gallery.
     * </p>
     * <p>
     * Arguments: [gallery directory path]
     * </p>
     * @param args
     */
    public static void main(String[] args) {
        GalleryBuilder app = new GalleryBuilder();
        app.application( args );
    }

}
