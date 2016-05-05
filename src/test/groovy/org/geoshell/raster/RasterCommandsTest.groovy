package org.geoshell.raster

import geoscript.layer.Format
import geoscript.layer.Layer
import geoscript.layer.Raster
import geoscript.workspace.Memory
import org.geoshell.Catalog
import org.geoshell.vector.LayerName
import org.geoshell.vector.WorkspaceName
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.shell.support.util.OsUtils

import static org.junit.Assert.*

class RasterCommandsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test void open() {
        Catalog catalog = new Catalog()
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        Format format = Format.getFormat(file)
        catalog.formats[new FormatName("raster")] = format

        RasterCommands cmds = new RasterCommands(catalog: catalog)
        String result = cmds.open(new FormatName("raster"), new RasterName("raster"), null)
        assertEquals "Opened Format raster Raster raster as raster:raster", result
        assertNotNull catalog.rasters[new RasterName("raster:raster")]
    }

    @Test void close() {
        Catalog catalog = new Catalog()
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        Format format = Format.getFormat(file)
        catalog.formats[new FormatName("raster")] = format

        RasterCommands cmds = new RasterCommands(catalog: catalog)
        // Open
        cmds.open(new FormatName("raster"), new RasterName("raster"), null)
        assertNotNull catalog.rasters[new RasterName("raster:raster")]
        // Close
        String result = cmds.close(new RasterName("raster:raster"))
        assertEquals "Raster raster:raster closed!", result
        assertNull catalog.rasters[new RasterName("raster:raster")]
    }

    @Test void list() {
        Catalog catalog = new Catalog()
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        Format format = Format.getFormat(file)
        catalog.formats[new FormatName("raster")] = format

        RasterCommands cmds = new RasterCommands(catalog: catalog)
        cmds.open(new FormatName("raster"), new RasterName("raster"), null)
        assertNotNull catalog.rasters[new RasterName("raster:raster")]
        String result =  cmds.list()
        assertEquals "raster:raster = GeoTIFF", result
    }

    @Test void info() {
        Catalog catalog = new Catalog()
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        Format format = Format.getFormat(file)
        catalog.formats[new FormatName("raster")] = format

        RasterCommands cmds = new RasterCommands(catalog: catalog)
        cmds.open(new FormatName("raster"), new RasterName("raster"), null)
        assertNotNull catalog.rasters[new RasterName("raster:raster")]
        String result = cmds.info(new RasterName("raster:raster"))
        assertTrue result.contains("Format: GeoTIFF")
        assertTrue result.contains("Size: 900, 450")
    }

    @Test void value() {
        Catalog catalog = new Catalog()
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        Format format = Format.getFormat(file)
        catalog.formats[new FormatName("raster")] = format

        RasterCommands cmds = new RasterCommands(catalog: catalog)
        cmds.open(new FormatName("raster"), new RasterName("raster"), null)
        String result = cmds.value(new RasterName("raster:raster"), 0, -179, 89, "geometry")
        assertEquals "184.0", result
        result = cmds.value(new RasterName("raster:raster"), 0, 10, 15, "pixel")
        assertEquals "184.0", result
    }

    @Test void crop() {
        Catalog catalog = new Catalog()
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        Format format = Format.getFormat(file)
        catalog.formats[new FormatName("raster")] = format

        File outFile = new File(temporaryFolder.newFolder("cropped"), "cropped.tif")
        Format outFormat = Format.getFormat(outFile)
        catalog.formats[new FormatName("cropped")] = outFormat

        RasterCommands cmds = new RasterCommands(catalog: catalog)
        cmds.open(new FormatName("raster"), new RasterName("raster"), null)
        assertNotNull catalog.rasters[new RasterName("raster:raster")]
        String result = cmds.crop(new RasterName("raster:raster"),new FormatName("cropped"),"cropped","0,0,90,90")
        assertEquals "Raster raster:raster cropped to cropped!", result
        Raster raster = catalog.rasters[new RasterName("cropped")]
        assertNotNull raster
        assertEquals 0.0, raster.bounds.minX, 0.01
        assertEquals 0.0, raster.bounds.minY, 0.01
        assertEquals 90.0, raster.bounds.maxX, 0.01
        assertEquals 90.0, raster.bounds.maxY, 0.01
    }

    @Test void reproject() {
        Catalog catalog = new Catalog()
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        Format format = Format.getFormat(file)
        catalog.formats[new FormatName("raster")] = format

        File croppedOutFile = new File(temporaryFolder.newFolder("cropped"), "cropped.tif")
        Format croppedOutFormat = Format.getFormat(croppedOutFile)
        catalog.formats[new FormatName("cropped")] = croppedOutFormat

        File outFile = new File(temporaryFolder.newFolder("reprojected"), "reprojected.tif")
        Format outFormat = Format.getFormat(outFile)
        catalog.formats[new FormatName("reprojected")] = outFormat

        RasterCommands cmds = new RasterCommands(catalog: catalog)
        cmds.open(new FormatName("raster"), new RasterName("raster"), null)
        cmds.crop(new RasterName("raster:raster"),new FormatName("cropped"),"cropped","-124.771729,45.359865,-116.883545,48.169749")
        String result = cmds.reproject(new RasterName("cropped"),new FormatName("reprojected"),"reprojected","EPSG:3857")
        assertEquals("Raster cropped reprojected to reprojected as EPSG:3857!", result)
        Raster raster = catalog.rasters[new RasterName("reprojected")]
        assertNotNull raster
        assertEquals "EPSG:3857", raster.proj.srs
    }

    @Test void getSetStyle() {
        Catalog catalog = new Catalog()
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        Format format = Format.getFormat(file)
        catalog.formats[new FormatName("raster")] = format

        RasterCommands cmds = new RasterCommands(catalog: catalog)
        cmds.open(new FormatName("raster"), new RasterName("raster"), "raster")

        File sldFile = temporaryFolder.newFile("raster.sld")

        String result = cmds.getStyle(new RasterName("raster"), sldFile)
        assertTrue result.startsWith("raster style written to")
        assertTrue result.endsWith("raster.sld")

        result = cmds.getStyle(new RasterName("raster"), null)
        assertTrue result.contains("<sld:StyledLayerDescriptor")

        result = cmds.setStyle(new RasterName("raster"), sldFile)
        assertTrue result.startsWith("Style ")
        assertTrue result.endsWith("raster.sld set on raster")
    }

    @Test void contours() {
        Catalog catalog = new Catalog()
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        Format format = Format.getFormat(file)
        catalog.formats[new FormatName("raster")] = format

        RasterCommands cmds = new RasterCommands(catalog: catalog)
        cmds.open(new FormatName("raster"), new RasterName("raster"), "raster")
        String result = cmds.contours(new RasterName("raster"), new WorkspaceName("mem"), "raster_contours", 0, "184,185,186,187", false, false, "")
        assertEquals("Done creating contours!", result)
        Layer layer = catalog.layers[new LayerName("raster_contours")]
        assertNotNull layer
    }

    @Test void reclassify() {
        Catalog catalog = new Catalog()
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        Format format = Format.getFormat(file)
        catalog.formats[new FormatName("raster")] = format

        File outFile = new File(temporaryFolder.newFolder("reclassified"), "reclassified.tif")
        Format outFormat = Format.getFormat(outFile)
        catalog.formats[new FormatName("reclassified")] = outFormat

        RasterCommands cmds = new RasterCommands(catalog: catalog)
        cmds.open(new FormatName("raster"), new RasterName("raster"), "raster")
        String result = cmds.reclassify(new RasterName("raster"), new FormatName("reclassified"), "reclassified", "0-185=1,186-200=2,201-255=3", 0, 0)
        assertEquals("Raster raster reclassified to reclassified!", result)
        Raster raster = catalog.rasters[new RasterName("reclassified")]
        assertNotNull raster
    }

}