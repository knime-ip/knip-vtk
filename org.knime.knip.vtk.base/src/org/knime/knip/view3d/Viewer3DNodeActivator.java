/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.view3d;

import java.awt.GraphicsEnvironment;

import org.knime.core.node.NodeLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This class must be called before the first rendering is done, as it loads all the vtk libs.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeActivator implements BundleActivator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger("View3D");

    private static boolean VTKLoaded = false;

    /*
     * The system specific files These are in order of their dependencies, SO DO
     * NOT TOUCH THEM!
     */
    private static String[] linux = {"lwjgl", "vtkproj4", "vtkalglib", "vtksys", "vtkCommon", "vtkCommonJava",
            "vtkFiltering", "vtkFilteringJava", "vtkexpat", "vtkjpeg", "vtkzlib", "vtklibxml2", "vtktiff", "vtkpng",
            "vtksqlite", "vtkmetaio", "vtkNetCDF", "vtkDICOMParser", "vtkNetCDF_cxx", "vtkIO", "vtkIOJava",
            "vtkImaging", "vtkImagingJava", "vtkverdict", "vtkGraphics", "vtkGraphicsJava", "vtkfreetype", "vtkftgl",
            "vtkGenericFiltering", "vtkRendering", "vtkRenderingJava", "vtkexoIIc", "vtkHybrid", "vtkHybridJava",
            "vtkVolumeRendering", "vtkVolumeRenderingJava", "vtkWidgets", "vtkWidgetsJava", "vtkInfovis",
            "vtkInfovisJava"};

    private static String[] windows = {"lwjgl", "msvcr100", "msvcp100", "vtkproj4", "vtkalglib", "vtksys", "vtkCommon",
            "vtkCommonJava", "vtkFiltering", "vtkFilteringJava", "vtkexpat", "vtkjpeg", "vtkzlib", "vtkhdf5",
            "vtklibxml2", "vtktiff", "vtkpng", "vtkmetaio", "vtkNetCDF", "vtkDICOMParser", "vtkNetCDF_cxx", "vtkIO",
            "vtkIOJava", "vtkImaging", "vtkImagingJava", "vtkverdict", "vtkGraphics", "vtkGraphicsJava", "vtkfreetype",
            "vtkftgl", "vtkGenericFiltering", "vtkRendering", "vtkRenderingJava", "vtkexoIIc", "vtkHybrid",
            "vtkHybridJava", "vtkVolumeRendering", "vtkVolumeRenderingJava", "vtkWidgets", "vtkWidgetsJava",
            "vtkInfovis", "vtkInfovisJava"};

    private static String[] osx = {"lwjgl", "vtkproj4", "vtkalglib", "vtksys", "vtkCommon", "vtkCommonJava",
            "vtkFiltering", "vtkFilteringJava", "vtkexpat", "vtkjpeg", "vtkzlib", "vtklibxml2", "vtktiff", "vtkpng",
            "vtksqlite", "vtkmetaio", "vtkNetCDF", "vtkDICOMParser", "vtkNetCDF_cxx", "vtkIO", "vtkIOJava",
            "vtkImaging", "vtkImagingJava", "vtkverdict", "vtkGraphics", "vtkGraphicsJava", "vtkfreetype", "vtkftgl",
            "vtkGenericFiltering", "vtkRendering", "vtkRenderingJava", "vtkexoIIc", "vtkHybrid", "vtkHybridJava",
            "vtkVolumeRendering", "vtkVolumeRenderingJava", "vtkWidgets", "vtkWidgetsJava", "vtkInfovis",
            "vtkInfovisJava"};

    /**
     * This method trys to load all libs that are passed to it.<br>
     * 
     * To ensure the libs are actually all loaded, the programmer has to make sure that the libs are in correct order.
     * 
     * @param libs the system specific libs to load
     * 
     * @throws UnsatisfiedLinkError if one or more libs could not be loaded at all
     */
    private void loadLibs(final String[] libs) {

        if (libs != null) {
            for (final String s : libs) {
                System.loadLibrary(s);
            }
        }
    }

    /*
     * Fixes bug where 3D Viewer is no longer available with java 7
     * 
     * For some reason under java7 liblwjgl.so is no longer able to load the libjawt.so
     * by itself. This is supposed to be fixed in the nightly builds, however a quick test
     * does not confirm that. The code below however does fix it.
     * 
     * Info: http://lwjgl.org/forum/index.php/topic,4085.0.html
     */
    private void preloadAWT() {
        java.awt.Toolkit.getDefaultToolkit();
        System.loadLibrary("jawt");

    }

    @Override
    public final void start(final BundleContext context) throws Exception {

        if (!GraphicsEnvironment.isHeadless()) {

            preloadAWT();

            LOGGER.debug("Trying to load vtk libs");

            final String os = System.getProperty("os.name");

            try {

                if (os.contains("Windows")) {
                    loadLibs(windows);
                } else if (os.equals("Linux")) {
                    loadLibs(linux);
                } else if (os.equals("Mac OS X")) {
                    loadLibs(osx);
                } else {
                    LOGGER.error("VTK not loaded, could not determine System: " + os);
                }

                LOGGER.debug("VTK successfully loaded");
                VTKLoaded = true;

            } catch (final UnsatisfiedLinkError error) {
                LOGGER.error("Could not load VTK, the 3D Viewer will not be available!");
                LOGGER.error(error.getMessage());
            }

        }
    }

    @Override
    public final void stop(final BundleContext context) throws Exception {
        // unused
    }

    public static final boolean VTKLoaded() {
        return VTKLoaded;
    }
}
