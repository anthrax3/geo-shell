package org.geoshell.tile

import geoscript.geom.Bounds
import geoscript.layer.Format
import geoscript.layer.ImageTileLayer
import geoscript.layer.Raster
import geoscript.layer.TileGenerator
import geoscript.layer.TileLayer
import geoscript.layer.TileRenderer
import org.geoshell.Catalog
import org.geoshell.map.MapName
import org.geoshell.raster.FormatName
import org.geoshell.raster.RasterName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.shell.support.util.OsUtils
import org.springframework.stereotype.Component

@Component
class TileCommands implements CommandMarker {

    @Autowired
    Catalog catalog

    @CliCommand(value = "tile open", help = "Open a Tile Layer.")
    String open(
            @CliOption(key = "name", mandatory = true, help = "The tile name") TileName name,
            @CliOption(key = "params", mandatory = true, help = "The connection parameters") String params
    ) throws Exception {
        TileLayer tileLayer = TileLayer.getTileLayer(params)
        if (tileLayer) {
            catalog.tiles[name] = tileLayer
            "Tile Layer ${name} opened!"
        } else {
            "Could not create Tile Layer ${name} from ${params}!"
        }
    }

    @CliCommand(value = "tile close", help = "Close a Tile Layer.")
    String close(
            @CliOption(key = "name", mandatory = true, help = "The tile name") TileName name
    ) throws Exception {
        TileLayer tileLayer = catalog.tiles[name]
        if (tileLayer) {
            tileLayer.close()
            catalog.tiles.remove(name)
            "Tile Layer ${name} closed!"
        } else {
            "Unable to find Tile Layer ${name}"
        }
    }

    @CliCommand(value = "tile list", help = "List open Tile Layers.")
    String list() throws Exception {
        catalog.tiles.collect{TileName name, TileLayer tileLayer ->
            "${name} = ${tileLayer.class.simpleName}"
        }.join(OsUtils.LINE_SEPARATOR)
    }

    @CliCommand(value = "tile info", help = "Get information about a Tile Layer.")
    String info(
            @CliOption(key = "name", mandatory = true, help = "The tile name") TileName name
    ) throws Exception {
        TileLayer tileLayer = catalog.tiles[name]
        if (tileLayer) {
            tileLayer.name + OsUtils.LINE_SEPARATOR +
                    tileLayer.pyramid.csv
        } else {
            "Unable to find Tile Layer ${name}"
        }
    }

    @CliCommand(value = "tile generate", help = "Generate tiles for a Tile Layer.")
    String generate(
            @CliOption(key = "name", mandatory = true, help = "The tile name") TileName name,
            @CliOption(key = "map", mandatory = true, help = "The map name") MapName mapName,
            @CliOption(key = "start", mandatory = true, help = "The map name") int startZoom,
            @CliOption(key = "end", mandatory = true, help = "The map name") int endZoom,
            @CliOption(key = "bounds", mandatory = false, help = "The map name") String bounds,
            @CliOption(key = "missingOnly", specifiedDefaultValue = "false", unspecifiedDefaultValue = "false", mandatory = false, help = "The map name") boolean missingOnly,
            @CliOption(key = "verbose", specifiedDefaultValue = "false", unspecifiedDefaultValue = "false", mandatory = false, help = "The map name") boolean verbose
    ) throws Exception {
        TileLayer tileLayer = catalog.tiles[name]
        if (tileLayer) {
            org.geoshell.map.Map map = catalog.maps[mapName]
           if (map) {
                TileRenderer tileRenderer = TileLayer.getTileRenderer(tileLayer, map.getLayers())
                TileGenerator generator = new TileGenerator(verbose: verbose)
                generator.generate(tileLayer, tileRenderer, startZoom, endZoom,
                        bounds: bounds ? Bounds.fromString(bounds) : null,
                        missingOnly: missingOnly
                )
                "Tiles generated!"
            } else {
                "Unable to find Map ${mapName}"
            }
        } else {
            "Unable to find Tile Layer ${name}"
        }
    }

    @CliCommand(value = "tile stitch raster", help = "Create a Raster from a Tile Layer.")
    String stitchRaster(
            @CliOption(key = "name",   mandatory = true, help = "The tile name") TileName tileName,
            @CliOption(key = "format", mandatory = true, help = "The raster format name") FormatName formatName,
            @CliOption(key = "raster", mandatory = true, help = "The raster name") String rasterName,
            @CliOption(key = "bounds", mandatory = false, help = "The bounds") String bounds,
            @CliOption(key = "width",  mandatory = false, unspecifiedDefaultValue = "400", specifiedDefaultValue = "400", help = "The raster width") int width,
            @CliOption(key = "height", mandatory = false, unspecifiedDefaultValue = "400", specifiedDefaultValue = "400", help = "The raster height") int height,
            @CliOption(key = "z",      mandatory = false, unspecifiedDefaultValue = "-1", specifiedDefaultValue = "0", help = "The zoom level") long z,
            @CliOption(key = "minx",   mandatory = false, unspecifiedDefaultValue = "-1", help = "The min x or column") long minX,
            @CliOption(key = "miny",   mandatory = false, unspecifiedDefaultValue = "-1", help = "The min y or row") long minY,
            @CliOption(key = "maxx",   mandatory = false, unspecifiedDefaultValue = "-1", help = "The max x or column") long maxX,
            @CliOption(key = "maxy",   mandatory = false, unspecifiedDefaultValue = "-1", help = "The max y or row") long maxY
    ) {
        TileLayer tileLayer = catalog.tiles[tileName]
        if (tileLayer) {
            if (!tileLayer instanceof ImageTileLayer) {
                return "Tile Layer must be an Image Tile Layer!"
            }
            Format format = catalog.formats[formatName]
            if (format) {
                ImageTileLayer imageTileLayer = tileLayer as ImageTileLayer
                Raster raster
                if (bounds && z == -1) {
                    Bounds b = Bounds.fromString(bounds)
                    if (b.proj && !b.proj.equals(tileLayer.pyramid.proj)) {
                        b = b.reproject(tileLayer.pyramid.proj)
                    }
                    raster = imageTileLayer.getRaster(b, width, height)
                } else if (bounds && z > -1) {
                    Bounds b = Bounds.fromString(bounds)
                    if (b.proj && !b.proj.equals(tileLayer.pyramid.proj)) {
                        b = b.reproject(tileLayer.pyramid.proj)
                    }
                    raster = imageTileLayer.getRaster(imageTileLayer.tiles(b, z))
                } else if (z > -1 && minX > -1 && minY > -1 && maxX > -1 && maxY > -1) {
                    raster = imageTileLayer.getRaster(imageTileLayer.tiles(z, minX, minY, maxX, maxY))
                } else if (z > -1) {
                    raster = imageTileLayer.getRaster(imageTileLayer.tiles(z))
                } else {
                    return "Wrong combination of options for stitching together a raster from a tile layer!"
                }
                format.write(raster)
                catalog.rasters[new RasterName(rasterName)] = raster
                "Done stitching Raster ${rasterName} from ${tileName}!"
            } else {
                "Unable to find Raster Format ${formatName}"
            }  
        } else {
            "Unable to find Tile Layer ${tileName}"
        }
    }
    
}
