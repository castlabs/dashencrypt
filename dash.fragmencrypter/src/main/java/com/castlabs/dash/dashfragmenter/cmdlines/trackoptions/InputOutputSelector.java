package com.castlabs.dash.dashfragmenter.cmdlines.trackoptions;


import org.apache.commons.io.FilenameUtils;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.TrackBox;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.FileRandomAccessSourceImpl;
import org.mp4parser.muxer.Mp4TrackImpl;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.mp4parser.muxer.tracks.h264.H264TrackImpl;
import org.mp4parser.muxer.tracks.h265.H265TrackImpl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.castlabs.dash.helpers.DashHelper2.getTextTrackLocale;


/**
 * Represents one stream.
 */
public class InputOutputSelector {
    public static final List<Track> TEXTTRACK = new ArrayList<>();
    public static final List<Track> THUMBTRACK = new ArrayList<>();

    private static final Pattern periodPattern = Pattern.compile("period-([0-9]+)");

    private static final Set<String> MP4_FILE_EXTS = new HashSet<>(Arrays.asList("mp4", "m4a", "m4v", "ismv", "isma", "mov"));
    private static final Set<String> THUMB_FILE_EXTS = new HashSet<>(Arrays.asList("jpg", "jpeg"));
    private static final Set<String> H264_FILE_EXTS = new HashSet<>(Arrays.asList("h264", "264"));
    private static final Set<String> H265_FILE_EXTS = new HashSet<>(Arrays.asList("h265", "265"));
    private static final Set<String> AAC_FILE_EXTS = new HashSet<>(Collections.singletonList("aac"));
    private static final Set<String> TEXT_FILE_EXTS = new HashSet<>(Arrays.asList("dfxp", "vtt"));
    private static final Map<String, Set<String>> outputOptionsPerExt = new HashMap<>();
    private static final Map<String, Set<String>> inputOptionsPerExt = new HashMap<>();
    private static final Map<String, Set<String>> mandatoryOutputOptionsPerExt = new HashMap<>();

    static {

        for (String ext : MP4_FILE_EXTS) {
            inputOptionsPerExt.put(ext, new HashSet<>(Arrays.asList("type", "lang", "trackNo")));
            outputOptionsPerExt.put(ext, new HashSet<>(Arrays.asList("lang", "period", "role")));
            mandatoryOutputOptionsPerExt.put(ext, Collections.emptySet());
        }
        for (String ext : THUMB_FILE_EXTS) {
            inputOptionsPerExt.put(ext, Collections.emptySet());
            outputOptionsPerExt.put(ext, new HashSet<>(Arrays.asList("period", "role", "htiles", "vtiles", "thduration")));
            mandatoryOutputOptionsPerExt.put(ext, new HashSet<>(Arrays.asList("htiles", "vtiles", "thduration")));
        }
        for (String ext : H264_FILE_EXTS) {
            inputOptionsPerExt.put(ext, Collections.emptySet());
            outputOptionsPerExt.put(ext, new HashSet<>(Arrays.asList("period", "role")));
            mandatoryOutputOptionsPerExt.put(ext, Collections.emptySet());
        }
        for (String ext : H265_FILE_EXTS) {
            inputOptionsPerExt.put(ext, Collections.emptySet());
            outputOptionsPerExt.put(ext, new HashSet<>(Arrays.asList("period", "role")));
            mandatoryOutputOptionsPerExt.put(ext, Collections.emptySet());
        }
        for (String ext : AAC_FILE_EXTS) {
            inputOptionsPerExt.put(ext, Collections.emptySet());
            outputOptionsPerExt.put(ext, new HashSet<>(Arrays.asList("lang", "period", "role")));
            mandatoryOutputOptionsPerExt.put(ext, Collections.emptySet());
        }
        for (String ext : TEXT_FILE_EXTS) {
            inputOptionsPerExt.put(ext, Collections.emptySet());
            outputOptionsPerExt.put(ext, new HashSet<>(Arrays.asList("lang", "period", "role")));
            mandatoryOutputOptionsPerExt.put(ext, Collections.emptySet());
        }

    }


    private Map<String, String> outputOptions = new HashMap<>();
    private List<Track> tracks = new ArrayList<>();
    private List<File> files = new ArrayList<>();

    public List<File> getFiles() {
        return files;
    }

    public InputOutputSelector(Map<String, String> in, String filePattern, Map<String, String> outputOptions) throws IOException {
        this.outputOptions = outputOptions;
        this.files = Glob.get(new File(""), filePattern);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("The file pattern " + filePattern + " doesn't yield any results.");
        }

        if (files.stream().map(f -> FilenameUtils.getExtension(f.getName()).toLowerCase()).distinct().count() > 1) {
            throw new IllegalArgumentException("The pattern " + filePattern + " includes multiple filetypes: " +
                    files.stream().map(f -> FilenameUtils.getExtension(f.getName()).toLowerCase()).distinct().collect(Collectors.joining(", ")) +
                    ". All files captured by the pattern need to have the same extension!");
        }


        for (File f : files) {
            Matcher m = periodPattern.matcher(f.getAbsolutePath());
            if (m.find()) {
                if (outputOptions.containsKey("period")) {
                    throw new IllegalArgumentException("Period must be either specified via out-property or filename");
                }
                outputOptions.put("period", m.group(1));
            }


            String fileExt = FilenameUtils.getExtension(f.getName()).toLowerCase();

            if (!inputOptionsPerExt.containsKey(fileExt)) {
                throw new IllegalArgumentException("File Extension of " + f + " is not supported.");
            }
            if (in.keySet().stream().anyMatch(s -> !inputOptionsPerExt.get(fileExt).contains(s))) {
                String wrong = in.keySet().stream().filter(s -> !inputOptionsPerExt.get(fileExt).contains(s)).collect(Collectors.joining(", "));
                throw new IllegalArgumentException("The input options " + wrong + " are invalid for input files with extension " + fileExt + ". Valid input options are: " + inputOptionsPerExt.get(fileExt));
            }


            if (!outputOptionsPerExt.containsKey(fileExt)) {
                throw new IllegalArgumentException("File Extension of " + f + " is not supported.");
            }
            if (outputOptions.keySet().stream().anyMatch(s -> !outputOptionsPerExt.get(fileExt).contains(s))) {
                String wrong = outputOptions.keySet().stream().filter(s -> !outputOptionsPerExt.get(fileExt).contains(s)).collect(Collectors.joining(", "));
                throw new IllegalArgumentException("The output options " + wrong + " are invalid for input files with extension " + fileExt + ". Valid output options are: " + outputOptionsPerExt.get(fileExt));
            }

            if (!mandatoryOutputOptionsPerExt.containsKey(fileExt)) {
                throw new IllegalArgumentException("File Extension of " + f + " is not supported.");
            }
            if (mandatoryOutputOptionsPerExt.get(fileExt).stream().anyMatch(opt -> !outputOptions.containsKey(opt))) {
                String req = mandatoryOutputOptionsPerExt.get(fileExt).stream().collect(Collectors.joining(", "));
                throw new IllegalArgumentException("The output options " + req + " are required for input files with extension " + fileExt);
            }

            if (MP4_FILE_EXTS.contains(fileExt)) {
                IsoFile isoFile = new IsoFile(f);
                List<TrackBox> trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox.class);

                String type = in.get("type");
                String language = in.get("lang");
                String trackNo = in.get("trackNo");

                int no = 0;

                for (TrackBox trackBox : trackBoxes) {
                    boolean include = true;
                    String handler = trackBox.getMediaBox().getHandlerBox().getHandlerType();
                    if (language != null && !trackBox.getMediaBox().getMediaHeaderBox().getLanguage().equals(language)) {
                        LOG.info(f.getName() + ": Excluding track " + trackBox.getTrackHeaderBox().getTrackId() + " from processing as language is " + trackBox.getMediaBox().getMediaHeaderBox().getLanguage() + " but not " + language + ".");
                        include = false;
                    }
                    if (type != null) {

                        if (!handlerToType.computeIfAbsent(handler, e -> e).equals(type)) {
                            LOG.info(f.getName() + ": Excluding track " + trackBox.getTrackHeaderBox().getTrackId() + " from processing as type is " + handlerToType.computeIfAbsent(handler, e -> e) + " but not " + type + ".");
                            include = false;
                        }
                    }
                    if (trackNo != null) {
                        if (Integer.parseInt(trackNo) != no) {
                            LOG.info(f.getName() + ": Excluding track " + no + "("+ handler +") as only " + trackNo + " is included");
                            include = false;
                        }
                    }
                    if (include) {
                        LOG.info(f.getName() + ": Selecting track " + no + " (" + handler + ")");
                        tracks.add(new Mp4TrackImpl(trackBox.getTrackHeaderBox().getTrackId(), isoFile, new FileRandomAccessSourceImpl(new RandomAccessFile(f, "r")), f.getName()));
                    }
                }
                if (tracks.isEmpty()) {
                    throw new IllegalArgumentException("File Extension of " + f + " unknown");
                }
            } else if (H264_FILE_EXTS.contains(fileExt)) {
                tracks.add(new H264TrackImpl(new FileDataSourceImpl(f)));
            } else if (H265_FILE_EXTS.contains(fileExt)) {
                tracks.add(new H265TrackImpl(new FileDataSourceImpl(f)));
            } else if (AAC_FILE_EXTS.contains(fileExt)) {
                AACTrackImpl a = new AACTrackImpl(new FileDataSourceImpl(f));
                String lang = "eng";
                if (!outputOptions.containsKey("lang")) {
                    lang = Locale.forLanguageTag(outputOptions.get("lang")).getISO3Language();
                }
                a.getTrackMetaData().setLanguage(lang);
                tracks.add(a);
            } else if (TEXT_FILE_EXTS.contains(fileExt)) {
                tracks = TEXTTRACK;
                if (!outputOptions.containsKey("lang")) {
                    outputOptions.put("lang", getTextTrackLocale(f).toLanguageTag());
                }
            } else if (THUMB_FILE_EXTS.contains(fileExt)) {
                tracks = THUMBTRACK;

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
