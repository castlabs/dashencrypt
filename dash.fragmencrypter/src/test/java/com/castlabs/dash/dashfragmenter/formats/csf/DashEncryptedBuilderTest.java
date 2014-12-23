package com.castlabs.dash.dashfragmenter.formats.csf;

import com.castlabs.dash.dashfragmenter.formats.csf.DashBuilder;
import com.castlabs.dash.dashfragmenter.mp4todash.BoxComparator;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.SyncSampleIntersectFinderImpl;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CencEncryptingTrackImpl;
import com.googlecode.mp4parser.util.UUIDConverter;
import org.junit.Ignore;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.UUID;

public class DashEncryptedBuilderTest {
    @Test

    public void stabilize() throws IOException {
        DashBuilder dashEncryptedBuilder = new DashBuilder();



        Movie m1 = MovieCreator.build(this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile() + "/v1.mp4");
        Movie m2 = new Movie();
        Track t = m1.getTracks().get(0);
        UUID keyId =  UUIDConverter.convert(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16});
        SecretKey key = new SecretKeySpec(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, "AES");
        CencEncryptingTrackImpl cencEncryptingTrack = new CencEncryptingTrackImpl(t, keyId, key,true);
        m2.addTrack(cencEncryptingTrack);
        dashEncryptedBuilder.setIntersectionFinder(new SyncSampleIntersectFinderImpl(m2, null, -1));

        Container i1 = dashEncryptedBuilder.build(m2);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        i1.writeContainer( Channels.newChannel(baos));
        FileChannel fc = new FileOutputStream("v1-reference.mp4").getChannel();
        i1.writeContainer(fc);

        DataSource dataSourceRef = new FileDataSourceImpl(this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile() + "/v1-reference.mp4");
        IsoFile i2 = new IsoFile(dataSourceRef);
        BoxComparator.check(i1, i2, "/moov[0]/mvhd[0]", "/moov[0]/trak[0]/tkhd[0]", "/moov[0]/trak[0]/mdia[0]/mdhd[0]");



    }


}