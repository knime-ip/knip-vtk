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
package org.knime.knip.view3d.image;

import org.knime.core.node.NodeLogger;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.HistogramWithNormalization;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.PolylineTransferFunction;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.TransferFunction;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.TransferFunctionBundle;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.TransferFunctionColor;

import vtk.vtkActor;
import vtk.vtkAlgorithmOutput;
import vtk.vtkColorTransferFunction;
import vtk.vtkFixedPointVolumeRayCastMapper;
import vtk.vtkGPUVolumeRayCastMapper;
import vtk.vtkImageAccumulate;
import vtk.vtkImageActor;
import vtk.vtkImageCast;
import vtk.vtkImageData;
import vtk.vtkImageMapToColors;
import vtk.vtkImageReslice;
import vtk.vtkIntArray;
import vtk.vtkLookupTable;
import vtk.vtkMapper;
import vtk.vtkMatrix4x4;
import vtk.vtkObject;
import vtk.vtkOutlineFilter;
import vtk.vtkPassThrough;
import vtk.vtkPiecewiseFunction;
import vtk.vtkPlanes;
import vtk.vtkPolyDataMapper;
import vtk.vtkReferenceInformation;
import vtk.vtkSmartVolumeMapper;
import vtk.vtkVolume;
import vtk.vtkVolumeProperty;
import vtk.vtkVolumeRayCastIsosurfaceFunction;
import vtk.vtkVolumeRayCastMapper;
import vtk.vtkVolumeTextureMapper2D;
import vtk.vtkVolumeTextureMapper3D;

/**
 * This class wraps a vtkImage and all necessary properties for rendering it.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeVolume {

    /**
     * The Mapper to use for this volume.
     */
    public enum Mapper {
        /**
         * vtkSmartVolumeMapper.
         */
        SMART,
        /**
         * vtkVolumeTextureMapper3D.
         */
        TEXTURE3D,
        /**
         * vtkFixedPointVolumeRayCastMapper.
         */
        RAYFIXEDPOINT,
        /**
         * vtkGPUVolumeRayCastMapper.
         */
        GPU
    };

    private final vtkImageData m_image;

    private final vtkPassThrough m_imageWrapper;

    private final vtkVolume m_volume;

    private final vtkVolumeProperty m_property;

    // needed to display the bounding box
    private final vtkOutlineFilter m_boundingBoxFilter;

    private final vtkMapper m_boundingBoxMapper;

    private final vtkActor m_boundingBoxActor;

    // all available mappers
    private final vtkFixedPointVolumeRayCastMapper m_mapperFixedPoint;

    private final vtkVolumeTextureMapper3D m_mapperTexture3d;

    private final vtkSmartVolumeMapper m_mapperSmart;

    private final vtkGPUVolumeRayCastMapper m_mapperGPU;

    private Mapper m_mapper = null;

    private TransferFunctionBundle m_bundleGray;

    private TransferFunctionBundle m_bundleRGB;

    private final vtkPiecewiseFunction m_opacityGray;

    private final vtkPiecewiseFunction m_opacityRGB;

    private final vtkColorTransferFunction m_colorGray;

    private final vtkColorTransferFunction m_colorRGB;

    // all reslicers
    private vtkImageReslice m_resliceAxial;

    private vtkImageReslice m_resliceCoronal;

    private vtkImageReslice m_resliceSagittal;

    // the image to color mappers
    private vtkImageMapToColors m_mapAxial;

    private vtkImageMapToColors m_mapCoronal;

    private vtkImageMapToColors m_mapSagittal;

    // the image actors
    private vtkImageActor m_imageAxial;

    private vtkImageActor m_imageCoronal;

    private vtkImageActor m_imageSagittal;

    // the lookuptable for the slices
    private final vtkLookupTable m_tableRGB;

    private final vtkLookupTable m_tableGray;

    // various settings
    private static final double OPACITY_MAX = 1.0;

    private static final int NUM_COLORPOINTS = 50;

    private static final int NUM_BINS = 250;

    private static final double OPACITY_MULT = 10.0;

    // various thing we now about the image
    private final double[] m_rangeSelected = new double[2];

    private final Viewer3DNodeAxes.Volume m_axesVolume;

    private final HistogramWithNormalization m_histogram;

    // the logger for this class
    private static final NodeLogger LOGGER = NodeLogger.getLogger(Viewer3DNodeVolume.class);

    /**
     * Create a new volume.
     * 
     * @param image the vtkImageData
     * @param axes the Viewer3DNodeAxes.Volume instance that describes this volume
     * @param bundleGray the bundle to use for the gray mappings
     * @param bundleRGB the bundle to sue for the rgb mappings
     */
    public Viewer3DNodeVolume(final vtkImageData image, final Viewer3DNodeAxes.Volume axes,
                              final TransferFunctionBundle bundleGray, final TransferFunctionBundle bundleRGB) {

        // make a deep copy so that the info stays static
        m_axesVolume = axes.deepCopy();

        // Set up the image and its wrapper
        m_image = image;

        m_imageWrapper = new vtkPassThrough();
        m_imageWrapper.SetInput(m_image);

        // Get some info about the image
        m_rangeSelected[0] = m_image.GetScalarTypeMin();
        m_rangeSelected[1] = m_image.GetScalarTypeMax();

        // Set up the new bundles
        m_bundleGray = bundleGray;
        m_bundleRGB = bundleRGB;

        // Set up all the functions
        m_opacityGray = new vtkPiecewiseFunction();
        m_opacityRGB = new vtkPiecewiseFunction();

        m_colorGray = new vtkColorTransferFunction();
        m_colorRGB = new vtkColorTransferFunction();

        // calc the values of the transfer functions
        final double min = m_rangeSelected[0];
        final double max = m_rangeSelected[1];

        // set up the lookuptable
        m_tableRGB = new vtkLookupTable();
        m_tableRGB.SetScaleToLinear();
        m_tableRGB.SetTableRange(min, max);
        m_tableRGB.Build();

        m_tableGray = new vtkLookupTable();
        m_tableGray.SetScaleToLinear();
        m_tableGray.SetTableRange(min, max);
        m_tableGray.Build();

        constructOpacityFunction(m_bundleGray.get(TransferFunctionColor.ALPHA), min, max, m_opacityGray);
        constructOpacityFunction(m_bundleRGB.get(TransferFunctionColor.ALPHA), min, max, m_opacityRGB);

        constructGrayTransferFunction(m_bundleGray.get(TransferFunctionColor.GREY), min, max);
        constructColorFunction(m_bundleRGB, min, max);

        constructLookupTableRGB(m_bundleRGB);
        constructLookupTableGray(m_bundleGray);

        // calc the histogram of the image
        m_histogram = createHistogram(m_imageWrapper.GetOutputPort(), min, max, NUM_BINS);

        final vtkAlgorithmOutput castImage = castImage(m_imageWrapper.GetOutputPort());

        // only works with unsigned short and unsigned char
        final vtkVolumeTextureMapper2D mapperTex2 = new vtkVolumeTextureMapper2D();
        mapperTex2.SetInputConnection(castImage);

        // the different methods for the tay caster
        final vtkVolumeRayCastMapper rayMapper = new vtkVolumeRayCastMapper();
        rayMapper.SetInputConnection(castImage);

        // speed is comparable to floating point mapper
        // vtkVolumeRayCastCompositeFunction composite = new
        // vtkVolumeRayCastCompositeFunction();
        // everything is just black
        final vtkVolumeRayCastIsosurfaceFunction isosurface = new vtkVolumeRayCastIsosurfaceFunction();
        // rayMapper.SetVolumeRayCastFunction(composite);
        rayMapper.SetVolumeRayCastFunction(isosurface);

        // Set up all the mappers
        m_mapperFixedPoint = new vtkFixedPointVolumeRayCastMapper();
        m_mapperFixedPoint.SetInputConnection(m_imageWrapper.GetOutputPort());

        m_mapperTexture3d = new vtkVolumeTextureMapper3D();
        m_mapperTexture3d.SetInputConnection(m_imageWrapper.GetOutputPort());

        m_mapperSmart = new vtkSmartVolumeMapper();
        m_mapperSmart.SetInputConnection(m_imageWrapper.GetOutputPort());
        // smart.SetRequestedRenderModeToRayCastAndTexture();

        m_mapperGPU = new vtkGPUVolumeRayCastMapper();
        m_mapperGPU.SetInputConnection(m_imageWrapper.GetOutputPort());

        // Set up the vtk property stuff
        m_property = new vtkVolumeProperty();

        m_volume = new vtkVolume();
        m_volume.SetProperty(m_property);
        setMapper(Mapper.SMART);

        setUpImageSlices(m_image, m_imageWrapper.GetOutputPort());

        // set up the bounding box
        m_boundingBoxFilter = new vtkOutlineFilter();
        m_boundingBoxFilter.SetInputConnection(m_imageWrapper.GetOutputPort());

        m_boundingBoxMapper = new vtkPolyDataMapper();
        m_boundingBoxMapper.SetInputConnection(m_boundingBoxFilter.GetOutputPort());

        m_boundingBoxActor = new vtkActor();
        m_boundingBoxActor.SetMapper(m_boundingBoxMapper);
        m_boundingBoxActor.GetProperty().SetColor(0, 0, 0);
    }

    /**
     * Set up a new Wrapper for a given image.
     * 
     * @param image the vtkImageData
     * @param axes the Viewer3DNodeAxes.Volume instance that describes this volume
     */
    public Viewer3DNodeVolume(final vtkImageData image, final Viewer3DNodeAxes.Volume axes) {
        this(image, axes, TransferFunctionBundle.newGABundle(), TransferFunctionBundle.newRGBABundle());
    }

    private void setUpImageSlices(final vtkImageData image, final vtkAlgorithmOutput data) {

        // first we need to calculate the center of the image
        final int[] extent = image.GetWholeExtent();
        final double[] spacing = image.GetSpacing();
        final double[] origin = image.GetOrigin();

        final double[] center = new double[3];
        center[0] = origin[0] + (spacing[0] * 0.5 * (extent[0] + extent[1]));
        center[1] = origin[1] + (spacing[1] * 0.5 * (extent[2] + extent[3]));
        center[2] = origin[2] + (spacing[2] * 0.5 * (extent[4] + extent[5]));

        // then set up all three slice planes
        final vtkMatrix4x4 axial = new vtkMatrix4x4();
        axial.DeepCopy(new double[]{1, 0, 0, center[0], 0, 1, 0, center[1], 0, 0, 1, center[2], 0, 0, 0, 1});

        final vtkMatrix4x4 coronal = new vtkMatrix4x4();
        coronal.DeepCopy(new double[]{1, 0, 0, center[0], 0, 0, 1, center[1], 0, -1, 0, center[2], 0, 0, 0, 1});

        final vtkMatrix4x4 sagittal = new vtkMatrix4x4();
        sagittal.DeepCopy(new double[]{0, 0, -1, center[0], 1, 0, 0, center[1], 0, -1, 0, center[2], 0, 0, 0, 1});

        // and set up the actual reslice classes
        m_resliceAxial = new vtkImageReslice();
        m_resliceAxial.SetInputConnection(data);
        m_resliceAxial.SetOutputDimensionality(2);
        m_resliceAxial.SetInterpolationModeToLinear();
        m_resliceAxial.SetResliceAxes(axial);

        m_resliceCoronal = new vtkImageReslice();
        m_resliceCoronal.SetInputConnection(data);
        m_resliceCoronal.SetOutputDimensionality(2);
        m_resliceCoronal.SetInterpolationModeToLinear();
        m_resliceCoronal.SetResliceAxes(coronal);

        m_resliceSagittal = new vtkImageReslice();
        m_resliceSagittal.SetInputConnection(data);
        m_resliceSagittal.SetOutputDimensionality(2);
        m_resliceSagittal.SetInterpolationModeToLinear();
        m_resliceSagittal.SetResliceAxes(sagittal);

        // Set up the mappers
        m_mapAxial = new vtkImageMapToColors();
        m_mapAxial.SetInputConnection(m_resliceAxial.GetOutputPort());
        m_mapAxial.SetLookupTable(m_tableRGB);

        m_mapCoronal = new vtkImageMapToColors();
        m_mapCoronal.SetInputConnection(m_resliceCoronal.GetOutputPort());
        m_mapCoronal.SetLookupTable(m_tableRGB);

        m_mapSagittal = new vtkImageMapToColors();
        m_mapSagittal.SetInputConnection(m_resliceSagittal.GetOutputPort());
        m_mapSagittal.SetLookupTable(m_tableRGB);

        // Set up the image Actors
        m_imageAxial = new vtkImageActor();
        m_imageAxial.SetInput(m_mapAxial.GetOutput());

        m_imageCoronal = new vtkImageActor();
        m_imageCoronal.SetInput(m_mapCoronal.GetOutput());

        m_imageSagittal = new vtkImageActor();
        m_imageSagittal.SetInput(m_mapSagittal.GetOutput());
    }

    /**
     * Set the mapper for this volume.
     * 
     * @param mapper the mapper to use
     */
    public final void setMapper(final Mapper mapper) {

        if (m_mapper != mapper) {
            m_mapper = mapper;

            switch (m_mapper) {
                case GPU:
                    m_volume.SetMapper(m_mapperGPU);
                    break;
                case TEXTURE3D:
                    m_volume.SetMapper(m_mapperTexture3d);
                    break;
                case RAYFIXEDPOINT:
                    m_volume.SetMapper(m_mapperFixedPoint);
                    break;
                case SMART:
                    m_volume.SetMapper(m_mapperSmart);
                    break;
                default:
                    m_volume.SetMapper(m_mapperSmart);
                    break;
            }
        }
    }

    /**
     * Set the clipping planes to use in all mappers.
     * 
     * @param planes the planes
     */
    public final void setClippingPlanes(final vtkPlanes planes) {
        m_mapperSmart.SetClippingPlanes(planes);
        m_mapperFixedPoint.SetClippingPlanes(planes);
        m_mapperTexture3d.SetClippingPlanes(planes);
        m_mapperGPU.SetClippingPlanes(planes);
    }

    private vtkAlgorithmOutput castImage(final vtkAlgorithmOutput data) {
        final vtkImageCast cast = new vtkImageCast();
        cast.SetOutputScalarTypeToUnsignedShort();
        cast.SetInputConnection(data);

        return cast.GetOutputPort();
    }

    /**
     * Build a new opacity function in form of a vtkPiecewiseFunction.
     * 
     * @param func the source to use
     * @param min the minimum of the image to display
     * @param max the maximum of the image to display
     * @param target the function to write the result to
     */
    private void constructOpacityFunction(final TransferFunction func, final double min, final double max,
                                          final vtkPiecewiseFunction target) {

        // Build it from scratch
        target.RemoveAllPoints();

        final double range = Math.abs(max - min);

        // Add the points from the function if this is a polyline
        if (func.getClass() == PolylineTransferFunction.class) {

            for (final PolylineTransferFunction.Point p : ((PolylineTransferFunction)func).getPoints()) {

                addOpacityPoint(p.getX(), p.getY(), range, min, target);
            }
        } else {

            // else just build a number of points, same procedure as for the
            // color transfer function

            final double step = 1.0 / (NUM_COLORPOINTS);
            double x = 0.0;

            while (x <= 1.0) {
                addOpacityPoint(x, func.getValueAt(x), range, min, target);
                x += step;
            }
        }
    }

    private void addOpacityPoint(final double x, final double y, final double range, final double min,
                                 final vtkPiecewiseFunction target) {
        final double pos = (x * range) + min;
        final double value = y * OPACITY_MAX;

        target.AddPoint(pos, value);
    }

    /**
     * Set up a new Colortransferfunction for mapping to a gray scale.
     * 
     * @param func the function from which to construct transferfunction
     * @param min the minimum value that the function should map to
     * @param max the maximum value that the function should map to
     */
    private void constructGrayTransferFunction(final TransferFunction func, final double min, final double max) {

        // Build from scratch
        m_colorGray.RemoveAllPoints();

        // REMEMBER: Gray = 0.0, White = 1.0
        final double range = Math.abs(max - min);

        // if polyline, just use those points
        if (func.getClass() == PolylineTransferFunction.class) {

            for (final PolylineTransferFunction.Point p : ((PolylineTransferFunction)func).getPoints()) {

                addGrayPoint(p.getX(), p.getY(), range, min);
            }
        } else {
            // else just build a number of points, same procedure as for the
            // color transfer function

            final double step = 1.0 / (NUM_COLORPOINTS);
            double x = 0.0;

            while (x <= 1.0) {
                addGrayPoint(x, func.getValueAt(x), range, min);
                x += step;
            }
        }
    }

    private void addGrayPoint(final double x, final double y, final double range, final double min) {
        final double pos = (x * range) + min;
        final double val = 1.0 - y;

        m_colorGray.AddRGBPoint(pos, val, val, val);
    }

    /**
     * Build a new vtkColorTransferFunction from a standard rbg TFBundle.
     * 
     * @param bundle the bundle to use
     * @param min the minimum of the image to display
     * @param max the maximum of the image to display
     */
    private void constructColorFunction(final TransferFunctionBundle bundle, final double min, final double max) {

        // build from scratch
        m_colorRGB.RemoveAllPoints();

        final double range = Math.abs(max - min);

        // get all three functions
        final TransferFunction red = bundle.get(TransferFunctionColor.RED);
        final TransferFunction green = bundle.get(TransferFunctionColor.GREEN);
        final TransferFunction blue = bundle.get(TransferFunctionColor.BLUE);

        // Add some points
        for (int i = 0; i <= NUM_COLORPOINTS; i++) {

            final double frac = ((double)i) / NUM_COLORPOINTS;
            final double pos = (range * frac) + min;

            final double redVal = red.getValueAt(frac);
            final double greenVal = green.getValueAt(frac);
            final double blueVal = blue.getValueAt(frac);

            m_colorRGB.AddRGBPoint(pos, redVal, greenVal, blueVal);
        }
    }

    /**
     * Construct the Lookuptable for the slices.
     * 
     * @param bundle the bundle for the rgb functions
     */
    private void constructLookupTableRGB(final TransferFunctionBundle bundle) {

        // get all four functions
        final TransferFunction red = bundle.get(TransferFunctionColor.RED);
        final TransferFunction green = bundle.get(TransferFunctionColor.GREEN);
        final TransferFunction blue = bundle.get(TransferFunctionColor.BLUE);
        final TransferFunction alpha = bundle.get(TransferFunctionColor.ALPHA);

        // add the points
        final int maxValues = m_tableRGB.GetNumberOfTableValues() - 1;
        for (int i = 0; i <= maxValues; i++) {

            final double frac = ((double)i) / maxValues;

            final double redVal = red.getValueAt(frac);
            final double greenVal = green.getValueAt(frac);
            final double blueVal = blue.getValueAt(frac);
            double alphaVal = alpha.getValueAt(frac) * OPACITY_MULT;
            alphaVal = alphaVal > 1.0 ? 1.0 : alphaVal;

            m_tableRGB.SetTableValue(i, redVal, greenVal, blueVal, alphaVal);
        }
    }

    /**
     * Construct the Lookuptable for the slices.
     * 
     * @param bundle the bundle for the gray functions
     */
    private void constructLookupTableGray(final TransferFunctionBundle bundle) {

        // get all four functions
        final TransferFunction gray = bundle.get(TransferFunctionColor.GREY);
        final TransferFunction alpha = bundle.get(TransferFunctionColor.ALPHA);

        // add the points
        final int maxValues = m_tableGray.GetNumberOfTableValues() - 1;
        for (int i = 0; i <= maxValues; i++) {

            final double frac = ((double)i) / maxValues;

            final double grayVal = 1.0 - gray.getValueAt(frac);
            double alphaVal = alpha.getValueAt(frac) * OPACITY_MULT;
            alphaVal = alphaVal > 1.0 ? 1.0 : alphaVal;

            m_tableGray.SetTableValue(i, grayVal, grayVal, grayVal, alphaVal);
        }
    }

    /**
     * Create a histogram from an image.
     * 
     * @param data the pipeline connection to use
     * @param min the minimum value of the image
     * @param max the maximum value of the image
     * @param numBins the number of bins that should be created
     * @return an array of ints, containg the counts
     */
    private HistogramWithNormalization createHistogram(final vtkAlgorithmOutput data, final double min,
                                                       final double max, final int numBins) {

        final double spacing = Math.abs(max - min) / numBins;

        // set up an accumulator to generate the histogram
        final vtkImageAccumulate histogram = new vtkImageAccumulate();
        histogram.SetInputConnection(data);
        histogram.SetComponentExtent(0, numBins - 1, 0, 0, 0, 0);
        histogram.SetComponentSpacing(spacing, 0, 0);
        histogram.SetComponentOrigin(min, 0, 0);

        // call update to create the output image
        histogram.Update();

        // get the result
        final vtkImageData i = histogram.GetOutput();
        final vtkIntArray array = (vtkIntArray)i.GetPointData().GetScalars();
        final int[] d = array.GetJavaArray();

        final long[] dConverted = new long[d.length];
        for (int j = 0; j < d.length; j++) {
            dConverted[j] = d[j];
        }

        return new HistogramWithNormalization(dConverted, min, max);
    }

    /**
     * Gets the histogram for this instance.
     * 
     * @return The histogram.
     */
    public final HistogramWithNormalization getHistogram() {
        return m_histogram;
    }

    /**
     * Gets the volume for this instance.
     * 
     * @return The volume.
     */
    public final vtkVolume getVolume() {
        return m_volume;
    }

    /**
     * Call this if the TFBundle changed.
     * 
     * The method will sync the TFBundle to the corresponding vtkFunction.
     */
    public final void updateOpacityGray() {

        final double min = m_rangeSelected[0];
        final double max = m_rangeSelected[1];

        constructOpacityFunction(m_bundleGray.get(TransferFunctionColor.ALPHA), min, max, m_opacityGray);
    }

    /**
     * Call this if the TFBundle changed.
     * 
     * The method will sync the TFBundle to the corresponding vtkFunction.
     */
    public final void updateOpacityRGB() {

        final double min = m_rangeSelected[0];
        final double max = m_rangeSelected[1];

        constructOpacityFunction(m_bundleRGB.get(TransferFunctionColor.ALPHA), min, max, m_opacityRGB);
    }

    /**
     * Call this if the TFBundle changed.
     * 
     * The method will sync the TFBundle to the corresponding vtkFunction.
     */
    public final void updateColorGray() {

        final double min = m_rangeSelected[0];
        final double max = m_rangeSelected[1];

        constructGrayTransferFunction(m_bundleGray.get(TransferFunctionColor.GREY), min, max);
    }

    /**
     * Call this if the TFBundle changed.
     * 
     * The method will sync the TFBundle to the corresponding vtkFunction.
     */
    public final void updateColorRGB() {

        final double min = m_rangeSelected[0];
        final double max = m_rangeSelected[1];

        constructColorFunction(m_bundleRGB, min, max);
    }

    /**
     * Call this if the TFBundle changed.<br>
     * 
     * The method will sync the TFBundle to the corresponding vtkLookupTable.
     */
    public final void updateLookupTableRGB() {
        constructLookupTableRGB(m_bundleRGB);
    }

    /**
     * Call this if the TFBundle changed.<br>
     * 
     * The method will sync the TFBundle to the corresponding vtkLookupTable.
     */
    public final void updateLookupTableGray() {
        constructLookupTableGray(m_bundleGray);
    }

    /**
     * Set this volume to work in gray mode.
     * 
     * Basically, the image will now be rendered in gray
     */
    public final void setGrayMode() {
        m_property.SetColor(m_colorGray);
        m_property.SetScalarOpacity(m_opacityGray);

        // tell the vtk pipeline that we modified the property
        // will not work if done directly on the property or the volume or the
        // mapper
        m_colorGray.Modified();
        m_opacityGray.Modified();

        m_mapAxial.SetLookupTable(m_tableGray);
        m_mapCoronal.SetLookupTable(m_tableGray);
        m_mapSagittal.SetLookupTable(m_tableGray);
    }

    /**
     * Set this volume to work in rgb mode.
     * 
     * Basically, the image will now be rendered in color
     */
    public final void setRGBMode() {
        m_property.SetColor(m_colorRGB);
        m_property.SetScalarOpacity(m_opacityRGB);

        // tell the vtk pipeline that we modified the property
        // will not work if done directly on the property or the volume or the
        // mapper
        m_colorRGB.Modified();
        m_opacityRGB.Modified();

        m_mapAxial.SetLookupTable(m_tableRGB);
        m_mapCoronal.SetLookupTable(m_tableRGB);
        m_mapSagittal.SetLookupTable(m_tableRGB);
    }

    /**
     * Gets the bounding box actor.
     * 
     * @return the bounding box.
     */
    public final vtkActor getBoundingBoxActor() {
        return m_boundingBoxActor;
    }

    /**
     * Gets the bundleGray for this instance.
     * 
     * @return The bundleGray.
     */
    public final TransferFunctionBundle getBundleGray() {
        return m_bundleGray;
    }

    /**
     * Gets the bundleRGB for this instance.
     * 
     * @return The bundleRGB.
     */
    public final TransferFunctionBundle getBundleRGB() {
        return m_bundleRGB;
    }

    /**
     * Gets the axes for this instance.
     * 
     * @return The axes.
     */
    public final String[] getAxes() {
        final String[] axes = new String[m_axesVolume.getDisplayed().size()];

        for (int i = 0; i < axes.length; i++) {
            axes[i] = m_axesVolume.getDisplayed().get(i).getLabel();
        }

        return axes;
    }

    /**
     * Get the volume meta information of this volume.
     * 
     * @return the meta info
     */
    public final Viewer3DNodeAxes.Volume getAxesVolume() {
        return m_axesVolume;
    }

    /**
     * Sets the bundleGray for this instance.<br>
     * 
     * This will perform a deep copy of the given bundle, so that the volumes still have separate tfs.
     * 
     * @param bundleGray The bundleGray.
     */
    public final void setBundleGray(final TransferFunctionBundle bundleGray) {
        m_bundleGray = new TransferFunctionBundle(bundleGray);
        updateOpacityGray();
        updateColorGray();
    }

    /**
     * Sets the bundleRGB for this instance.<br>
     * 
     * This will perform a deep copy of the given bundle, so that the volumes still have separate tfs.
     * 
     * @param bundleRGB The bundleRGB.
     */
    public final void setBundleRGB(final TransferFunctionBundle bundleRGB) {
        m_bundleRGB = new TransferFunctionBundle(bundleRGB);
        updateOpacityRGB();
        updateColorRGB();
        updateLookupTableRGB();
    }

    /**
     * Get the vtkImageActor representing the axial slice image of this Volume.
     * 
     * @return the axial image actor
     */
    public final vtkImageActor getImageActorAxial() {
        return m_imageAxial;
    }

    /**
     * Get the vtkImageActor representing the coronal slice image of this Volume.
     * 
     * @return the coronal image actor
     */
    public final vtkImageActor getImageActorCoronal() {
        return m_imageCoronal;
    }

    /**
     * Get the vtkImageActor representing the sagittal slice image of this Volume.
     * 
     * @return the sagittal image actor
     */
    public final vtkImageActor getImageActorSagittal() {
        return m_imageSagittal;
    }

    /**
     * Move the axial image slice.<br>
     * 
     * The distance depends on the current spacing of the image volume.
     * 
     * @param slices by n slices
     */
    public final void moveImageAxial(final int slices) {
        final double spacing = m_image.GetSpacing()[2];
        moveImage(m_resliceAxial, spacing, slices);
    }

    /**
     * Move the coronal image slice.<br>
     * 
     * The distance depends on the current spacing of the image volume.
     * 
     * @param slices by n slices
     */
    public final void moveImageCoronal(final int slices) {
        final double spacing = m_image.GetSpacing()[1];
        moveImage(m_resliceCoronal, spacing, slices);
    }

    /**
     * Move the sagittal image slice.<br>
     * 
     * The distance depends on the current spacing of the image volume.
     * 
     * @param slices by n slices
     */
    public final void moveImageSagittal(final int slices) {
        final double spacing = m_image.GetSpacing()[0];
        moveImage(m_resliceSagittal, spacing, slices);
    }

    private void moveImage(final vtkImageReslice reslice, final double spacing, final int slices) {
        // calc the new center
        final double[] point = new double[]{0.0, 0.0, spacing * slices, 1.0};
        final double[] center = new double[4];

        final vtkMatrix4x4 axes = reslice.GetResliceAxes();
        axes.MultiplyPoint(point, center);

        // verify that we did not leave the boundaries
        final int[] extent = m_image.GetExtent();

        // correct the slice boundaries
        for (int i = 0; i < extent.length; i++) {
            extent[i] *= spacing;
        }

        center[0] = center[0] < extent[0] ? extent[0] : center[0];
        center[0] = center[0] > extent[1] ? extent[1] : center[0];
        center[1] = center[1] < extent[2] ? extent[2] : center[1];
        center[1] = center[1] > extent[3] ? extent[3] : center[1];
        center[2] = center[2] < extent[4] ? extent[4] : center[2];
        center[2] = center[2] > extent[5] ? extent[5] : center[2];

        // set the new slice center
        axes.SetElement(0, 3, center[0]);
        axes.SetElement(1, 3, center[1]);
        axes.SetElement(2, 3, center[2]);
    }

    /**
     * Get the extent of the image.
     * 
     * @return the extent of the image
     */
    public final int[] getExtent() {
        return m_image.GetExtent();
    }

    /**
     * Get all current slices at once.<br>
     * 
     * The ordering is as follows:<br>
     * 0 -> Sagittal 1 -> Coronal 2 -> Axial
     * 
     * @return the current slices
     */
    public final int[] getCurrentSlices() {
        final int slices[] = new int[3];

        slices[0] = getCurrentSliceSagittal();
        slices[1] = getCurrentSliceCoronal();
        slices[2] = getCurrentSliceAxial();

        return slices;
    }

    /**
     * Get the current slice of the axial image.
     * 
     * @return the axial slice
     */
    public final int getCurrentSliceAxial() {
        final double spacing = m_image.GetSpacing()[2];
        return (int)(m_resliceAxial.GetResliceAxes().GetElement(2, 3) / spacing);
    }

    /**
     * Get the current slice of the coronal image.
     * 
     * @return the coronal slice
     */
    public final int getCurrentSliceCoronal() {
        final double spacing = m_image.GetSpacing()[1];
        return (int)(m_resliceCoronal.GetResliceAxes().GetElement(1, 3) / spacing);
    }

    /**
     * Get the current slice of the sagittal image.
     * 
     * @return the sagittal slice
     */
    public final int getCurrentSliceSagittal() {
        final double spacing = m_image.GetSpacing()[0];
        return (int)(m_resliceSagittal.GetResliceAxes().GetElement(0, 3) / spacing);
    }

    public final void normalize() {
        final double[] values = m_image.GetScalarRange();
        setMappingRange(values[0], values[1]);
    }

    public final void useFullRangeForMapping() {
        setMappingRange(m_image.GetScalarTypeMin(), m_image.GetScalarTypeMax());
    }

    public final void setMappingRange(final double min, final double max) {

        if (min >= max) {
            throw new IllegalArgumentException("Min must be smaller than max");
        }

        m_rangeSelected[0] = min;
        m_rangeSelected[1] = max;

        updateOpacityGray();
        updateColorGray();
        updateLookupTableGray();

        updateOpacityRGB();
        updateColorRGB();
        updateLookupTableRGB();
    }

    /**
     * Free the memory occupied by the image by calling vtkImageData.Delete().
     * 
     * @param gc wheter or not to call the garbage collector
     */
    public final void delete(final boolean gc) {
        // We must call Delete() on all vtkObjects to decrease their
        // reference count, so that the vtk Garbage Collector can
        // actually free the memory
        m_mapAxial.Delete();
        m_mapCoronal.Delete();
        m_mapSagittal.Delete();

        m_resliceAxial.Delete();
        m_resliceCoronal.Delete();
        m_resliceSagittal.Delete();

        m_imageAxial.Delete();
        m_imageCoronal.Delete();
        m_imageSagittal.Delete();

        m_tableRGB.Delete();
        m_tableGray.Delete();

        m_opacityGray.Delete();
        m_opacityRGB.Delete();
        m_colorGray.Delete();
        m_colorRGB.Delete();

        m_mapperFixedPoint.Delete();
        m_mapperTexture3d.Delete();
        m_mapperSmart.Delete();
        m_mapperGPU.Delete();

        m_imageWrapper.Delete();

        m_volume.Delete();
        m_property.Delete();
        m_image.Delete();

        if (gc) {
            // now call the Garbage Collector to free all memory from this
            // volume
            final vtkReferenceInformation info = vtkObject.JAVA_OBJECT_MANAGER.gc(true);

            LOGGER.debug(info.toString());
            LOGGER.debug(info.listKeptReferenceToString());
            LOGGER.debug(info.listRemovedReferenceToString());
        }
    }
}
