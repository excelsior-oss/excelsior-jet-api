package com.excelsiorjet.api.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void testParseUsualArgs() {
        String[] args = Utils.parseRunArgs("arg1,arg2");
        assertArrayEquals(new String[]{"arg1", "arg2"}, args);
    }

    @Test
    public void testParseArgWithSpace() {
        String[] args = Utils.parseRunArgs("arg1 arg2");
        assertArrayEquals(new String[]{"arg1 arg2"}, args);
    }

    @Test
    public void testParseArgWithComma() {
        String[] args = Utils.parseRunArgs("arg1\\, arg2");
        assertArrayEquals(new String[]{"arg1, arg2"}, args);
    }
    @Test
    public void testParseEmptyFirstArg() {
        String[] args = Utils.parseRunArgs(",arg");
        assertArrayEquals(new String[]{"", "arg"}, args);
    }

    @Test
    public void testParseEmptyLastArg() {
        String[] args = Utils.parseRunArgs("arg,");
        assertArrayEquals(new String[]{"arg", ""}, args);
    }

    @Test
    public void testParseCommaOnly() {
        String[] args = Utils.parseRunArgs(",");
        assertArrayEquals(new String[]{"", ""}, args);
    }
}