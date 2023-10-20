package com.godaddy.vps4.vm;

import org.junit.Before;
import org.junit.Test;

import static com.godaddy.vps4.vm.Image.ControlPanel.CPANEL;
import static com.godaddy.vps4.vm.Image.ControlPanel.MYH;
import static com.godaddy.vps4.vm.Image.ControlPanel.PLESK;
import static com.godaddy.vps4.vm.Image.ControlPanel.getEnumValueFromEcommName;
import static org.junit.Assert.*;


public class ImageTest {
    private Image image;
    @Before
    public void setUp() throws Exception {
        image = new Image();
    }

    @Test
    public void hasCpanel() {
        image.controlPanel = CPANEL;
        assertTrue(image.hasCpanel());
    }

    @Test
    public void noCpanel() {
        image.controlPanel = Image.ControlPanel.PLESK;
        assertFalse(image.hasCpanel());
    }

    @Test
    public void hasPlesk() {
        image.controlPanel = Image.ControlPanel.PLESK;
        assertTrue(image.hasPlesk());
    }

    @Test
    public void noPlesk() {
        image.controlPanel = CPANEL;
        assertFalse(image.hasPlesk());
    }

    @Test
    public void hasMYH() {
        image.controlPanel = Image.ControlPanel.MYH;
        assertTrue(image.hasMYH());
    }

    @Test
    public void noMYH() {
        image.controlPanel = CPANEL;
        assertFalse(image.hasMYH());
    }

    @Test
    public void hasIspConfig() {
        image.controlPanel = Image.ControlPanel.MYH;
        image.hfsName = "foobar-ispconfig-hellworld";
        assertTrue(image.hasIspConfig());
    }

    @Test
    public void noISPConfig() {
        image.controlPanel = CPANEL;
        assertFalse(image.hasIspConfig());
    }

    @Test
    public void hasControlPanelReturnsTrueForCPanel() {
        image.controlPanel = CPANEL;
        assertTrue(image.hasControlPanel());
    }

    @Test
    public void hasControlPanelReturnsTrueForPlesk() {
        image.controlPanel = CPANEL;
        assertTrue(image.hasControlPanel());
    }

    @Test
    public void hasControlPanelReturnsTrueForISPConfig() {
        image.controlPanel = Image.ControlPanel.MYH;
        image.hfsName = "foobar-ispconfig-hellworld";
        assertTrue(image.hasIspConfig());
    }

    @Test
    public void hasControlPanelReturnsFalseForJustMYH() {
        image.controlPanel = Image.ControlPanel.MYH;
        image.hfsName = "foobar";
        assertFalse(image.hasIspConfig());
    }

    @Test
    public void hasPaidControlPanelReturnsTrueForCpanel() {
        image.controlPanel = CPANEL;
        assertTrue(image.hasPaidControlPanel());
    }

    @Test
    public void hasPaidControlPanelReturnsTrueForPlesk() {
        image.controlPanel = Image.ControlPanel.PLESK;
        assertTrue(image.hasPaidControlPanel());
    }

    @Test
    public void hasPaidControlPanelReturnsFalseForIspConfig() {
        image.controlPanel = Image.ControlPanel.MYH;
        image.hfsName = "foobar-ispconfig-hellworld";
        assertFalse(image.hasPaidControlPanel());
    }

    @Test
    public void hasPaidControlPanelReturnsFalseForMYH() {
        image.controlPanel = Image.ControlPanel.MYH;
        image.hfsName = "foobar";
        assertFalse(image.hasPaidControlPanel());
    }

    @Test
    public void hasFreeControlPanelReturnsTrueForIspConfig() {
        image.controlPanel = Image.ControlPanel.MYH;
        image.hfsName = "foobar-ispconfig-hellworld";
        assertTrue(image.hasFreeControlPanel());
    }

    @Test
    public void hasFreeControlPanelReturnsFalseForMYH() {
        image.controlPanel = Image.ControlPanel.MYH;
        image.hfsName = "foobar";
        assertFalse(image.hasFreeControlPanel());
    }

    @Test
    public void hasFreeControlPanelReturnsFalseForNonMYH() {
        image.controlPanel = CPANEL;
        image.hfsName = "foobar";
        assertFalse(image.hasFreeControlPanel());
    }

    @Test
    public void getImageControlPanelForCpanel() {
        image.controlPanel = CPANEL;
        assertEquals(CPANEL.toString().toLowerCase(), image.getImageControlPanel());
    }

    @Test
    public void getImageControlPanelForPlesk() {
        image.controlPanel = PLESK;
        assertEquals(PLESK.toString().toLowerCase(), image.getImageControlPanel());
    }

    @Test
    public void getImageControlPanelForISPConfig() {
        image.controlPanel = MYH;
        image.hfsName = "foobar-ispconfig-hellworld";
        assertEquals("ispconfig", image.getImageControlPanel());
    }

    @Test
    public void getImageControlPanelForJustMYH() {
        image.controlPanel = MYH;
        image.hfsName = "foobar";
        assertNull(image.getImageControlPanel());
    }

    @Test
    public void getImageIdForLinux() {
        image.operatingSystem = Image.OperatingSystem.LINUX;
        assertEquals(1, image.operatingSystem.getOperatingSystemId());
    }

    @Test
    public void getImageIdForWindows() {
        image.operatingSystem = Image.OperatingSystem.WINDOWS;
        assertEquals(2, image.operatingSystem.getOperatingSystemId());
    }

    @Test
    public void getControlPanelFromEcommName() {
        Image.ControlPanel controlPanelPleskWebPro = getEnumValueFromEcommName("pleskWebPro");
        Image.ControlPanel controlPanelPleskWebHost = getEnumValueFromEcommName("pleskWebHost");
        Image.ControlPanel controlPanelPlesk = getEnumValueFromEcommName("plesk");
        Image.ControlPanel controlPanelCpanel = getEnumValueFromEcommName("cpanel");
        Image.ControlPanel controlPanelMyh = getEnumValueFromEcommName("myh");

        assertEquals(PLESK, controlPanelPleskWebPro);
        assertEquals(PLESK, controlPanelPleskWebHost);
        assertEquals(PLESK, controlPanelPlesk);
        assertEquals(CPANEL, controlPanelCpanel);
        assertEquals(MYH, controlPanelMyh);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getControlPanelFromEcommNameBadName() {
        getEnumValueFromEcommName("badControlPanel");
    }
}
