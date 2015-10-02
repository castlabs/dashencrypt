package com.castlabs.dash.helpers;

import org.junit.Test;

import java.io.File;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Created by sannies on 04.09.2015.
 */
public class LanguageHelperTest {
    @Test
    public void testGetName() throws Exception {
        Locale l = LanguageHelper.getFilesLanguage(new File("stereo-track.wav.48000Hz.128k.LC.2chan-zho.aac"));
        assertEquals("zho", l.getISO3Language());
        l = LanguageHelper.getFilesLanguage(new File("stereo-track.wav.48000Hz.128k.LC.2chan-de.aac"));
        assertEquals("deu", l.getISO3Language());
        l = LanguageHelper.getFilesLanguage(new File("stereo-track.wav.48000Hz.128k.LC.2chan-fr-FR.aac"));
        assertEquals("fra", l.getISO3Language());
    }
}