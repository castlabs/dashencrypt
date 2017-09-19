package com.castlabs.dash.dashfragmenter.cmdlines.trackoptions;

import org.junit.Assert;
import org.junit.Test;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.io.IOException;

public class InputOutputSelectorOptionHandlerTest {

    public class Main {
        @Argument(handler = InputOutputSelectorOptionHandler.class)
        InputOutputSelector i;
    }

    @Test
    public void test1() throws CmdLineException, IOException {
        Main m = new Main();
        CmdLineParser cmdLineParser = new CmdLineParser(m);
        File f = File.createTempFile("aaa", ".jpg");
        cmdLineParser.parseArgument(f.getParent() + "/*.jpg[vtiles=5,htiles=6,thduration=7]");
        Assert.assertEquals("5", m.i.getOutputOptions().get("vtiles"));
        Assert.assertEquals("6", m.i.getOutputOptions().get("htiles"));
        Assert.assertEquals("7", m.i.getOutputOptions().get("thduration"));

    }

    @Test(expected = CmdLineException.class)

    public void test2() throws CmdLineException, IOException {
        Main m = new Main();
        CmdLineParser cmdLineParser = new CmdLineParser(m);
        File f = File.createTempFile("aaa", ".jpghhh");
        cmdLineParser.parseArgument(f.getParent() + "/*.jpghhh[vtiles=5,htiles=6,thduration=7]");
    }

}