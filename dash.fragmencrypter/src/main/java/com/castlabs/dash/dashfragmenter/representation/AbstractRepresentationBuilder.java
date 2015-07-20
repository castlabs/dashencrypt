package com.castlabs.dash.dashfragmenter.representation;

import com.castlabs.dash.helpers.SapHelper;
import com.castlabs.dash.helpers.Timing;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.DataEntryUrlBox;
import com.coremedia.iso.boxes.DataInformationBox;
import com.coremedia.iso.boxes.DataReferenceBox;
import com.coremedia.iso.boxes.EditBox;
import com.coremedia.iso.boxes.EditListBox;
import com.coremedia.iso.boxes.FileTypeBox;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.HintMediaHeaderBox;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.MediaInformationBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.coremedia.iso.boxes.NullMediaHeaderBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.SampleToChunkBox;
import com.coremedia.iso.boxes.SoundMediaHeaderBox;
import com.coremedia.iso.boxes.StaticChunkOffsetBox;
import com.coremedia.iso.boxes.SubtitleMediaHeaderBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.coremedia.iso.boxes.VideoMediaHeaderBox;
import com.coremedia.iso.boxes.fragment.MovieExtendsBox;
import com.coremedia.iso.boxes.fragment.MovieExtendsHeaderBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.SampleFlags;
import com.coremedia.iso.boxes.fragment.TrackExtendsBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.authoring.Edit;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.tracks.CencEncryptedTrack;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.CencSampleEncryptionInformationGroupEntry;
import com.googlecode.mp4parser.boxes.mp4.samplegrouping.GroupEntry;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;
import com.googlecode.mp4parser.util.Path;
import com.googlecode.mp4parser.util.UUIDConverter;
import com.mp4parser.iso23001.part7.ProtectionSystemSpecificHeaderBox;
import mpegCenc2013.DefaultKIDAttribute;
import mpegDashSchemaMpd2011.DescriptorType;
import mpegDashSchemaMpd2011.RepresentationType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.castlabs.dash.helpers.BoxHelper.boxToBytes;
import static com.castlabs.dash.helpers.Timing.getPtss;
import static com.castlabs.dash.helpers.Timing.getTimeMappingEditTime;
import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * Created by sannies on 26.06.2015.
 */
public abstract class AbstractRepresentationBuilder extends AbstractList<Container> implements RepresentationBuilder {
    protected Track theTrack;
    protected final List<ProtectionSystemSpecificHeaderBox> psshs;
    protected Date date = new Date();
    protected String source;

    public AbstractRepresentationBuilder(Track track, List<ProtectionSystemSpecificHeaderBox> psshs, String source) {
        this.theTrack = track;
        this.psshs = psshs;
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public Track getTrack() {
        return theTrack;
    }

    public Container getInitSegment() {
        List<Box> initSegment = new ArrayList<Box>();
        List<String> minorBrands = new ArrayList<String>();
        minorBrands.add("isom");
        minorBrands.add("iso6");
        minorBrands.add("avc1");
        initSegment.add(new FileTypeBox("isom", 0, minorBrands));
        initSegment.add(createMoov());
        return new ListContainer(initSegment);
    }

    protected Box createMoov() {
        MovieBox movieBox = new MovieBox();

        movieBox.addBox(createMvhd());
        movieBox.addBox(createTrak(theTrack));
        movieBox.addBox(createMvex());
        if (psshs != null) {
            for (ProtectionSystemSpecificHeaderBox pssh : psshs) {
                movieBox.addBox(pssh);
            }
        }
        // metadata here
        return movieBox;
    }


    protected Box createTrex(Track track) {
        TrackExtendsBox trex = new TrackExtendsBox();
        trex.setTrackId(track.getTrackMetaData().getTrackId());
        trex.setDefaultSampleDescriptionIndex(1);
        trex.setDefaultSampleDuration(0);
        trex.setDefaultSampleSize(0);
        SampleFlags sf = new SampleFlags();
        if ("soun".equals(track.getHandler()) || "subt".equals(track.getHandler())) {
            // as far as I know there is no audio encoding
            // where the sample are not self contained.
            // same seems to be true for subtitle tracks
            sf.setSampleDependsOn(2);
            sf.setSampleIsDependedOn(2);
        }
        trex.setDefaultSampleFlags(sf);
        return trex;
    }

    protected Box createMvex() {
        MovieExtendsBox mvex = new MovieExtendsBox();
        final MovieExtendsHeaderBox mved = new MovieExtendsHeaderBox();
        mved.setVersion(1);
        mved.setFragmentDuration(theTrack.getDuration());
        mvex.addBox(mved);

        mvex.addBox(createTrex(theTrack));
        return mvex;
    }

    protected Box createTkhd(Track track) {
        TrackHeaderBox tkhd = new TrackHeaderBox();
        tkhd.setVersion(1);
        tkhd.setFlags(7); // enabled, in movie, in previe, in poster

        tkhd.setAlternateGroup(track.getTrackMetaData().getGroup());
        tkhd.setCreationTime(track.getTrackMetaData().getCreationTime());
        // We need to take edit list box into account in trackheader duration
        // but as long as I don't support edit list boxes it is sufficient to
        // just translate media duration to movie timescale
        tkhd.setDuration(0);//no duration in moov for fragmented movies
        tkhd.setHeight(track.getTrackMetaData().getHeight());
        tkhd.setWidth(track.getTrackMetaData().getWidth());
        tkhd.setLayer(track.getTrackMetaData().getLayer());
        tkhd.setModificationTime(getDate());
        tkhd.setTrackId(track.getTrackMetaData().getTrackId());
        tkhd.setVolume(track.getTrackMetaData().getVolume());
        return tkhd;
    }


    protected Box createMvhd() {
        MovieHeaderBox mvhd = new MovieHeaderBox();
        mvhd.setVersion(1);
        mvhd.setCreationTime(getDate());
        mvhd.setModificationTime(getDate());
        mvhd.setDuration(0);//no duration in moov for fragmented movies
        long movieTimeScale = theTrack.getTrackMetaData().getTimescale();
        mvhd.setTimescale(movieTimeScale);
        mvhd.setNextTrackId(theTrack.getTrackMetaData().getTrackId() + 1);
        return mvhd;
    }


    protected Box createStbl(Track track) {
        SampleTableBox stbl = new SampleTableBox();

        createStsd(track, stbl);
        stbl.addBox(new TimeToSampleBox());
        stbl.addBox(new SampleToChunkBox());
        stbl.addBox(new SampleSizeBox());
        stbl.addBox(new StaticChunkOffsetBox());
        return stbl;
    }

    protected void createStsd(Track track, SampleTableBox stbl) {
        stbl.addBox(track.getSampleDescriptionBox());
    }

    protected Box createMinf(Track track) {
        MediaInformationBox minf = new MediaInformationBox();
        if (track.getHandler().equals("vide")) {
            minf.addBox(new VideoMediaHeaderBox());
        } else if (track.getHandler().equals("soun")) {
            minf.addBox(new SoundMediaHeaderBox());
        } else if (track.getHandler().equals("text")) {
            minf.addBox(new NullMediaHeaderBox());
        } else if (track.getHandler().equals("subt")) {
            minf.addBox(new SubtitleMediaHeaderBox());
        } else if (track.getHandler().equals("hint")) {
            minf.addBox(new HintMediaHeaderBox());
        } else if (track.getHandler().equals("sbtl")) {
            minf.addBox(new NullMediaHeaderBox());
        }
        minf.addBox(createDinf());
        minf.addBox(createStbl(track));
        return minf;
    }

    protected Box createMdiaHdlr(Track track) {
        HandlerBox hdlr = new HandlerBox();
        hdlr.setHandlerType(track.getHandler());
        return hdlr;
    }

    protected Box createMdhd(Track track) {
        MediaHeaderBox mdhd = new MediaHeaderBox();
        mdhd.setCreationTime(track.getTrackMetaData().getCreationTime());
        mdhd.setModificationTime(getDate());
        mdhd.setDuration(0);//no duration in moov for fragmented movies
        mdhd.setTimescale(track.getTrackMetaData().getTimescale());
        mdhd.setLanguage(track.getTrackMetaData().getLanguage());
        return mdhd;
    }

    public Date getDate() {
        return date;
    }


    protected DataInformationBox createDinf() {
        DataInformationBox dinf = new DataInformationBox();
        DataReferenceBox dref = new DataReferenceBox();
        dinf.addBox(dref);
        DataEntryUrlBox url = new DataEntryUrlBox();
        url.setFlags(1);
        dref.addBox(url);
        return dinf;
    }

    protected Box createMdia(Track track) {
        MediaBox mdia = new MediaBox();
        mdia.addBox(createMdhd(track));
        mdia.addBox(createMdiaHdlr(track));
        mdia.addBox(createMinf(track));
        return mdia;
    }

    protected Box createTrak(Track track) {
        TrackBox trackBox = new TrackBox();
        trackBox.addBox(createTkhd(track));
        Box edts = createEdts(track);
        if (edts != null) {
            trackBox.addBox(edts);
        }
        trackBox.addBox(createMdia(track));
        return trackBox;
    }

    protected Box createEdts(Track track) {
        if (track.getEdits() != null && track.getEdits().size() > 0) {
            EditListBox elst = new EditListBox();
            elst.setVersion(1);
            List<EditListBox.Entry> entries = new ArrayList<EditListBox.Entry>();

            for (Edit edit : track.getEdits()) {
                entries.add(new EditListBox.Entry(elst,
                        Math.round(edit.getSegmentDuration() * track.getTrackMetaData().getTimescale()),
                        edit.getMediaTime() * track.getTrackMetaData().getTimescale() / edit.getTimeScale(),
                        edit.getMediaRate()));
            }

            elst.setEntries(entries);
            EditBox edts = new EditBox();
            edts.addBox(elst);
            return edts;
        } else {
            return null;
        }
    }


    protected void addContentProtection(RepresentationType representation) {
        List<String> keyIds = new ArrayList<String>();
        if (theTrack instanceof CencEncryptedTrack) {
            keyIds.add(((CencEncryptedTrack) theTrack).getDefaultKeyId().toString());
            for (GroupEntry ge : theTrack.getSampleGroups().keySet()) {
                if (ge instanceof CencSampleEncryptionInformationGroupEntry) {
                    if (((CencSampleEncryptionInformationGroupEntry) ge).getKid() != null) {
                        keyIds.add(((CencSampleEncryptionInformationGroupEntry) ge).getKid().toString());
                    }
                }
            }

        }

        if (!keyIds.isEmpty()) {
            DescriptorType contentProtection = representation.addNewContentProtection();
            final DefaultKIDAttribute defaultKIDAttribute = DefaultKIDAttribute.Factory.newInstance();

            defaultKIDAttribute.setDefaultKID(keyIds);
            contentProtection.set(defaultKIDAttribute);
            contentProtection.setSchemeIdUri("urn:mpeg:dash:mp4protection:2011");
            contentProtection.setValue("cenc");
        }

        if (psshs != null) {
            for (ProtectionSystemSpecificHeaderBox pssh : psshs) {
                DescriptorType dt = representation.addNewContentProtection();
                byte[] psshContent = pssh.getContent();
                dt.setSchemeIdUri("urn:uuid:" + UUIDConverter.convert(pssh.getSystemId()).toString());
                if (Arrays.equals(ProtectionSystemSpecificHeaderBox.PLAYREADY_SYSTEM_ID, pssh.getSystemId())) {
                    dt.setValue("MSPR 2.0");
                    Node playReadyCPN = dt.getDomNode();
                    Document d = playReadyCPN.getOwnerDocument();
                    Element pro = d.createElementNS("urn:microsoft:playready", "pro");
                    Element prPssh = d.createElementNS("urn:mpeg:cenc:2013", "pssh");

                    pro.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(psshContent)));
                    prPssh.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(boxToBytes(pssh))));

                    playReadyCPN.appendChild(pro);
                    playReadyCPN.appendChild(prPssh);
                }
                if (Arrays.equals(ProtectionSystemSpecificHeaderBox.WIDEVINE, pssh.getSystemId())) {
                    // Widevvine
                    Node widevineCPN = dt.getDomNode();
                    Document d = widevineCPN.getOwnerDocument();
                    Element wvPssh = d.createElementNS("urn:mpeg:cenc:2013", "pssh");
                    wvPssh.appendChild(d.createTextNode(Base64.getEncoder().encodeToString(boxToBytes(pssh))));

                    widevineCPN.appendChild(wvPssh);
                }
            }
        }
    }

    public Container getIndexSegment() {
        SegmentIndexBox sidx = new SegmentIndexBox();
        sidx.setVersion(0);
        sidx.setFlags(0);
        sidx.setReserved(0);

        Container initSegment = getInitSegment();
        TrackHeaderBox tkhd = Path.getPath(initSegment, "/moov[0]/trak[0]/tkhd[0]");
        MediaHeaderBox mdhd = Path.getPath(initSegment, "/moov[0]/trak[0]/mdia[0]/mdhd[0]");
        sidx.setReferenceId(tkhd.getTrackId());
        sidx.setTimeScale(mdhd.getTimescale());
        // we only have one
        long[] ptss = getPtss(Path.<TrackRunBox>getPath(get(0), "/moof[0]/traf[0]/trun[0]"));
        Arrays.sort(ptss); // index 0 has now the earliest presentation time stamp!
        long timeMappingEdit = getTimeMappingEditTime(initSegment);
        sidx.setEarliestPresentationTime(ptss[0] - timeMappingEdit);
        List<SegmentIndexBox.Entry> entries = sidx.getEntries();

        TrackExtendsBox trex = Path.getPath(initSegment, "/moov[0]/mvex[0]/trex[0]");

        // ugly code ...

        for (Container c : this) {
            int size = 0;
            for (Box box : c.getBoxes()) {
                size += l2i(box.getSize());
            }
            MovieFragmentBox moof = Path.getPath(c, "/moof[0]");
            SegmentIndexBox.Entry entry = new SegmentIndexBox.Entry();
            entries.add(entry);
            entry.setReferencedSize(size);
            TrackRunBox trun = Path.getPath(moof, "traf[0]/trun[0]");
            ptss = getPtss(trun);
            entry.setSapType(SapHelper.getFirstFrameSapType(ptss, SapHelper.getSampleFlags(0, trun, trex)));
            entry.setSubsegmentDuration(Timing.getDuration(Path.<TrackRunBox>getPath(moof, "traf[0]/trun[0]")));
            entry.setStartsWithSap((byte) 1); // we know it - no need to lookup
        }

        sidx.setFirstOffset(0);
        return new ListContainer(Collections.<Box>singletonList(sidx));
    }

}
