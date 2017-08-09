package com.castlabs.dash.dashfragmenter.cmdlines.trackoptions;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.TrackBox;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.FileRandomAccessSourceImpl;
import org.mp4parser.muxer.Mp4TrackImpl;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.mp4parser.muxer.tracks.h264.H264TrackImpl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.castlabs.dash.helpers.DashHelper.getTextTrackLocale;

/**
 * Represents one stream.
 */
public class InputOutputSelector {
    public static final List<Track> TEXTTRACK = new ArrayList<>();
    public static final List<Track> THUMBTRACK = new ArrayList<>();

    private static final Pattern periodPattern = Pattern.compile("period-([0-9]+)");
    private static final String[] outputOptionsKeys = new String[]{"lang", "period", "role", "htiles", "vtiles", "thduration"};

    private static final Set<String> MP4_FILE_EXTS = new HashSet<>(Arrays.asList("mp4", "m4a", "m4v", "ismv", "isma", "mov"));
    private static final Set<String> THUMB_FILE_EXTS = new HashSet<>(Arrays.asList("jpg", "jpeg"));


    private Map<String, String> outputOptions = new HashMap<>();
    private List<Track> tracks = new ArrayList<>();
    private List<File> files = new ArrayList<>();

    public List<File> getFiles() {
        return files;
    }

    public InputOutputSelector(Map<String, String> in, String filePattern, Map<String, String> out) throws IOException {
        Map<String, String> inPop = new HashMap<>(in);
        Map<String, String> outPop = new HashMap<>(out);


        this.files = Glob.get(new File(""), filePattern);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("The file pattern " + filePattern + " doesn't yield any results.");
        }

        for (String outputOptionsKey : outputOptionsKeys) {
            outPop.remove(outputOptionsKey);
        }

        if (!outPop.isEmpty()) {
            throw new IllegalArgumentException("out Options " + outPop.keySet() + " not supported");
        }

        outputOptions = new HashMap<>(out);

        for (File f : files) {

            Matcher m = periodPattern.matcher(f.getAbsolutePath());
            if (m.find()) {
                if (out.containsKey("period")) {
                    throw new IllegalArgumentException("Period must be either specified via out-property or filename");
                }
                outputOptions.put("period", m.group(1));

            }


            if (MP4_FILE_EXTS.contains(FilenameUtils.getExtension(f.getName()).toLowerCase())) {
                IsoFile isoFile = new IsoFile(f);
                List<TrackBox> trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox.class);
                String type = inPop.remove("type");
                String language = inPop.remove("lang");
                String trackNo = inPop.remove("trackNo");


                if (!inPop.isEmpty()) {
                    throw new IllegalArgumentException("in Options " + inPop.keySet() + " not supported");
                }

                int no = 0;

                for (TrackBox trackBox : trackBoxes) {
                    boolean include = true;
                    if (language != null && !trackBox.getMediaBox().getMediaHeaderBox().getLanguage().equals(language)) {
                        LOG.info("Excluding track " + trackBox.getTrackHeaderBox().getTrackId() + " of " + f + " from processing as language is " + trackBox.getMediaBox().getMediaHeaderBox().getLanguage() + " but not " + language + ".");
                        include = false;
                    }
                    if (type != null) {
                        String handler = trackBox.getMediaBox().getHandlerBox().getHandlerType();
                        if (!handlerToType.computeIfAbsent(handler, e -> e).equals(type)) {
                            LOG.info("Excluding track " + trackBox.getTrackHeaderBox().getTrackId() + " of " + f + " from processing as type is " + handlerToType.computeIfAbsent(handler, e -> e) + " but not " + type + ".");
                            include = false;
                        }
                    }
                    if (trackNo != null) {
                        if (Integer.parseInt(trackNo) != no) {
                            LOG.info("Excluding track no. " + no + " as only " + trackNo + " is included");
                            include = false;
                        }
                    }
                    if (include) {
                        tracks.add(new Mp4TrackImpl(trackBox.getTrackHeaderBox().getTrackId(), isoFile, new FileRandomAccessSourceImpl(new RandomAccessFile(f, "r")), f.getName()));
                    }
                }
                if (tracks.isEmpty()) {
                    throw new IllegalArgumentException("File Extension of " + f + " unknown");
                }
            } else if (f.getName().endsWith(".h264")) {
                tracks.add(new H264TrackImpl(new FileDataSourceImpl(f)));

            } else if (f.getName().endsWith(".aac")) {
                AACTrackImpl a = new AACTrackImpl(new FileDataSourceImpl(f));
                a.getTrackMetaData().setLanguage("eng");
                tracks.add(a);
            } else if (f.getName().endsWith(".dfxp") || f.getName().endsWith(".vtt") || f.getName().endsWith(".vtt")) {
                if (!inPop.isEmpty()) {
                    throw new IllegalArgumentException("in Options " + inPop.keySet() + " not supported for text tracks");
                }
                tracks = TEXTTRACK;
                if (!outputOptions.containsKey("lang")) {
                    outputOptions.put("lang", getTextTrackLocale(f).toLanguageTag());
                }
            } else if (THUMB_FILE_EXTS.contains(FilenameUtils.getExtension(f.getName()).toLowerCase())) {
                if (!inPop.isEmpty()) {
                    throw new IllegalArgumentException("in Options " + inPop.keySet() + " not supported for text tracks");
                }

                tracks = THUMBTRACK;
                if (!outputOptions.containsKey("htiles")) {
                    throw new IllegalArgumentException("All " + FilenameUtils.getExtension(f.getName()) + " thumbnail tracks require 'htiles' output property to be set (number of tiles left to right)");
                }
                if (!outputOptions.containsKey("vtiles")) {
                    throw new IllegalArgumentException("All " + FilenameUtils.getExtension(f.getName()) + " thumbnail tracks require 'vtiles' output property to be set (number of tiles top to bottom)");
                }
                if (!outputOptions.containsKey("thduration")) {
                    throw new IllegalArgumentException("All " + FilenameUtils.getExtension(f.getName()) + " thumbnail tracks require 'thduration' output property to be set (duration of a single thumbnail in seconds)");
                }
                for (File file : files) {
                    if (!FilenameUtils.getExtension(file.getName()).toLowerCase().equals(FilenameUtils.getExtension(f.getName()).toLowerCase())) {
                        throw new IllegalArgumentException("The pattern " + filePattern + " also includes " + file.getName() + " which is of a different type. All ");
                    }
                }
                List<File> sortedFiles = new ArrayList<>(files);
                sortedFiles.sort(new WindowsExplorerComparator());
                this.files = sortedFiles;
                break;
            } else {
                throw new IllegalArgumentException("File Extension of " + f + " unknown");
            }
        }


    }


    public List<Track> getSelectedTracks() {
        return tracks;
    }

    public Map<String, String> getOutputOptions() {
        return outputOptions;
    }

    private static Map<String, String> handlerToType = new HashMap<>();

    static {
        handlerToType.put("soun", "audio");
        handlerToType.put("vide", "video");
    }

    private static Logger LOG = Logger.getLogger(InputOutputSelector.class.getName());

    public static class WindowsExplorerComparator implements Comparator<File> {

        private static final Pattern splitPattern = Pattern.compile("\\d+|\\.|\\s");

        @Override
        public int compare(File f1, File f2) {
            String str1 = f1.getName();
            String str2 = f2.getName();
            Iterator<String> i1 = splitStringPreserveDelimiter(str1).iterator();
            Iterator<String> i2 = splitStringPreserveDelimiter(str2).iterator();
            while (true) {
                //Til here all is equal.
                if (!i1.hasNext() && !i2.hasNext()) {
                    return 0;
                }
                //first has no more parts -> comes first
                if (!i1.hasNext() && i2.hasNext()) {
                    return -1;
                }
                //first has more parts than i2 -> comes after
                if (i1.hasNext() && !i2.hasNext()) {
                    return 1;
                }

                String data1 = i1.next();
                String data2 = i2.next();
                int result;
                try {
                    //If both datas are numbers, then compare numbers
                    result = Long.compare(Long.valueOf(data1), Long.valueOf(data2));
                    //If numbers are equal than longer comes first
                    if (result == 0) {
                        result = -Integer.compare(data1.length(), data2.length());
                    }
                } catch (NumberFormatException ex) {
                    //compare text case insensitive
                    result = data1.compareToIgnoreCase(data2);
                }

                if (result != 0) {
                    return result;
                }
            }
        }

        private List<String> splitStringPreserveDelimiter(String str) {
            Matcher matcher = splitPattern.matcher(str);
            List<String> list = new ArrayList<String>();
            int pos = 0;
            while (matcher.find()) {
                list.add(str.substring(pos, matcher.start()));
                list.add(matcher.group());
                pos = matcher.end();
            }
            list.add(str.substring(pos));
            return list;
        }
    }

}
